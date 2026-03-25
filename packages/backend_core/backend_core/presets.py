from __future__ import annotations

from .schemas import TaskPreset


TASK_PRESETS: tuple[TaskPreset, ...] = (
    TaskPreset(
        key="douyin_hotcut",
        name="抖音爆款切条",
        description="适合强冲突、强反转的短剧高能片段，优先提升首刷停留。",
        defaultTitle="抖音爆款版",
        platform="douyin",
        aspectRatio="9:16",
        minDurationSeconds=15,
        maxDurationSeconds=30,
        outputCount=3,
        introTemplate="hook",
        outroTemplate="brand",
        creativePrompt="优先保留冲突、反转和情绪爆点，节奏更快，适合短视频投放。",
    ),
    TaskPreset(
        key="feed_conversion",
        name="信息流转化版",
        description="适合广告投放和素材 A/B 测试，突出卖点和行动召唤。",
        defaultTitle="信息流投放版",
        platform="wechat",
        aspectRatio="9:16",
        minDurationSeconds=20,
        maxDurationSeconds=35,
        outputCount=4,
        introTemplate="hook",
        outroTemplate="call_to_action",
        creativePrompt="保留人物关系和产品卖点，结尾强化行动召唤与转化信息。",
    ),
    TaskPreset(
        key="episode_highlight",
        name="剧集高能版",
        description="适合长剧切条和连续集内容，突出剧情推进和高能转折。",
        defaultTitle="剧集高能版",
        platform="kuaishou",
        aspectRatio="9:16",
        minDurationSeconds=25,
        maxDurationSeconds=45,
        outputCount=3,
        introTemplate="cinematic",
        outroTemplate="brand",
        creativePrompt="优先保留冲突升级、角色关系变化和关键反转，适合系列追更。",
    ),
    TaskPreset(
        key="longform_snippet",
        name="长视频精华版",
        description="适合横版内容截取精华段落，兼顾信息传达和观看完整性。",
        defaultTitle="长视频精华版",
        platform="xiaohongshu",
        aspectRatio="16:9",
        minDurationSeconds=30,
        maxDurationSeconds=60,
        outputCount=2,
        introTemplate="none",
        outroTemplate="call_to_action",
        creativePrompt="保留信息完整度和关键观点，剪辑更克制，适合横版分发。",
    ),
)


def get_task_presets() -> list[TaskPreset]:
    return [preset.model_copy(deep=True) for preset in TASK_PRESETS]


def get_task_preset(key: str) -> TaskPreset | None:
    for preset in TASK_PRESETS:
        if preset.key == key:
            return preset.model_copy(deep=True)
    return None
