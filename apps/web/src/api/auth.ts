/**
 * 认证相关 API 请求封装。
 */
import { getJson, postJson } from "./client";
import type {
  ActivateInviteRequest,
  AdminModelConfigKeyUpdateRequest,
  AdminModelConfigResponse,
  AdminModelConfigValidationResponse,
  AuthSession,
  LoginRequest,
} from "@/types";

export async function fetchAuthSession() {
  return getJson<AuthSession>("/auth/session");
}

export async function loginByPassword(payload: LoginRequest) {
  return postJson<AuthSession>("/auth/login", payload, { skipUnauthorizedHandler: true });
}

export async function logoutSession() {
  return postJson<{ success: boolean }>("/auth/logout", {}, { skipUnauthorizedHandler: true });
}

export async function activateInviteAccount(payload: ActivateInviteRequest) {
  return postJson<AuthSession>("/auth/activate-invite", payload, { skipUnauthorizedHandler: true });
}

export async function fetchUserModelConfig() {
  return getJson<AdminModelConfigResponse>("/auth/model-config");
}

export async function validateUserModelConfig(payload: AdminModelConfigKeyUpdateRequest) {
  return postJson<AdminModelConfigValidationResponse>("/auth/model-config/validate", payload);
}

export async function saveUserModelConfigKeys(payload: AdminModelConfigKeyUpdateRequest) {
  return postJson<AdminModelConfigResponse>("/auth/model-config/keys", payload);
}
