export type UserRole = "ADMIN" | "USER";

export type UserStatus = "ACTIVE" | "DISABLED";

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
}

export interface UpdateAdminUserRequest {
  displayName: string;
  role: UserRole;
  status: UserStatus;
}

export interface UpdateAdminUserPasswordRequest {
  password: string;
}
