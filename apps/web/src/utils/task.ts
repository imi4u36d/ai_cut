/**
 * 任务相关工具方法。
 */
import type { TaskStatus } from "@/types";

export const TERMINAL_TASK_STATUSES: TaskStatus[] = ["COMPLETED", "FAILED"];

export const TASK_STATUS_LABELS: Record<TaskStatus, string> = {
  PENDING: "排队等待",
  PAUSED: "已暂停",
  ANALYZING: "分析素材",
  PLANNING: "生成方案",
  RENDERING: "渲染导出",
  COMPLETED: "已完成",
  FAILED: "失败"
};

export const TASK_LIFECYCLE_GROUP_LABELS = {
  queued: "排队中",
  paused: "已暂停",
  running: "处理中",
  completed: "已完成",
  failed: "失败"
} as const;

/**
 * 检查是否终态任务状态。
 * @param status 状态值
 */
export function isTerminalTaskStatus(status: TaskStatus) {
  return TERMINAL_TASK_STATUSES.includes(status);
}

/**
 * 格式化任务状态。
 * @param status 状态值
 */
export function formatTaskStatus(status: TaskStatus) {
  return TASK_STATUS_LABELS[status] ?? status;
}

/**
 * 返回任务生命周期分组。
 * @param status 状态值
 */
export function getTaskLifecycleGroup(status: TaskStatus) {
  switch (status) {
    case "COMPLETED":
      return "completed";
    case "FAILED":
      return "failed";
    case "PAUSED":
      return "paused";
    case "ANALYZING":
    case "PLANNING":
    case "RENDERING":
      return "running";
    default:
      return "queued";
  }
}

/**
 * 格式化任务范围。
 * @param minDuration 最小时长值
 * @param maxDuration 最大时长值
 */
export function formatTaskRange(minDuration: number, maxDuration: number) {
  return `${minDuration}-${maxDuration}s`;
}

/**
 * 格式化任务进度。
 * @param progress 进度值
 */
export function formatTaskProgress(progress: number) {
  return `${Math.max(0, Math.min(100, progress))}%`;
}
