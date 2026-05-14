package com.jiandou.api.auth.web;

import com.jiandou.api.auth.application.AdminIdentityService;
import com.jiandou.api.auth.application.UserModelConfigService;
import com.jiandou.api.admin.dto.AdminModelConfigKeyUpdateRequest;
import com.jiandou.api.auth.security.CurrentUserPrincipal;
import com.jiandou.api.auth.web.dto.AdminInviteResponse;
import com.jiandou.api.auth.web.dto.AdminUserResponse;
import com.jiandou.api.auth.web.dto.CreateAdminUserRequest;
import com.jiandou.api.auth.web.dto.CreateInviteRequest;
import com.jiandou.api.auth.web.dto.UpdateAdminUserPasswordRequest;
import com.jiandou.api.auth.web.dto.UpdateAdminUserRequest;
import com.jiandou.api.config.ApiPathConstants;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端身份管理控制器。
 */
@RestController
@RequestMapping(ApiPathConstants.ADMIN)
public class AdminIdentityController {

    private final AdminIdentityService adminIdentityService;
    private final UserModelConfigService userModelConfigService;

    public AdminIdentityController(AdminIdentityService adminIdentityService, UserModelConfigService userModelConfigService) {
        this.adminIdentityService = adminIdentityService;
        this.userModelConfigService = userModelConfigService;
    }

    /**
     * 列出用户。
     * @return 处理结果
     */
    @GetMapping("/users")
    public List<AdminUserResponse> listUsers(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) String status
    ) {
        return adminIdentityService.listUsers(q, role, status);
    }

    /**
     * 获取用户详情。
     * @param id 用户ID
     * @return 处理结果
     */
    @GetMapping("/users/{id}")
    public AdminUserResponse getUser(@PathVariable Long id) {
        return adminIdentityService.getUser(id);
    }

    /**
     * 创建用户。
     * @param request 创建请求
     * @return 处理结果
     */
    @PostMapping("/users")
    public AdminUserResponse createUser(@Valid @RequestBody CreateAdminUserRequest request) {
        return adminIdentityService.createUser(request);
    }

    /**
     * 更新用户。
     * @param id 用户ID
     * @param request 更新请求
     * @return 处理结果
     */
    @PutMapping("/users/{id}")
    public AdminUserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UpdateAdminUserRequest request) {
        return adminIdentityService.updateUser(id, request);
    }

    /**
     * 更新用户密码。
     * @param id 用户ID
     * @param request 密码更新请求
     * @return 处理结果
     */
    @PutMapping("/users/{id}/password")
    public AdminUserResponse updateUserPassword(@PathVariable Long id, @Valid @RequestBody UpdateAdminUserPasswordRequest request) {
        return adminIdentityService.updateUserPassword(id, request);
    }

    /**
     * 禁用用户。
     * @param id 用户ID
     * @param actor 当前管理员
     * @return 处理结果
     */
    @PostMapping("/users/{id}/disable")
    public AdminUserResponse disableUser(@PathVariable Long id, @AuthenticationPrincipal CurrentUserPrincipal actor) {
        return adminIdentityService.disableUser(id, actor);
    }

    /**
     * 启用用户。
     * @param id 用户ID
     * @return 处理结果
     */
    @PostMapping("/users/{id}/enable")
    public AdminUserResponse enableUser(@PathVariable Long id) {
        return adminIdentityService.enableUser(id);
    }

    /**
     * 删除用户。
     * @param id 用户ID
     */
    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable Long id) {
        adminIdentityService.deleteUser(id);
    }

    /**
     * 保存用户模型厂商 Key。
     * @param id 用户ID
     * @param request 请求体
     */
    @PostMapping("/users/{id}/model-config/keys")
    public void saveUserModelConfigKeys(
        @PathVariable Long id,
        @RequestBody AdminModelConfigKeyUpdateRequest request
    ) {
        adminIdentityService.getUser(id);
        userModelConfigService.resetKeys(id, request);
    }

    /**
     * 列出邀请码。
     * @return 处理结果
     */
    @GetMapping("/invites")
    public List<AdminInviteResponse> listInvites() {
        return adminIdentityService.listInvites();
    }

    /**
     * 创建邀请码。
     * @param request 创建请求
     * @param actor 当前管理员
     * @return 处理结果
     */
    @PostMapping("/invites")
    public AdminInviteResponse createInvite(
        @Valid @RequestBody CreateInviteRequest request,
        @AuthenticationPrincipal CurrentUserPrincipal actor
    ) {
        return adminIdentityService.createInvite(request, actor);
    }

    /**
     * 撤销邀请码。
     * @param id 邀请码ID
     * @return 处理结果
     */
    @PostMapping("/invites/{id}/revoke")
    public AdminInviteResponse revokeInvite(@PathVariable Long id) {
        return adminIdentityService.revokeInvite(id);
    }
}
