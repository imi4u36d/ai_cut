package com.jiandou.api.auth.application;

import com.jiandou.api.auth.domain.UserRole;
import com.jiandou.api.auth.domain.UserStatus;
import com.jiandou.api.auth.infrastructure.mybatis.MybatisAuthRepository;
import com.jiandou.api.auth.infrastructure.mybatis.SysInviteCodeEntity;
import com.jiandou.api.auth.infrastructure.mybatis.SysUserEntity;
import com.jiandou.api.auth.security.CurrentUserPrincipal;
import com.jiandou.api.auth.web.dto.AdminInviteActorResponse;
import com.jiandou.api.auth.web.dto.AdminInviteResponse;
import com.jiandou.api.auth.web.dto.AdminUserResponse;
import com.jiandou.api.auth.web.dto.CreateAdminUserRequest;
import com.jiandou.api.auth.web.dto.CreateInviteRequest;
import com.jiandou.api.auth.web.dto.UpdateAdminUserPasswordRequest;
import com.jiandou.api.auth.web.dto.UpdateAdminUserRequest;
import com.jiandou.api.common.exception.ApiException;
import com.jiandou.api.config.JiandouAuthProperties;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 管理端用户与邀请码服务。
 */
@Service
public class AdminIdentityService {

    private final MybatisAuthRepository authRepository;
    private final JiandouAuthProperties authProperties;
    private final PasswordEncoder passwordEncoder;
    private final UserCreditAccountInitializer userCreditAccountInitializer;
    private final UserQueueStatsProvider userQueueStatsProvider;

    public AdminIdentityService(
        MybatisAuthRepository authRepository,
        JiandouAuthProperties authProperties,
        PasswordEncoder passwordEncoder
    ) {
        this(authRepository, authProperties, passwordEncoder, null, null);
    }

    public AdminIdentityService(
        MybatisAuthRepository authRepository,
        JiandouAuthProperties authProperties,
        PasswordEncoder passwordEncoder,
        UserCreditAccountInitializer userCreditAccountInitializer
    ) {
        this(authRepository, authProperties, passwordEncoder, userCreditAccountInitializer, null);
    }

    @Autowired
    public AdminIdentityService(
        MybatisAuthRepository authRepository,
        JiandouAuthProperties authProperties,
        PasswordEncoder passwordEncoder,
        UserCreditAccountInitializer userCreditAccountInitializer,
        UserQueueStatsProvider userQueueStatsProvider
    ) {
        this.authRepository = authRepository;
        this.authProperties = authProperties;
        this.passwordEncoder = passwordEncoder;
        this.userCreditAccountInitializer = userCreditAccountInitializer;
        this.userQueueStatsProvider = userQueueStatsProvider;
    }

    /**
     * 列出用户。
     * @return 处理结果
     */
    public List<AdminUserResponse> listUsers() {
        Map<Long, UserQueueStats> queueStats = loadUserQueueStats();
        return authRepository.listUsers().stream().map(user -> toUserResponse(user, queueStats)).toList();
    }

    /**
     * 按条件列出用户。
     * @param keyword 关键词
     * @param role 角色
     * @param status 状态
     * @return 处理结果
     */
    public List<AdminUserResponse> listUsers(String keyword, String role, String status) {
        String normalizedRole = normalizeRoleFilter(role);
        String normalizedStatus = normalizeStatusFilter(status);
        Map<Long, UserQueueStats> queueStats = loadUserQueueStats();
        return authRepository.listUsers(keyword, normalizedRole, normalizedStatus).stream()
            .map(user -> toUserResponse(user, queueStats))
            .toList();
    }

    /**
     * 获取用户详情。
     * @param id 用户ID
     * @return 处理结果
     */
    public AdminUserResponse getUser(Long id) {
        return toUserResponse(requireUser(id), loadUserQueueStats());
    }

    /**
     * 创建用户。
     * @param request 创建请求
     * @return 处理结果
     */
    public AdminUserResponse createUser(CreateAdminUserRequest request) {
        String username = AuthApplicationService.validateUsername(request.username());
        if (authRepository.findUserByUsername(username) != null) {
            throw new ApiException(HttpStatus.CONFLICT, "username_taken", "用户名已存在");
        }
        String displayName = AuthApplicationService.validateDisplayName(request.displayName());
        String password = AuthApplicationService.validatePassword(request.password());
        UserRole role = parseRole(request.role());
        UserStatus status = parseStatus(request.status());
        int taskConcurrencyLimit = normalizeTaskConcurrencyLimit(request.taskConcurrencyLimit());
        SysUserEntity user = authRepository.createUser(
            username,
            displayName,
            passwordEncoder.encode(password),
            role,
            status,
            taskConcurrencyLimit
        );
        ensureCreditAccount(user);
        return toUserResponse(user, loadUserQueueStats());
    }

