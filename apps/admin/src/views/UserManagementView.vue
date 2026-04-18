<template>
  <section class="user-page">
    <div class="user-page__summary">
      <el-card v-for="item in summaryCards" :key="item.label" class="surface-card user-page__summary-card" shadow="never">
        <p>{{ item.label }}</p>
        <strong>{{ item.value }}</strong>
        <span>{{ item.note }}</span>
      </el-card>
    </div>

    <el-card class="surface-card" shadow="never">
      <template #header>
        <div class="user-page__toolbar">
          <div>
            <p class="user-page__eyebrow">User Directory</p>
            <h3>账号列表</h3>
          </div>
          <div class="user-page__toolbar-actions">
            <el-button plain @click="resetFilters">重置</el-button>
            <el-button :icon="Refresh" plain @click="loadUsers">刷新</el-button>
            <el-button :icon="Plus" type="primary" @click="openCreateDialog">新建用户</el-button>
          </div>
        </div>
      </template>

      <el-form class="user-page__filters" inline @submit.prevent="loadUsers">
        <el-form-item label="关键词">
          <el-input v-model.trim="filters.q" clearable placeholder="用户名 / 显示名" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="filters.role" clearable placeholder="全部角色" style="width: 160px">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="普通用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 160px">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item class="user-page__filters-action">
          <el-button :loading="loading" native-type="submit" type="primary">查询</el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="errorMessage" :closable="false" class="user-page__alert" show-icon type="error" :title="errorMessage" />

      <el-table v-loading="loading" :data="users" class="user-page__table">
        <el-table-column label="用户名" min-width="140" prop="username" />
        <el-table-column label="显示名" min-width="140" prop="displayName" />
        <el-table-column label="角色" min-width="110">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'warning' : 'info'" effect="plain">
              {{ row.role === "ADMIN" ? "管理员" : "普通用户" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" effect="light">
              {{ row.status === "ACTIVE" ? "启用" : "禁用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.lastLoginAt) }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column align="right" fixed="right" label="操作" min-width="280">
          <template #default="{ row }">
            <div class="user-page__actions">
              <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
              <el-button link type="warning" @click="openPasswordDialog(row)">改密码</el-button>
              <el-button
                v-if="row.status === 'ACTIVE'"
                link
                type="warning"
                @click="toggleUserStatus(row, 'disable')"
              >
                禁用
              </el-button>
              <el-button
                v-else
                link
                type="success"
                @click="toggleUserStatus(row, 'enable')"
              >
                启用
              </el-button>
              <el-button link type="danger" @click="removeUser(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="editorVisible" :title="editorMode === 'create' ? '新建用户' : '编辑用户'" width="520px">
      <el-form label-position="top">
        <el-form-item label="用户名">
          <el-input v-model.trim="editorForm.username" :disabled="editorMode === 'edit'" placeholder="3-32 位登录名" />
        </el-form-item>
        <el-form-item label="显示名">
          <el-input v-model.trim="editorForm.displayName" placeholder="用于后台展示" />
        </el-form-item>
        <el-form-item v-if="editorMode === 'create'" label="初始密码">
          <el-input v-model="editorForm.password" placeholder="8-72 位密码" show-password type="password" />
        </el-form-item>
        <div class="user-page__dialog-grid">
          <el-form-item label="角色">
            <el-select v-model="editorForm.role">
              <el-option label="管理员" value="ADMIN" />
              <el-option label="普通用户" value="USER" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="editorForm.status">
              <el-option label="启用" value="ACTIVE" />
              <el-option label="禁用" value="DISABLED" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button :loading="submittingEditor" type="primary" @click="submitEditor">
          {{ editorMode === "create" ? "创建用户" : "保存修改" }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" title="重置用户密码" width="420px">
      <el-form label-position="top">
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.password" placeholder="请输入新密码" show-password type="password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button :loading="submittingPassword" type="primary" @click="submitPassword">
          更新密码
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, Refresh } from "@element-plus/icons-vue";
import {
  createAdminUser,
  deleteAdminUser,
  disableAdminUser,
  enableAdminUser,
  fetchAdminUsers,
  updateAdminUser,
  updateAdminUserPassword
} from "@/api/users";
import type { AdminUser, CreateAdminUserRequest, UpdateAdminUserRequest, UserRole, UserStatus } from "@/types";

const loading = ref(false);
const submittingEditor = ref(false);
const submittingPassword = ref(false);
const errorMessage = ref("");
const users = ref<AdminUser[]>([]);
const filters = reactive({
  q: "",
  role: "" as UserRole | "",
  status: "" as UserStatus | ""
});

const editorVisible = ref(false);
const editorMode = ref<"create" | "edit">("create");
const editingUserId = ref<number | null>(null);
const editorForm = reactive({
  username: "",
  displayName: "",
  password: "",
  role: "USER" as UserRole,
  status: "ACTIVE" as UserStatus
});

const passwordDialogVisible = ref(false);
const passwordUserId = ref<number | null>(null);
const passwordForm = reactive({
  password: ""
});

const summaryCards = computed(() => {
  const total = users.value.length;
  const active = users.value.filter((user) => user.status === "ACTIVE").length;
  const disabled = users.value.filter((user) => user.status === "DISABLED").length;
  const admins = users.value.filter((user) => user.role === "ADMIN").length;
  return [
    { label: "全部账号", value: total, note: "可管理登录账户" },
    { label: "启用中", value: active, note: "当前可正常登录" },
    { label: "管理员", value: admins, note: "后台可访问账号" },
    { label: "已禁用", value: disabled, note: "等待恢复或删除" }
  ];
});

function formatDateTime(value?: string | null) {
  if (!value) {
    return "未记录";
  }
  return new Date(value).toLocaleString();
}

function resetEditorForm() {
  editorForm.username = "";
  editorForm.displayName = "";
  editorForm.password = "";
  editorForm.role = "USER";
  editorForm.status = "ACTIVE";
  editingUserId.value = null;
}

async function loadUsers() {
  loading.value = true;
  errorMessage.value = "";
  try {
    users.value = await fetchAdminUsers(filters);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "读取用户列表失败";
  } finally {
    loading.value = false;
  }
}

function resetFilters() {
  filters.q = "";
  filters.role = "";
  filters.status = "";
  void loadUsers();
}

function openCreateDialog() {
  editorMode.value = "create";
  resetEditorForm();
  editorVisible.value = true;
}

function openEditDialog(user: AdminUser) {
  editorMode.value = "edit";
  editorVisible.value = true;
  editingUserId.value = user.id;
  editorForm.username = user.username;
  editorForm.displayName = user.displayName;
  editorForm.password = "";
  editorForm.role = user.role;
  editorForm.status = user.status;
}

function openPasswordDialog(user: AdminUser) {
  passwordUserId.value = user.id;
  passwordForm.password = "";
  passwordDialogVisible.value = true;
}

async function submitEditor() {
  submittingEditor.value = true;
  try {
    if (editorMode.value === "create") {
      const payload: CreateAdminUserRequest = {
        username: editorForm.username,
        displayName: editorForm.displayName,
        password: editorForm.password,
        role: editorForm.role,
        status: editorForm.status
      };
      await createAdminUser(payload);
      ElMessage.success("用户创建成功");
    } else if (editingUserId.value != null) {
      const payload: UpdateAdminUserRequest = {
        displayName: editorForm.displayName,
        role: editorForm.role,
        status: editorForm.status
      };
      await updateAdminUser(editingUserId.value, payload);
      ElMessage.success("用户信息已更新");
    }
    editorVisible.value = false;
    resetEditorForm();
    await loadUsers();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "保存用户失败");
  } finally {
    submittingEditor.value = false;
  }
}

async function submitPassword() {
  if (passwordUserId.value == null) {
    return;
  }
  submittingPassword.value = true;
  try {
    await updateAdminUserPassword(passwordUserId.value, {
      password: passwordForm.password
    });
    passwordDialogVisible.value = false;
    passwordForm.password = "";
    ElMessage.success("用户密码已更新");
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "更新密码失败");
  } finally {
    submittingPassword.value = false;
  }
}

async function toggleUserStatus(user: AdminUser, action: "enable" | "disable") {
  try {
    await ElMessageBox.confirm(
      action === "enable" ? `确认启用账号 ${user.username} 吗？` : `确认禁用账号 ${user.username} 吗？`,
      "状态变更",
      {
        type: "warning"
      }
    );
    if (action === "enable") {
      await enableAdminUser(user.id);
      ElMessage.success("用户已启用");
    } else {
      await disableAdminUser(user.id);
      ElMessage.success("用户已禁用");
    }
    await loadUsers();
  } catch (error) {
    if (error === "cancel") {
      return;
    }
    ElMessage.error(error instanceof Error ? error.message : "状态更新失败");
  }
}

async function removeUser(user: AdminUser) {
  try {
    await ElMessageBox.confirm(`删除后不可恢复，确认删除账号 ${user.username} 吗？`, "删除用户", {
      type: "warning",
      confirmButtonText: "确认删除",
      cancelButtonText: "取消"
    });
    await deleteAdminUser(user.id);
    ElMessage.success("用户已删除");
    await loadUsers();
  } catch (error) {
    if (error === "cancel") {
      return;
    }
    ElMessage.error(error instanceof Error ? error.message : "删除用户失败");
  }
}

onMounted(async () => {
  await loadUsers();
});
</script>

<style scoped>
.user-page {
  display: grid;
  gap: 20px;
}

.user-page__summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.user-page__summary-card {
  border-radius: 22px;
}

.user-page__summary-card :deep(.el-card__body) {
  display: grid;
  gap: 6px;
}

.user-page__summary-card p {
  margin: 0;
  color: var(--jd-text-soft);
}

.user-page__summary-card strong {
  font-family: "Space Grotesk", sans-serif;
  font-size: 2rem;
}

.user-page__summary-card span {
  color: var(--jd-text-soft);
  font-size: 0.92rem;
}

.user-page__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.user-page__eyebrow {
  margin: 0 0 4px;
  color: var(--jd-text-soft);
  font-size: 0.76rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.user-page__toolbar h3 {
  margin: 0;
  font-family: "Space Grotesk", sans-serif;
}

.user-page__toolbar-actions,
.user-page__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.user-page__filters {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 6px 10px;
  margin-bottom: 10px;
}

.user-page__filters-action {
  margin-left: auto;
}

.user-page__alert {
  margin-bottom: 16px;
}

.user-page__table {
  width: 100%;
}

.user-page__dialog-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

@media (max-width: 1200px) {
  .user-page__summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .user-page__summary,
  .user-page__dialog-grid {
    grid-template-columns: 1fr;
  }

  .user-page__toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .user-page__toolbar-actions {
    width: 100%;
  }

  .user-page__toolbar-actions :deep(.el-button) {
    flex: 1;
    min-width: 0;
  }
}
</style>
