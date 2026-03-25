from __future__ import annotations

from dataclasses import dataclass
from array import array
from pathlib import Path
import json
import math
import subprocess
import tempfile
from typing import Iterable
import wave

from .schemas import MediaProbe


class MediaToolError(RuntimeError):
    pass


@dataclass(frozen=True)
class RenderResult:
    output_path: Path
    duration_seconds: float


@dataclass(frozen=True)
class VideoFrameSample:
    timestamp_seconds: float
    image_path: Path


@dataclass(frozen=True)
class AudioPeakSample:
    timestamp_seconds: float
    energy: float


def _run_command(command: list[str]) -> str:
    completed = subprocess.run(command, capture_output=True, text=True)
    if completed.returncode != 0:
        stderr = completed.stderr.strip() or completed.stdout.strip() or "media command failed"
        raise MediaToolError(stderr)
    return completed.stdout


def probe_media(path: str | Path) -> MediaProbe:
    command = [
        "ffprobe",
        "-v",
        "error",
        "-print_format",
        "json",
        "-show_format",
        "-show_streams",
        str(path),
    ]
    raw = _run_command(command)
    payload = json.loads(raw)
    streams = payload.get("streams", [])
    fmt = payload.get("format", {})

    video_stream = next((stream for stream in streams if stream.get("codec_type") == "video"), {})
    audio_stream = next((stream for stream in streams if stream.get("codec_type") == "audio"), None)
    duration = float(fmt.get("duration") or video_stream.get("duration") or 0.0)

    fps = None
    rate = video_stream.get("avg_frame_rate") or video_stream.get("r_frame_rate")
    if isinstance(rate, str) and rate not in {"0/0", "0"} and "/" in rate:
        numerator, denominator = rate.split("/", 1)
        try:
            fps = float(numerator) / float(denominator)
        except Exception:
            fps = None

    return MediaProbe(
        durationSeconds=duration,
        width=int(video_stream.get("width") or 0),
        height=int(video_stream.get("height") or 0),
        hasAudio=audio_stream is not None,
        fps=fps,
    )


def sample_video_frames(
    source_path: str | Path,
    output_dir: str | Path,
    duration_seconds: float,
    frame_count: int = 6,
    max_width: int = 576,
    timestamps: list[float] | None = None,
) -> list[VideoFrameSample]:
    source = Path(source_path)
    target_dir = Path(output_dir)
    target_dir.mkdir(parents=True, exist_ok=True)

    safe_duration = max(0.6, float(duration_seconds))
    if timestamps:
        raw_timestamps = [
            min(max(0.0, float(timestamp)), max(0.0, safe_duration - 0.2))
            for timestamp in timestamps[:12]
        ]
    else:
        normalized_count = max(1, min(frame_count, 12))
        raw_timestamps = [
            min(max(0.0, safe_duration * ((index + 0.5) / normalized_count)), max(0.0, safe_duration - 0.2))
            for index in range(normalized_count)
        ]

    samples: list[VideoFrameSample] = []
    seen: set[float] = set()
    for index, timestamp in enumerate(raw_timestamps, start=1):
        rounded = round(timestamp, 3)
        if rounded in seen:
            continue
        seen.add(rounded)
        image_path = target_dir / f"frame_{index:02d}.jpg"
        command = [
            "ffmpeg",
            "-y",
            "-ss",
            f"{rounded:.3f}",
            "-i",
            str(source),
            "-frames:v",
            "1",
            "-vf",
            f"scale={max_width}:-2:force_original_aspect_ratio=decrease",
            "-q:v",
            "5",
            str(image_path),
        ]
        _run_command(command)
        if image_path.exists():
            samples.append(VideoFrameSample(timestamp_seconds=rounded, image_path=image_path))
    return samples


def sample_audio_peaks(
    source_path: str | Path,
    duration_seconds: float,
    peak_count: int = 6,
    sample_rate: int = 16000,
    window_seconds: float = 1.0,
) -> list[AudioPeakSample]:
    safe_duration = max(0.6, float(duration_seconds))
    normalized_count = max(1, min(peak_count, 12))
    with tempfile.TemporaryDirectory(prefix="ai-cut-audio-") as temp_dir:
        wav_path = Path(temp_dir) / "mono.wav"
        command = [
            "ffmpeg",
            "-y",
            "-i",
            str(source_path),
            "-vn",
            "-ac",
            "1",
            "-ar",
            str(sample_rate),
            "-acodec",
            "pcm_s16le",
            str(wav_path),
        ]
        _run_command(command)

        with wave.open(str(wav_path), "rb") as handle:
            frames = handle.readframes(handle.getnframes())
            rate = handle.getframerate()

    samples = array("h")
    samples.frombytes(frames)
    if not samples:
        return []

    window_size = max(1, int(rate * max(0.4, window_seconds)))
    minimum_gap = max(2.5, safe_duration / max(4.0, normalized_count * 1.4))
    windows: list[AudioPeakSample] = []
    for start in range(0, len(samples), window_size):
        chunk = samples[start : start + window_size]
        if not chunk:
            continue
        squares = sum(int(sample) * int(sample) for sample in chunk)
        rms = math.sqrt(squares / max(1, len(chunk))) / 32768.0
        timestamp = min(max(0.0, (start / rate) + (len(chunk) / rate) / 2), max(0.0, safe_duration - 0.2))
        windows.append(AudioPeakSample(timestamp_seconds=round(timestamp, 3), energy=round(rms, 6)))

    ranked = sorted(windows, key=lambda item: item.energy, reverse=True)
    selected: list[AudioPeakSample] = []
    for peak in ranked:
        if peak.energy <= 0:
            continue
        if any(abs(peak.timestamp_seconds - item.timestamp_seconds) < minimum_gap for item in selected):
            continue
        selected.append(peak)
        if len(selected) >= normalized_count:
            break
    return sorted(selected, key=lambda item: item.timestamp_seconds)


