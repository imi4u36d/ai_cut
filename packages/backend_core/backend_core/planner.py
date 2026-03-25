from __future__ import annotations

import base64
from dataclasses import dataclass
from pathlib import Path
import socket
import tempfile
from typing import Callable, Protocol
import json
import re
import urllib.error
import urllib.request

from .config import Settings
from .media import sample_audio_peaks, sample_video_frames
from .schemas import ClipPlan, MediaProbe, TaskSpec
from .utils import clamp, extract_json_object


@dataclass(frozen=True)
class PlannerContext:
    task: TaskSpec
    source: MediaProbe
    transcriptText: str | None = None
    sourcePath: str | None = None
    trace: Callable[[str, str, str, dict[str, object] | None, str], None] | None = None


@dataclass(frozen=True)
class TranscriptCue:
    startSeconds: float
    endSeconds: float
    text: str


@dataclass(frozen=True)
class VisualInsight:
    title: str
    reason: str
    focusSeconds: float
    confidence: float | None = None
    eventType: str | None = None
    relatedSubtitle: str | None = None


@dataclass(frozen=True)
class SignalBundle:
    transcriptCues: list[TranscriptCue]
    selectedTranscriptCues: list[TranscriptCue]
    transcriptTimestamps: list[float]
    audioPeaks: list[tuple[float, float]]


@dataclass(frozen=True)
class VisionAnalysisResult:
    modelName: str
    events: list[VisualInsight]
    frameTimestamps: list[float]
    signals: SignalBundle


class Planner(Protocol):
    def plan(self, context: PlannerContext) -> list[ClipPlan]:
        raise NotImplementedError


class HeuristicPlanner:
    def __init__(self, settings: Settings):
        self.settings = settings

    def plan(self, context: PlannerContext) -> list[ClipPlan]:
        task = context.task
        source = context.source
        clip_count = max(1, min(task.outputCount, self.settings.pipeline.max_output_count))
        transcript_cues = parse_transcript_cues(context.transcriptText)
        if context.trace is not None:
            context.trace(
                "planning",
                "heuristic.start",
                "未使用大模型，切换到本地启发式规划。",
                {
                    "clip_count": clip_count,
                    "transcript_cue_count": len(transcript_cues),
                },
                "INFO",
            )
        target_duration = clamp(
            (task.minDurationSeconds + task.maxDurationSeconds) / 2,
            float(task.minDurationSeconds),
            float(task.maxDurationSeconds),
        )
        max_available = max(1.0, source.durationSeconds - 0.5)
        target_duration = min(target_duration, max_available)
        window = max(0.0, source.durationSeconds - target_duration)

        clips: list[ClipPlan] = []
        for index in range(clip_count):
            cue = _pick_cue(transcript_cues, index, clip_count)
            if cue is not None:
                cue_center = (cue.startSeconds + cue.endSeconds) / 2
                start_seconds = clamp(
                    cue_center - target_duration * 0.35,
                    0.0,
                    max(0.0, source.durationSeconds - target_duration),
                )
                title_hint = cue.text[:16]
                semantic_hint = f"参考字幕片段：{cue.text[:80]}"
            elif clip_count == 1:
                start_seconds = max(0.0, (source.durationSeconds - target_duration) / 2)
                title_hint = task.creativePrompt.strip() if task.creativePrompt else "高能切片"
                semantic_hint = ""
            else:
                ratio = index / max(1, clip_count - 1)
                start_seconds = window * ratio
                title_hint = task.creativePrompt.strip() if task.creativePrompt else "高能切片"
                semantic_hint = ""
            end_seconds = min(source.durationSeconds, start_seconds + target_duration)
            duration_seconds = max(1.0, end_seconds - start_seconds)
            title = f"{task.title[:18]} - {index + 1}"
            if title_hint:
                title = f"{title} / {title_hint[:12]}"
            reason = (
                f"基于时长区间 {task.minDurationSeconds}-{task.maxDurationSeconds} 秒自动生成，"
                f"适配 {task.platform} 投放。"
            )
            if task.creativePrompt:
                reason += f" 创意提示：{task.creativePrompt[:120]}"
            if semantic_hint:
                reason += f" {semantic_hint}"
            clips.append(
                ClipPlan(
                    clipIndex=index + 1,
                    title=title,
                    reason=reason,
                    startSeconds=round(start_seconds, 3),
                    endSeconds=round(end_seconds, 3),
                    durationSeconds=round(duration_seconds, 3),
                )
            )
        if context.trace is not None:
            context.trace(
                "planning",
                "heuristic.completed",
                "本地启发式规划已生成候选片段。",
                {
                    "clip_count": len(clips),
                    "clip_titles": [clip.title for clip in clips[:5]],
                },
                "INFO",
            )
        return clips