    /**
     * 更新用户。
     * @param id 用户ID
     * @param request 更新请求
     * @return 处理结果
     */
    public AdminUserResponse updateUser(Long id, UpdateAdminUserRequest request) {
        SysUserEntity existing = requireUser(id);
        String displayName = AuthApplicationService.validateDisplayName(request.displayName());
        UserRole role = parseRole(request.role());
        UserStatus status = parseStatus(request.status());
        int taskConcurrencyLimit = normalizeTaskConcurrencyLimit(request.taskConcurrencyLimit());
        ensureAdminUserGuard(existing, role, status);
        return toUserResponse(authRepository.updateUser(id, displayName, role, status, taskConcurrencyLimit), loadUserQueueStats());
    }

    /**
     * 更新用户密码。
     * @param id 用户ID
     * @param request 更新请求
     * @return 处理结果
     */
    public AdminUserResponse updateUserPassword(Long id, UpdateAdminUserPasswordRequest request) {
        requireUser(id);
        String password = AuthApplicationService.validatePassword(request.password());
        return toUserResponse(authRepository.updateUserPassword(id, passwordEncoder.encode(password)), loadUserQueueStats());
    }

    /**
     * 禁用用户。
     * @param id 用户ID
     * @param actor 当前操作者
     * @return 处理结果
     */
    public AdminUserResponse disableUser(Long id, CurrentUserPrincipal actor) {
        SysUserEntity user = requireUser(id);
        if (UserStatus.DISABLED.value().equals(user.getStatus())) {
            return toUserResponse(user);
        }
        ensureAdminUserGuard(user, parseRole(user.getRole()), UserStatus.DISABLED);
        return toUserResponse(authRepository.updateUserStatus(id, UserStatus.DISABLED), loadUserQueueStats());
    }

    /**
     * 启用用户。
     * @param id 用户ID
     * @return 处理结果
     */
    public AdminUserResponse enableUser(Long id) {
        requireUser(id);
        return toUserResponse(authRepository.updateUserStatus(id, UserStatus.ACTIVE), loadUserQueueStats());
    }

    /**
     * 删除用户。
     * @param id 用户ID
     * @return 处理结果
     */
    public void deleteUser(Long id) {
        SysUserEntity user = requireUser(id);
        ensureAdminUserGuard(user, UserRole.USER, UserStatus.DISABLED);
        authRepository.deleteUser(id);
    }

    /**
     * 列出邀请码。
     * @return 处理结果
     */
    public List<AdminInviteResponse> listInvites() {
        List<SysInviteCodeEntity> invites = authRepository.listInvites();
        Set<Long> relatedUserIds = new LinkedHashSet<>();
        for (SysInviteCodeEntity invite : invites) {
            if (invite.getCreatedBy() != null) {
                relatedUserIds.add(invite.getCreatedBy());
            }
            if (invite.getUsedBy() != null) {
                relatedUserIds.add(invite.getUsedBy());
            }
        }
        Map<Long, SysUserEntity> userMap = authRepository.findUsersByIds(relatedUserIds);
        return invites.stream().map(invite -> toInviteResponse(invite, userMap)).toList();
    }

    /**
     * 创建邀请码。
     * @param request 创建请求
     * @param actor 当前操作者
     * @return 处理结果
     */
    public AdminInviteResponse createInvite(CreateInviteRequest request, CurrentUserPrincipal actor) {
        UserRole role = UserRole.from(request.role());
        OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(authProperties.getInviteExpiryHours());
        SysInviteCodeEntity invite = authRepository.createInvite(role, expiresAt, actor.userId());
        Map<Long, SysUserEntity> userMap = authRepository.findUsersByIds(Set.of(actor.userId()));
        return toInviteResponse(invite, userMap);
    }

