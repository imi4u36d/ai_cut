package com.jiandou.api.auth.web;

import com.jiandou.api.admin.dto.AdminModelConfigKeyUpdateRequest;
import com.jiandou.api.admin.dto.AdminModelConfigResponse;
import com.jiandou.api.admin.dto.AdminModelConfigValidationResponse;
import com.jiandou.api.auth.application.UserModelConfigService;
import com.jiandou.api.auth.security.CurrentUserPrincipal;
import com.jiandou.api.config.ApiPathConstants;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前登录用户的模型 key 配置接口。
 */
@RestController
@RequestMapping(ApiPathConstants.AUTH)
public class UserModelConfigController {

    private final UserModelConfigService userModelConfigService;

    public UserModelConfigController(UserModelConfigService userModelConfigService) {
        this.userModelConfigService = userModelConfigService;
    }

    /**
     * 返回当前用户模型配置快照。
     * @param principal 当前登录用户
     * @return 处理结果
     */
    @GetMapping("/model-config")
    public AdminModelConfigResponse modelConfig(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return userModelConfigService.read(principal == null ? null : principal.userId());
    }

    /**
     * 校验当前用户 key 草稿。
     * @param principal 当前登录用户
     * @param request 请求体
     * @return 处理结果
     */
    @PostMapping("/model-config/validate")
    public AdminModelConfigValidationResponse validateModelConfig(
        @AuthenticationPrincipal CurrentUserPrincipal principal,
        @RequestBody AdminModelConfigKeyUpdateRequest request
    ) {
        return userModelConfigService.validateKeys(principal == null ? null : principal.userId(), request);
    }

    /**
     * 保存当前用户 key 配置。
     * @param principal 当前登录用户
     * @param request 请求体
     * @return 处理结果
     */
    @PostMapping("/model-config/keys")
    public AdminModelConfigResponse saveModelConfigKeys(
        @AuthenticationPrincipal CurrentUserPrincipal principal,
        @RequestBody AdminModelConfigKeyUpdateRequest request
    ) {
        return userModelConfigService.saveKeys(principal == null ? null : principal.userId(), request);
    }
}