def _image_data_url(path: Path) -> str:
    encoded = base64.b64encode(path.read_bytes()).decode("ascii")
    return f"data:image/jpeg;base64,{encoded}"


def _cue_signal_score(cue: TranscriptCue) -> float:
    emphasis_keywords = [
        "不要",
        "不行",
        "住手",
        "为什么",
        "怎么会",
        "竟然",
        "离婚",
        "结婚",
        "杀",
        "死",
        "滚",
        "完了",
        "求你",
        "真相",
        "秘密",
        "反转",
    ]
    score = min(len(cue.text) / 18.0, 3.0)
    if any(token in cue.text for token in emphasis_keywords):
        score += 2.5
    if any(token in cue.text for token in ["！", "!", "？", "?"]):
        score += 1.5
    if len(cue.text) >= 18:
        score += 0.5
    return score


def select_transcript_signal_cues(
    transcript_cues: list[TranscriptCue],
    duration_seconds: float,
    frame_count: int,
) -> list[TranscriptCue]:
    cue_limit = max(1, frame_count)
    if not transcript_cues:
        return []

    minimum_gap = max(3.0, duration_seconds / max(4.0, frame_count * 1.6))
    ranked = sorted(
        transcript_cues,
        key=lambda cue: (_cue_signal_score(cue), cue.endSeconds - cue.startSeconds),
        reverse=True,
    )

    selected: list[TranscriptCue] = []
    for cue in ranked:
        center = (cue.startSeconds + cue.endSeconds) / 2
        if any(abs(center - ((item.startSeconds + item.endSeconds) / 2)) < minimum_gap for item in selected):
            continue
        selected.append(cue)
        if len(selected) >= cue_limit:
            break

    if len(selected) < cue_limit:
        for cue in transcript_cues:
            if cue in selected:
                continue
            selected.append(cue)
            if len(selected) >= cue_limit:
                break

    return selected[:cue_limit]


def select_signal_timestamps(context: PlannerContext, frame_count: int) -> tuple[list[float], list[TranscriptCue]]:
    transcript_cues = parse_transcript_cues(context.transcriptText)
    selected = select_transcript_signal_cues(
        transcript_cues=transcript_cues,
        duration_seconds=context.source.durationSeconds,
        frame_count=frame_count,
    )
    timestamps = [round((cue.startSeconds + cue.endSeconds) / 2, 3) for cue in selected]
    return timestamps, selected


def select_audio_signal_timestamps(context: PlannerContext, frame_count: int) -> list[tuple[float, float]]:
    if not context.sourcePath or not context.source.hasAudio:
        return []
    try:
        peaks = sample_audio_peaks(
            source_path=context.sourcePath,
            duration_seconds=context.source.durationSeconds,
            peak_count=frame_count,
        )
    except Exception:
        return []
    return [(peak.timestamp_seconds, peak.energy) for peak in peaks]


def merge_signal_timestamps(
    transcript_timestamps: list[float],
    audio_peak_timestamps: list[tuple[float, float]],
    duration_seconds: float,
    limit: int,
) -> list[float]:
    safe_duration = max(0.6, duration_seconds)
    minimum_gap = max(2.5, safe_duration / max(4.0, limit * 1.4))
    candidates: list[tuple[float, float]] = []
    for timestamp in transcript_timestamps:
        candidates.append((timestamp, 4.0))
    for timestamp, energy in audio_peak_timestamps:
        candidates.append((timestamp, 1.5 + min(energy * 12.0, 3.0)))
    if not candidates:
        return []

    selected: list[tuple[float, float]] = []
    for timestamp, weight in sorted(candidates, key=lambda item: item[1], reverse=True):
        if any(abs(timestamp - existing[0]) < minimum_gap for existing in selected):
            continue
        selected.append((timestamp, weight))
        if len(selected) >= limit:
            break
    return sorted(round(timestamp, 3) for timestamp, _ in selected)


