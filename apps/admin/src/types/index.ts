export type UserRole = "ADMIN" | "USER";

export type UserStatus = "ACTIVE" | "DISABLED";

export type InviteStatus = "UNUSED" | "USED" | "REVOKED" | "EXPIRED";

export interface AuthenticatedUser {
  id: number;
  username: string;
  displayName: string;
  role: UserRole;
}

export interface AuthSession {
  authenticated: boolean;
  user: AuthenticatedUser | null;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AdminUser {
  id: number;
  username: string;
  displayName: string;
  role: UserRole;
  status: UserStatus;
  taskConcurrencyLimit: number;
  runningTaskCount: number;
  queuedTaskCount: number;
  lastLoginAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AdminUserQuery {
  q?: string;
  role?: UserRole | "";
  status?: UserStatus | "";
}

export interface CreateAdminUserRequest {
  username: string;
  displayName: string;
  password: string;
  role: UserRole;
  status: UserStatus;
  taskConcurrencyLimit: number;
}

export interface UpdateAdminUserRequest {
  displayName: string;
  role: UserRole;
  status: UserStatus;
  taskConcurrencyLimit: number;
}

export interface UpdateAdminUserPasswordRequest {
  password: string;
}

export interface AdminModelConfigProviderItem {
  key: string;
  provider: string;
  vendor: string;
  kinds: string[];
  baseUrl: string;
  taskBaseUrl: string;
  endpointHost: string;
  taskEndpointHost: string;
  apiKeyConfigured: boolean;
  baseUrlConfigured: boolean;
  taskBaseUrlConfigured: boolean;
  extras: Record<string, string>;
  modelNames: string[];
}

export interface AdminModelConfigResponse {
  configSource: string;
  providers: AdminModelConfigProviderItem[];
}

export interface AdminModelConfigProviderKeyInput {
  key: string;
  apiKey: string;
}

export interface AdminModelConfigKeyUpdateRequest {
  providers: AdminModelConfigProviderKeyInput[];
}

export interface AdminCreditUser {
  id: number;
  userId?: number;
  username: string;
  displayName: string;
  role?: UserRole | string | null;
  status?: UserStatus | string | null;
  balance: number;
  totalConsumed: number;
  totalAdjusted: number;
  imageGenerationCount: number;
  videoGenerationCount: number;
  lastUsedAt?: string | null;
}

export interface AdminCreditUserQuery {
  q?: string;
}

export interface AdminCreditAdjustmentRequest {
  amount: number;
  reason: string;
}

export type AdminCreditTransactionType = "ADJUST" | "CONSUME" | "USAGE" | "REFUND" | string;

export interface AdminCreditTransaction {
  transactionId: string;
  userId: number;
  featureCode: string;
  transactionType: AdminCreditTransactionType;
  amountDelta: number;
  balanceBefore: number;
  balanceAfter: number;
  relatedRunId?: string | null;
  relatedTaskId?: string | null;
  relatedWorkflowId?: string | null;
  reason?: string | null;
  createdAt: string;
}

export interface AdminCreditRule {
  id: number;
  featureCode: string;
  displayName: string;
  cost: number;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface AdminCreditRuleUpdateRequest {
  cost: number;
}

export interface AdminInviteActor {
  id: number;
  username: string;
  displayName: string;
}

export interface AdminInvite {
  id: number;
  code: string;
  role: UserRole;
  status: InviteStatus;
  expiresAt?: string | null;
  createdBy?: AdminInviteActor | null;
  usedBy?: AdminInviteActor | null;
  usedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAdminInviteRequest {
  role: UserRole;
}

export type TaskStatus =
  | "PENDING"
  | "PAUSED"
  | "ANALYZING"
  | "PLANNING"
  | "RENDERING"
  | "COMPLETED"
  | "FAILED";

export type AdminTaskSortMode =
  | "updated_desc"
  | "created_desc"
  | "progress_desc"
  | "status_desc"
  | "effect_rating_desc";

export interface AdminTaskListItem {
  id: string;
  title: string;
  status: TaskStatus;
  progress: number;
  createdAt: string;
  updatedAt: string;
  sourceFileName?: string | null;
  aspectRatio?: string | null;
  minDurationSeconds?: number | null;
  maxDurationSeconds?: number | null;
  retryCount?: number | null;
  startedAt?: string | null;
  finishedAt?: string | null;
  completedOutputCount?: number | null;
  taskSeed?: number | null;
  effectRating?: number | null;
  effectRatingNote?: string | null;
  ratedAt?: string | null;
  hasTranscript?: boolean;
  hasTimedTranscript?: boolean;
  sourceAssetCount?: number | null;
  editingMode?: string | null;
  isQueued?: boolean;
  queuePosition?: number | null;
  currentStage?: string | null;
  activeWorkerInstanceId?: string | null;
  plannedClipCount?: number | null;
  renderedClipCount?: number | null;
  diagnosisSeverity?: "info" | "low" | "medium" | "high";
  diagnosisCode?: string | null;
  diagnosisHint?: string | null;
  recommendedAction?: string | null;
  ownerUserId?: number | null;
  ownerUsername?: string | null;
  ownerDisplayName?: string | null;
  ownerRole?: UserRole | null | string;
}

export interface AdminTaskQuery {
  q?: string;
  status?: TaskStatus | "";
  sort?: AdminTaskSortMode;
}

export interface AdminTaskBatchFailure {
  taskId: string;
  reason: string;
}

export interface AdminTaskBatchResult {
  action: "terminate";
  requestedCount: number;
  succeededTaskIds: string[];
  failed: AdminTaskBatchFailure[];
}

export interface AdminOverviewCounts {
  totalTasks: number;
  queuedTasks: number;
  runningTasks: number;
  completedTasks: number;
  failedTasks: number;
  highRiskTasks: number;
  riskyTasks: number;
  semanticTasks: number;
  timedSemanticTasks: number;
  averageProgress: number;
}

export interface AdminOverviewQueue {
  generatedAt: string;
  queueLength: number;
  queueSnapshot: string[];
  runningWorkers: number;
  userQueues: AdminUserQueueOverview[];
  latestEvents: Array<Record<string, unknown>>;
  oldestQueuedTaskId: string;
  oldestQueuedTaskTitle: string;
  oldestQueuedTaskCreatedAt?: string | null;
}

export interface AdminUserQueueOverview {
  ownerUserId?: number | null;
  ownerUsername: string;
  ownerDisplayName: string;
  ownerRole: UserRole | "SYSTEM" | string;
  taskConcurrencyLimit: number;
  runningTaskCount: number;
  queuedTaskCount: number;
  oldestQueuedTaskId: string;
  oldestQueuedTaskTitle: string;
  oldestQueuedTaskCreatedAt?: string | null;
}

export interface AdminOverviewWorkers {
  items: Array<Record<string, unknown>>;
  onlineCount: number;
}

export interface AdminOverviewResponse {
  generatedAt: string;
  counts: AdminOverviewCounts;
  queue: AdminOverviewQueue;
  workers: AdminOverviewWorkers;
  recentTasks: AdminTaskListItem[];
  recentFailures: AdminTaskListItem[];
  recentRunningTasks: AdminTaskListItem[];
  recentTraceCount: number;
  modelReady: boolean;
  primaryModel?: string | null;
  textModel?: string | null;
}