    /**
     * 撤销邀请码。
     * @param id 邀请码ID
     * @return 处理结果
     */
    public AdminInviteResponse revokeInvite(Long id) {
        SysInviteCodeEntity invite = authRepository.revokeInvite(id);
        Set<Long> relatedUserIds = new LinkedHashSet<>();
        if (invite.getCreatedBy() != null) {
            relatedUserIds.add(invite.getCreatedBy());
        }
        if (invite.getUsedBy() != null) {
            relatedUserIds.add(invite.getUsedBy());
        }
        return toInviteResponse(invite, authRepository.findUsersByIds(relatedUserIds));
    }

    private SysUserEntity requireUser(Long id) {
        SysUserEntity user = authRepository.findUserById(id);
        if (user == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "user_not_found", "用户不存在");
        }
        return user;
    }

    private void ensureCreditAccount(SysUserEntity user) {
        if (userCreditAccountInitializer == null || user == null || isAdminUsername(user.getUsername())) {
            return;
        }
        userCreditAccountInitializer.ensureAccount(user.getId());
    }

    private boolean isAdminUsername(String username) {
        return "admin".equals(MybatisAuthRepository.normalizeUsername(username));
    }

    private UserRole parseRole(String raw) {
        try {
            return UserRole.from(raw);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_user_role", "用户角色不合法", exception);
        }
    }

    private UserStatus parseStatus(String raw) {
        try {
            return UserStatus.from(raw);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_user_status", "用户状态不合法", exception);
        }
    }

    private String normalizeRoleFilter(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }
        return parseRole(role).value();
    }

    private String normalizeStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }
        return parseStatus(status).value();
    }

    private void ensureAdminUserGuard(SysUserEntity existing, UserRole nextRole, UserStatus nextStatus) {
        boolean currentAdmin = UserRole.ADMIN.value().equals(existing.getRole());
        boolean currentActiveAdmin = currentAdmin && UserStatus.ACTIVE.value().equals(existing.getStatus());
        boolean nextAdmin = nextRole == UserRole.ADMIN;
        boolean nextActiveAdmin = nextAdmin && nextStatus == UserStatus.ACTIVE;

        if (currentAdmin && !nextAdmin && authRepository.countAdmins() <= 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "last_admin_guard", "至少保留一个管理员账号");
        }
        if (currentActiveAdmin && !nextActiveAdmin && authRepository.countActiveAdmins() <= 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "last_active_admin_guard", "至少保留一个可登录的管理员账号");
        }
    }

    private int normalizeTaskConcurrencyLimit(Integer raw) {
        int value = raw == null ? 1 : raw;
        if (value < 1 || value > 20) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_task_concurrency_limit", "任务并发额度必须在 1 到 20 之间");
        }
        return value;
    }

    private int taskConcurrencyLimit(SysUserEntity user) {
        Integer value = user == null ? null : user.getTaskConcurrencyLimit();
        return value == null || value < 1 ? 1 : Math.min(20, value);
    }

    private Map<Long, UserQueueStats> loadUserQueueStats() {
        if (userQueueStatsProvider == null) {
            return Map.of();
        }
        return userQueueStatsProvider.listUserQueueStats();
    }

    private AdminUserResponse toUserResponse(SysUserEntity user) {
        return toUserResponse(user, Map.of());
    }

    private AdminUserResponse toUserResponse(SysUserEntity user, Map<Long, UserQueueStats> queueStats) {
        UserQueueStats stats = queueStats.getOrDefault(user.getId(), UserQueueStats.EMPTY);
        return new AdminUserResponse(
            user.getId(),
            user.getUsername(),
            user.getDisplayName(),
            user.getRole(),
            user.getStatus(),
            taskConcurrencyLimit(user),
            stats.runningTaskCount(),
            stats.queuedTaskCount(),
            user.getLastLoginAt(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    private AdminInviteResponse toInviteResponse(SysInviteCodeEntity invite, Map<Long, SysUserEntity> userMap) {
        return new AdminInviteResponse(
            invite.getId(),
            invite.getCode(),
            invite.getRole(),
            invite.getStatus(),
            invite.getExpiresAt(),
            toInviteActor(userMap.get(invite.getCreatedBy())),
            toInviteActor(userMap.get(invite.getUsedBy())),
            invite.getUsedAt(),
            invite.getCreatedAt(),
            invite.getUpdatedAt()
        );
    }

    private AdminInviteActorResponse toInviteActor(SysUserEntity user) {
        if (user == null) {
            return null;
        }
        return new AdminInviteActorResponse(user.getId(), user.getUsername(), user.getDisplayName());
    }
}