def collect_signal_bundle(context: PlannerContext, frame_count: int) -> SignalBundle:
    transcript_cues = parse_transcript_cues(context.transcriptText)
    selected_transcript_cues = select_transcript_signal_cues(
        transcript_cues=transcript_cues,
        duration_seconds=context.source.durationSeconds,
        frame_count=frame_count,
    )
    transcript_timestamps = [
        round((cue.startSeconds + cue.endSeconds) / 2, 3)
        for cue in selected_transcript_cues
    ]
    audio_peaks = select_audio_signal_timestamps(context, frame_count)
    return SignalBundle(
        transcriptCues=transcript_cues,
        selectedTranscriptCues=selected_transcript_cues,
        transcriptTimestamps=transcript_timestamps,
        audioPeaks=audio_peaks,
    )


def _signal_sources(context: PlannerContext, signals: SignalBundle) -> list[str]:
    return [
        "video_frames",
        "timed_transcript" if signals.transcriptCues else ("creative_prompt" if context.task.creativePrompt else "no_transcript"),
        "audio_peaks" if signals.audioPeaks else "no_audio_signal",
    ]


class VisionEventAnalyzer:
    def __init__(self, settings: Settings):
        self.settings = settings

    def _build_messages(
        self,
        context: PlannerContext,
        model_name: str,
        signals: SignalBundle,
    ) -> tuple[list[dict[str, object]], list[float], dict[str, object]]:
        if not context.sourcePath:
            raise RuntimeError("video source path is missing")

        with tempfile.TemporaryDirectory(prefix="ai-cut-vision-") as temp_dir:
            merged_timestamps = merge_signal_timestamps(
                transcript_timestamps=signals.transcriptTimestamps,
                audio_peak_timestamps=signals.audioPeaks,
                duration_seconds=context.source.durationSeconds,
                limit=self.settings.model.vision_frame_count,
            )
            samples = sample_video_frames(
                source_path=context.sourcePath,
                output_dir=temp_dir,
                duration_seconds=context.source.durationSeconds,
                frame_count=self.settings.model.vision_frame_count,
                timestamps=merged_timestamps or signals.transcriptTimestamps or None,
            )
            if not samples:
                raise RuntimeError("no visual samples extracted")

            content: list[dict[str, object]] = [
                {
                    "type": "text",
                    "text": (
                        "你是短剧视频内容理解助手，只输出 JSON。"
                        "你会看到一组按时间采样的关键帧，每张图前都标明对应秒数。"
                        "如果给了带时间戳字幕，请把字幕视为第二路强信号，优先关注字幕冲突点附近的画面。"
                        "如果给了音频峰值时间点，请把音频卡点视为第三路强信号，优先关注节奏爆点附近的画面。"
                        "请识别最适合短视频剪辑的高燃、冲突、反转、悬念、情绪爆发或视觉卡点，并输出 events。"
                        "不要直接输出最终剪辑片段，不要机械地按开头、中间、结尾平均分配。"
                        "输出格式必须为："
                        '{"events":[{"eventIndex":1,"title":"...","reason":"...","timestampSeconds":12.3,"confidence":0.92,"eventType":"冲突升级","relatedSubtitle":"..."}]}\n'
                        f"任务要求：{json.dumps(context.task.model_dump(), ensure_ascii=False)}\n"
                        f"视频信息：{json.dumps(context.source.model_dump(), ensure_ascii=False)}\n"
                        f"创意补充：{json.dumps((context.task.creativePrompt or '').strip(), ensure_ascii=False)}\n"
                        f"字幕摘录：{json.dumps(truncate_transcript(context.transcriptText, 320), ensure_ascii=False)}\n"
                        f"强信号字幕时间轴：{json.dumps([cue.__dict__ for cue in signals.selectedTranscriptCues[:12]], ensure_ascii=False)}\n"
                        f"音频峰值时间点：{json.dumps([{'timestampSeconds': timestamp, 'energy': round(energy, 4)} for timestamp, energy in signals.audioPeaks[:12]], ensure_ascii=False)}\n"
                        "要求："
                        "1. 识别 4-8 个最值得剪辑的剧情/情绪事件。"
                        "2. timestampSeconds 必须指向事件最强的画面或情绪时间点。"
                        "3. reason 要说明这是哪个冲突/反转/卡点。"
                        "4. eventType 需要是类似 冲突升级/情绪爆发/反转揭示/悬念停顿/动作卡点 的短标签。"
                        "5. relatedSubtitle 尽量填与该事件最相关的一句字幕，没有就留空。"
                    ),
                }
            ]
            timestamps: list[float] = []
            for index, sample in enumerate(samples, start=1):
                timestamps.append(sample.timestamp_seconds)
                content.append(
                    {
                        "type": "text",
                        "text": f"关键帧 {index}，对应时间点 {sample.timestamp_seconds:.1f} 秒。",
                    }
                )
                content.append(
                    {
                        "type": "image_url",
                        "image_url": {
                            "url": _image_data_url(sample.image_path),
                        },
                    }
                )
            messages = [
                {"role": "system", "content": "You are a precise JSON-only video highlight planner."},
                {"role": "user", "content": content},
            ]
        signal_meta = {
            "audio_peak_count": len(signals.audioPeaks),
            "transcript_cue_count": len(signals.transcriptCues),
            "signal_sources": _signal_sources(context, signals),
        }
        return messages, timestamps, signal_meta

    def _call_model(self, model_name: str, context: PlannerContext, signals: SignalBundle) -> VisionAnalysisResult:
        messages, frame_timestamps, signal_meta = self._build_messages(context, model_name, signals)
        if context.trace is not None:
            context.trace(
                "vision",
                "vision.request",
                f"已向视觉模型 {model_name} 发送画面事件识别请求。",
                {
                    "model": model_name,
                    "frame_count": len(frame_timestamps),
                    "frame_timestamps": frame_timestamps,
                    "has_transcript": bool(signals.transcriptCues),
                    **signal_meta,
                },
                "INFO",
            )
        payload = {
            "model": model_name,
            "messages": messages,
            "temperature": self.settings.model.temperature,
            "max_tokens": self.settings.model.max_tokens,
        }
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        request = urllib.request.Request(
            self.settings.model.endpoint,
            data=data,
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {self.settings.model.api_key}",
            },
        )
        try:
            with urllib.request.urlopen(request, timeout=self.settings.model.timeout_seconds) as response:
                raw = response.read().decode("utf-8")
        except (TimeoutError, socket.timeout) as exc:
            if context.trace is not None:
                context.trace(
                    "vision",
                    "vision.timeout",
                    f"视觉模型 {model_name} 在 {self.settings.model.timeout_seconds} 秒内没有返回结果。",
                    {
                        "model": model_name,
                        "timeout_seconds": self.settings.model.timeout_seconds,
                        "frame_count": len(frame_timestamps),
                    },
                    "ERROR",
                )
            raise RuntimeError(f"Vision request timed out ({model_name}): {exc}") from exc
        except urllib.error.HTTPError as exc:
            body = exc.read().decode("utf-8", errors="ignore")
            if context.trace is not None:
                context.trace(
                    "vision",
                    "vision.http_error",
                    f"视觉模型 {model_name} 请求失败，返回 HTTP {exc.code}。",
                    {
                        "model": model_name,
                        "status_code": exc.code,
                        "response_excerpt": truncate_transcript(body, 1000),
                    },
                    "ERROR",
                )
            raise RuntimeError(f"Vision request failed ({model_name}): {exc.code} {body[:400]}") from exc
        except urllib.error.URLError as exc:
            if context.trace is not None:
                context.trace(
                    "vision",
                    "vision.network_error",
                    f"视觉模型 {model_name} 请求在收到响应前失败。",
                    {
                        "model": model_name,
                        "error": str(exc),
                    },
                    "ERROR",
                )
            raise RuntimeError(f"Vision request failed ({model_name}): {exc}") from exc

        body = json.loads(raw)
        content = ""
        if isinstance(body, dict):
            if "choices" in body and body["choices"]:
                message = body["choices"][0].get("message", {})
                content = message.get("content", "")
            elif "output" in body:
                output = body["output"]
                if isinstance(output, dict):
                    content = output.get("text", "") or output.get("content", "")
        json_text = extract_json_object(content or raw)
        parsed = json.loads(json_text)
        events = parsed.get("events", [])
        if context.trace is not None:
            context.trace(
                "vision",
                "vision.response",
                f"视觉模型 {model_name} 已返回画面事件理解结果。",
                {
                    "model": model_name,
                    "frame_count": len(frame_timestamps),
                    "frame_timestamps": frame_timestamps,
                    "parsed_event_count": len(events) if isinstance(events, list) else 0,
                    "event_titles": [
                        str(event.get("title", f"事件 {index + 1}"))[:80]
                        for index, event in enumerate(events[:6] if isinstance(events, list) else [])
                    ],
                    "event_types": [
                        str(event.get("eventType", ""))[:32]
                        for event in (events[:6] if isinstance(events, list) else [])
                        if str(event.get("eventType", "")).strip()
                    ],
                    "content_excerpt": truncate_transcript(content or raw, 1000),
                },
                "INFO",
            )
        result: list[VisualInsight] = []
        for index, event in enumerate(events, start=1):
            result.append(
                VisualInsight(
                    title=str(event.get("title", f"事件 {index}")),
                    reason=str(event.get("reason", "由视觉模型识别到的剧情高点")),
                    focusSeconds=float(event.get("timestampSeconds", event.get("focusSeconds", 0.0))),
                    confidence=float(event.get("confidence")) if event.get("confidence") is not None else None,
                    eventType=str(event.get("eventType", "")).strip() or None,
                    relatedSubtitle=str(event.get("relatedSubtitle", "")).strip() or None,
                )
            )
        return VisionAnalysisResult(
            modelName=model_name,
            events=result,
            frameTimestamps=frame_timestamps,
            signals=signals,
        )

    def analyze(self, context: PlannerContext, signals: SignalBundle) -> VisionAnalysisResult | None:
        if not self.settings.model.api_key or not self.settings.model.endpoint or not context.sourcePath:
            return None

        model_names = [self.settings.model.vision_model_name or ""]
        if self.settings.model.vision_fallback_model_name:
            model_names.append(self.settings.model.vision_fallback_model_name)

        last_error: Exception | None = None
        for model_name in [item for item in model_names if item]:
            try:
                if context.trace is not None:
                    context.trace(
                        "vision",
                        "vision.attempt",
                        f"开始调用视觉模型 {model_name} 分析视频内容高点。",
                        {
                            "model": model_name,
                            "frame_count": self.settings.model.vision_frame_count,
                        },
                        "INFO",
                    )
                analysis = self._call_model(model_name, context, signals)
                if analysis.events:
                    return analysis
                return analysis
            except Exception as exc:
                if context.trace is not None:
                    context.trace(
                        "vision",
                        "vision.attempt_failed",
                        f"视觉模型 {model_name} 调用失败，准备尝试下一个回退模型。",
                        {
                            "model": model_name,
                            "error": str(exc),
                        },
                        "WARN",
                    )
                last_error = exc
        if last_error is not None:
            raise last_error
        return None