def target_resolution(aspect_ratio: str) -> tuple[int, int]:
    if aspect_ratio == "16:9":
        return 1920, 1080
    return 1080, 1920


def template_color(name: str) -> str:
    palette = {
        "hook": "#ff7a18",
        "brand": "#0f766e",
        "call_to_action": "#16a34a",
        "none": "#111827",
    }
    return palette.get(name, "#334155")


def _render_slate(path: Path, name: str, duration: float, resolution: tuple[int, int]) -> None:
    width, height = resolution
    command = [
        "ffmpeg",
        "-y",
        "-f",
        "lavfi",
        "-i",
        f"color=c={template_color(name)}:s={width}x{height}:r=30:d={duration}",
        "-f",
        "lavfi",
        "-i",
        "anullsrc=channel_layout=stereo:sample_rate=44100",
        "-map",
        "0:v:0",
        "-map",
        "1:a:0",
        "-c:v",
        "libx264",
        "-preset",
        "veryfast",
        "-crf",
        "18",
        "-pix_fmt",
        "yuv420p",
        "-c:a",
        "aac",
        "-ar",
        "44100",
        "-ac",
        "2",
        "-shortest",
        "-movflags",
        "+faststart",
        str(path),
    ]
    _run_command(command)


def _render_cut(
    path: Path,
    source_path: Path,
    start_seconds: float,
    end_seconds: float,
    resolution: tuple[int, int],
    has_audio: bool,
) -> None:
    width, height = resolution
    duration = max(0.5, end_seconds - start_seconds)
    video_filter = f"scale={width}:{height}:force_original_aspect_ratio=increase,crop={width}:{height},fps=30,format=yuv420p"
    command = [
        "ffmpeg",
        "-y",
        "-ss",
        f"{start_seconds:.3f}",
        "-i",
        str(source_path),
        "-t",
        f"{duration:.3f}",
    ]
    if has_audio:
        command.extend(
            [
                "-map",
                "0:v:0",
                "-map",
                "0:a:0?",
                "-vf",
                video_filter,
                "-c:v",
                "libx264",
                "-preset",
                "veryfast",
                "-crf",
                "20",
                "-c:a",
                "aac",
                "-ar",
                "44100",
                "-ac",
                "2",
                "-movflags",
                "+faststart",
                str(path),
            ]
        )
    else:
        command.extend(
            [
                "-f",
                "lavfi",
                "-i",
                "anullsrc=channel_layout=stereo:sample_rate=44100",
                "-map",
                "0:v:0",
                "-map",
                "1:a:0",
                "-vf",
                video_filter,
                "-c:v",
                "libx264",
                "-preset",
                "veryfast",
                "-crf",
                "20",
                "-c:a",
                "aac",
                "-ar",
                "44100",
                "-ac",
                "2",
                "-shortest",
                "-movflags",
                "+faststart",
                str(path),
            ]
        )
    _run_command(command)


def _concat_segments(output_path: Path, segments: Iterable[Path]) -> None:
    segment_list = list(segments)
    if len(segment_list) == 1:
        segment_list[0].replace(output_path)
        return

    with tempfile.NamedTemporaryFile("w", suffix=".txt", delete=False) as handle:
        list_file = Path(handle.name)
        for segment in segment_list:
            handle.write(f"file '{segment.as_posix()}'\n")

    try:
        command = [
            "ffmpeg",
            "-y",
            "-f",
            "concat",
            "-safe",
            "0",
            "-i",
            str(list_file),
            "-c",
            "copy",
            "-movflags",
            "+faststart",
            str(output_path),
        ]
        _run_command(command)
    finally:
        if list_file.exists():
            list_file.unlink()


def render_output(
    source_path: Path,
    output_path: Path,
    start_seconds: float,
    end_seconds: float,
    aspect_ratio: str,
    intro_template: str,
    outro_template: str,
    has_audio: bool,
) -> RenderResult:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    resolution = target_resolution(aspect_ratio)
    duration = max(0.5, end_seconds - start_seconds)

    with tempfile.TemporaryDirectory(prefix="ai-cut-render-") as temp_dir:
        temp_root = Path(temp_dir)
        cut_path = temp_root / "cut.mp4"
        _render_cut(cut_path, source_path, start_seconds, end_seconds, resolution, has_audio)

        segments: list[Path] = []
        if intro_template and intro_template != "none":
            intro_path = temp_root / "intro.mp4"
            _render_slate(intro_path, intro_template, 1.2, resolution)
            segments.append(intro_path)
        segments.append(cut_path)
        if outro_template and outro_template != "none":
            outro_path = temp_root / "outro.mp4"
            _render_slate(outro_path, outro_template, 1.2, resolution)
            segments.append(outro_path)
        _concat_segments(output_path, segments)

    return RenderResult(output_path=output_path, duration_seconds=duration)
