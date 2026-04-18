import { getJson } from "./client";
import type { AdminTaskListItem, AdminTaskQuery } from "@/types";

export async function fetchAdminTasks(query?: AdminTaskQuery) {
  const params = new URLSearchParams();
  if (query?.q?.trim()) {
    params.set("q", query.q.trim());
  }
  if (query?.status) {
    params.set("status", query.status);
  }
  if (query?.sort) {
    params.set("sort", query.sort);
  }
  const search = params.toString();
  return getJson<AdminTaskListItem[]>(search ? `/admin/tasks?${search}` : "/admin/tasks");
}