class FusionPlanner:
    def __init__(self, settings: Settings, vision_analyzer: VisionEventAnalyzer | None = None):
        self.settings = settings
        self.vision_analyzer = vision_analyzer

    def _trace_signal_candidates(self, context: PlannerContext, signals: SignalBundle) -> None:
        if context.trace is None:
            return
        if signals.audioPeaks:
            context.trace(
                "audio",
                "audio.peaks_detected",
                "已检测到音频峰值卡点，加入切点候选。",
                {
                    "peak_count": len(signals.audioPeaks),
                    "audio_peak_count": len(signals.audioPeaks),
                    "peaks": [
                        {"timestamp_seconds": timestamp, "energy": round(energy, 4)}
                        for timestamp, energy in signals.audioPeaks[:12]
                    ],
                },
                "INFO",
            )
        if signals.selectedTranscriptCues:
            context.trace(
                "planning",
                "planning.subtitle_signals",
                "已提取字幕强信号时间轴，加入切点候选。",
                {
                    "transcript_cue_count": len(signals.selectedTranscriptCues),
                    "signal_timestamps": signals.transcriptTimestamps[:12],
                },
                "INFO",
            )

    def _build_prompt(
        self,
        context: PlannerContext,
        signals: SignalBundle,
        vision_analysis: VisionAnalysisResult | None,
    ) -> str:
        task = context.task.model_dump()
        source = context.source.model_dump()
        candidates = HeuristicPlanner(self.settings).plan(
            PlannerContext(
                task=context.task,
                source=context.source,
                transcriptText=context.transcriptText,
                trace=None,
            )
        )
        transcript_excerpt = truncate_transcript(context.transcriptText)
        transcript_payload = [cue.__dict__ for cue in signals.transcriptCues[:48]]
        signal_transcript_payload = [cue.__dict__ for cue in signals.selectedTranscriptCues[:12]]
        audio_peaks_payload = [
            {"timestampSeconds": timestamp, "energy": round(energy, 4)}
            for timestamp, energy in signals.audioPeaks[:12]
        ]
        visual_events_payload = [
            {
                "eventIndex": index,
                "title": event.title,
                "reason": event.reason,
                "timestampSeconds": event.focusSeconds,
                "confidence": event.confidence,
                "eventType": event.eventType,
                "relatedSubtitle": event.relatedSubtitle,
            }
            for index, event in enumerate(vision_analysis.events[:8], start=1)
        ] if vision_analysis else []
        return (
            "你是一个短剧投放剪辑融合规划引擎，只输出 JSON，不要解释。\n"
            "目标：基于视觉事件理解、字幕时间轴、音频卡点和候选片段，输出更容易停留、冲突更强、信息更完整的切条方案。\n"
            "你必须优先利用上游视觉事件和带时间戳字幕来决定切点；如果视觉事件为空，再退化为基于字幕、音频和候选片段规划。\n"
            "任务要求如下：\n"
            f"{json.dumps(task, ensure_ascii=False)}\n"
            "视频信息如下：\n"
            f"{json.dumps(source, ensure_ascii=False)}\n"
            f"视觉事件理解结果（若为空表示视觉模型未返回可用事件）：\n{json.dumps(visual_events_payload, ensure_ascii=False)}\n"
            f"视觉事件来源模型：{json.dumps(vision_analysis.modelName if vision_analysis else '', ensure_ascii=False)}\n"
            f"强信号字幕时间轴：\n{json.dumps(signal_transcript_payload, ensure_ascii=False)}\n"
            f"音频峰值时间点：\n{json.dumps(audio_peaks_payload, ensure_ascii=False)}\n"
            "如果提供了带时间戳字幕/台词，请将切点尽量贴近字幕时间边界，并确保不要把人物对白从中间截断。"
            "开头和结尾尽量贴近动作、句子或情绪落点边界，不要从连续动作的半截突然切入或切出。\n"
            "请在候选片段基础上优化标题、理由和具体时间点，输出格式必须为："
            '{"clips":[{"clipIndex":1,"title":"...","reason":"...","startSeconds":0,"endSeconds":10,"durationSeconds":10}]}\n'
            "候选片段：\n"
            f"{json.dumps([c.model_dump() for c in candidates], ensure_ascii=False)}\n"
            "带时间戳字幕/台词（若为空则表示未提供）：\n"
            f"{json.dumps(transcript_payload, ensure_ascii=False)}\n"
            "原始文本摘录（若为空则表示未提供）：\n"
            f"{json.dumps(transcript_excerpt, ensure_ascii=False)}\n"
            "要求：\n"
            "1. 输出 clips 数量必须等于 outputCount。\n"
            "2. startSeconds/endSeconds 必须围绕视觉事件或字幕冲突点，不要均匀平铺。\n"
            "3. durationSeconds 必须等于 endSeconds-startSeconds，且满足时长区间。\n"
            "4. title 要像投放素材标题，reason 要说明这是哪个冲突/反转/高燃卡点。\n"
            "5. 优先完整保留一句关键对白或一个完整动作，不要切断句子中段。\n"
        )

    def _call_model(
        self,
        model_name: str,
        context: PlannerContext,
        signals: SignalBundle,
        vision_analysis: VisionAnalysisResult | None,
    ) -> list[ClipPlan]:
        prompt = self._build_prompt(context, signals, vision_analysis)
        if context.trace is not None:
            context.trace(
                "fusion",
                "fusion.request",
                f"已向融合规划模型 {model_name} 发送最终剪辑规划请求。",
                {
                    "model": model_name,
                    "temperature": self.settings.model.temperature,
                    "max_tokens": self.settings.model.max_tokens,
                    "prompt_length": len(prompt),
                    "prompt_excerpt": truncate_transcript(prompt, 1200),
                    "has_transcript": bool(signals.transcriptCues),
                    "transcript_cue_count": len(signals.transcriptCues),
                    "audio_peak_count": len(signals.audioPeaks),
                    "visual_event_count": len(vision_analysis.events) if vision_analysis else 0,
                    "used_visual_events": bool(vision_analysis and vision_analysis.events),
                    "visual_model": vision_analysis.modelName if vision_analysis else "",
                    "signal_sources": _signal_sources(context, signals),
                },
                "INFO",
            )
        payload = {
            "model": model_name,
            "messages": [
                {"role": "system", "content": "You are a precise JSON-only planning engine."},
                {"role": "user", "content": prompt},
            ],
            "temperature": self.settings.model.temperature,
            "max_tokens": self.settings.model.max_tokens,
        }
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        request = urllib.request.Request(
            self.settings.model.endpoint,
            data=data,
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {self.settings.model.api_key}",
            },
        )
        try:
            with urllib.request.urlopen(request, timeout=self.settings.model.timeout_seconds) as response:
                raw = response.read().decode("utf-8")
        except (TimeoutError, socket.timeout) as exc:
            if context.trace is not None:
                context.trace(
                    "fusion",
                    "fusion.timeout",
                    f"融合规划模型 {model_name} 在 {self.settings.model.timeout_seconds} 秒内没有返回结果。",
                    {
                        "model": model_name,
                        "timeout_seconds": self.settings.model.timeout_seconds,
                        "prompt_length": len(prompt),
                    },
                    "ERROR",
                )
            raise RuntimeError(f"Fusion request timed out ({model_name}): {exc}") from exc
        except urllib.error.HTTPError as exc:
            body = exc.read().decode("utf-8", errors="ignore")
            if context.trace is not None:
                context.trace(
                    "fusion",
                    "fusion.http_error",
                    f"融合规划模型 {model_name} 请求失败，返回 HTTP {exc.code}。",
                    {
                        "model": model_name,
                        "status_code": exc.code,
                        "response_excerpt": truncate_transcript(body, 1200),
                    },
                    "ERROR",
                )
            raise RuntimeError(f"Fusion request failed ({model_name}): {exc.code} {body[:400]}") from exc
        except urllib.error.URLError as exc:
            if context.trace is not None:
                context.trace(
                    "fusion",
                    "fusion.network_error",
                    f"融合规划模型 {model_name} 请求在收到响应前失败。",
                    {
                        "model": model_name,
                        "error": str(exc),
                    },
                    "ERROR",
                )
            raise RuntimeError(f"Fusion request failed ({model_name}): {exc}") from exc

        body = json.loads(raw)
        content = ""
        if isinstance(body, dict):
            if "choices" in body and body["choices"]:
                message = body["choices"][0].get("message", {})
                content = message.get("content", "")
            elif "output" in body:
                output = body["output"]
                if isinstance(output, dict):
                    content = output.get("text", "") or output.get("content", "")
        json_text = extract_json_object(content or raw)
        parsed = json.loads(json_text)
        clips = parsed.get("clips", [])
        if context.trace is not None:
            context.trace(
                "fusion",
                "fusion.response",
                f"融合规划模型 {model_name} 已返回最终剪辑方案。",
                {
                    "model": model_name,
                    "raw_length": len(raw),
                    "content_excerpt": truncate_transcript(content or raw, 1200),
                    "parsed_clip_count": len(clips) if isinstance(clips, list) else 0,
                    "visual_event_count": len(vision_analysis.events) if vision_analysis else 0,
                    "used_visual_events": bool(vision_analysis and vision_analysis.events),
                    "visual_model": vision_analysis.modelName if vision_analysis else "",
                    "clip_titles": [
                        str(clip.get("title", f"素材 {index + 1}"))[:80]
                        for index, clip in enumerate(clips[:5] if isinstance(clips, list) else [])
                    ],
                },
                "INFO",
            )
        result: list[ClipPlan] = []
        for index, clip in enumerate(clips, start=1):
            result.append(
                ClipPlan(
                    clipIndex=int(clip.get("clipIndex", index)),
                    title=str(clip.get("title", f"素材 {index}")),
                    reason=str(clip.get("reason", "由模型规划生成")),
                    startSeconds=float(clip.get("startSeconds", 0.0)),
                    endSeconds=float(clip.get("endSeconds", 0.0)),
                    durationSeconds=float(clip.get("durationSeconds", 0.0)),
                )
            )
        return result

    def plan(self, context: PlannerContext) -> list[ClipPlan]:
        if not self.settings.model.api_key or not self.settings.model.endpoint:
            raise RuntimeError("Fusion planner provider is not configured")

        signals = collect_signal_bundle(context, self.settings.model.vision_frame_count)
        self._trace_signal_candidates(context, signals)

        vision_analysis: VisionAnalysisResult | None = None
        if self.vision_analyzer is not None and context.sourcePath:
            try:
                vision_analysis = self.vision_analyzer.analyze(context, signals)
            except Exception as exc:
                if context.trace is not None:
                    context.trace(
                        "fusion",
                        "fusion.vision_fallback",
                        "视觉事件识别失败，改用字幕、音频和候选片段继续规划。",
                        {
                            "error": str(exc),
                            "transcript_cue_count": len(signals.transcriptCues),
                            "audio_peak_count": len(signals.audioPeaks),
                        },
                        "WARN",
                    )

        model_names = [self.settings.model.model_name]
        if self.settings.model.fallback_model_name:
            model_names.append(self.settings.model.fallback_model_name)

        last_error: Exception | None = None
        for model_name in model_names:
            try:
                if context.trace is not None:
                    context.trace(
                        "fusion",
                        "fusion.attempt",
                        f"开始调用融合规划模型 {model_name} 生成最终剪辑方案。",
                        {
                            "model": model_name,
                            "visual_event_count": len(vision_analysis.events) if vision_analysis else 0,
                            "used_visual_events": bool(vision_analysis and vision_analysis.events),
                            "visual_model": vision_analysis.modelName if vision_analysis else "",
                        },
                        "INFO",
                    )
                clips = self._call_model(model_name, context, signals, vision_analysis)
                if clips:
                    return clips
            except Exception as exc:
                if context.trace is not None:
                    context.trace(
                        "fusion",
                        "fusion.attempt_failed",
                        f"融合规划模型 {model_name} 调用失败，准备尝试下一个回退模型。",
                        {
                            "model": model_name,
                            "error": str(exc),
                        },
                        "WARN",
                    )
                last_error = exc
        if last_error is not None:
            raise last_error
        return []


class PlannerChain:
    def __init__(self, planners: list[Planner]):
        self.planners = planners

    def plan(self, context: PlannerContext) -> list[ClipPlan]:
        last_error: Exception | None = None
        for planner in self.planners:
            try:
                clips = planner.plan(context)
                if clips:
                    return clips
            except Exception as exc:  # noqa: PERF203 - planner fallback
                last_error = exc
        if last_error is not None:
            raise last_error
        return []


def build_planner(settings: Settings) -> PlannerChain:
    planners: list[Planner] = []
    if settings.model.provider.lower() == "qwen":
        planners.append(FusionPlanner(settings, vision_analyzer=VisionEventAnalyzer(settings)))
    planners.append(HeuristicPlanner(settings))
    return PlannerChain(planners)


_SRT_BLOCK_RE = re.compile(
    r"(?:^\s*\d+\s*$\n)?^\s*(?P<start>\d{1,2}:\d{2}(?::\d{2})?(?:[,.]\d{1,3})?)\s*-->\s*(?P<end>\d{1,2}:\d{2}(?::\d{2})?(?:[,.]\d{1,3})?)\s*$\n(?P<text>.*?)(?=\n{2,}|\Z)",
    re.MULTILINE | re.DOTALL,
)


def parse_timestamp_to_seconds(value: str) -> float:
    normalized = value.strip().replace(",", ".")
    parts = normalized.split(":")
    if len(parts) == 2:
        hours = 0
        minutes, seconds = parts
    elif len(parts) == 3:
        hours, minutes, seconds = parts
    else:
        raise ValueError(f"invalid timestamp: {value}")
    return int(hours) * 3600 + int(minutes) * 60 + float(seconds)


def parse_transcript_cues(text: str | None) -> list[TranscriptCue]:
    if not text or not text.strip():
        return []

    cues: list[TranscriptCue] = []
    for match in _SRT_BLOCK_RE.finditer(text.strip()):
        raw_text = " ".join(line.strip() for line in match.group("text").splitlines() if line.strip())
        if not raw_text:
            continue
        try:
            start_seconds = parse_timestamp_to_seconds(match.group("start"))
            end_seconds = parse_timestamp_to_seconds(match.group("end"))
        except Exception:
            continue
        if end_seconds <= start_seconds:
            continue
        cues.append(
            TranscriptCue(
                startSeconds=round(start_seconds, 3),
                endSeconds=round(end_seconds, 3),
                text=raw_text[:500],
            )
        )
    return cues


def truncate_transcript(text: str | None, limit: int = 4000) -> str:
    if not text:
        return ""
    stripped = text.strip()
    if len(stripped) <= limit:
        return stripped
    return stripped[: limit - 3] + "..."


def _pick_cue(cues: list[TranscriptCue], index: int, clip_count: int) -> TranscriptCue | None:
    if not cues:
        return None
    if clip_count <= 1:
        return cues[len(cues) // 2]
    position = round(index * (len(cues) - 1) / max(1, clip_count - 1))
    return cues[position]
