package com.jiandou.api.auth.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jiandou.api.auth.application.AdminIdentityService;
import com.jiandou.api.auth.application.AuthApplicationService;
import com.jiandou.api.auth.application.UserModelConfigService;
import com.jiandou.api.auth.config.SecurityConfig;
import com.jiandou.api.auth.domain.UserRole;
import com.jiandou.api.auth.domain.UserStatus;
import com.jiandou.api.auth.infrastructure.mybatis.MybatisAuthRepository;
import com.jiandou.api.auth.infrastructure.mybatis.SysUserEntity;
import com.jiandou.api.auth.security.CurrentUserPrincipal;
import com.jiandou.api.auth.security.SessionUserValidationFilter;
import com.jiandou.api.auth.web.dto.AuthSessionResponse;
import com.jiandou.api.common.web.ApiErrorResponseWriter;
import com.jiandou.api.config.JiandouAppProperties;
import com.jiandou.api.config.JiandouStorageProperties;
import com.jiandou.api.config.JiandouTaskOpsProperties;
import com.jiandou.api.generation.application.GenerationApplicationService;
import com.jiandou.api.generation.web.GenerationController;
import com.jiandou.api.task.application.TaskApplicationService;
import com.jiandou.api.task.web.TaskController;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AuthController.class, AdminIdentityController.class, TaskController.class, GenerationController.class})
@Import({SecurityConfig.class, SessionUserValidationFilter.class, ApiErrorResponseWriter.class, SecurityAccessTest.TestConfig.class})
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthApplicationService authApplicationService;

    @MockBean
    private AdminIdentityService adminIdentityService;

    @MockBean
    private UserModelConfigService userModelConfigService;

    @MockBean
    private TaskApplicationService taskApplicationService;

    @MockBean
    private GenerationApplicationService generationApplicationService;

    @MockBean
    private MybatisAuthRepository authRepository;

    @Test
    void authSessionEndpointIsPublic() throws Exception {
        when(authApplicationService.session(any())).thenReturn(new AuthSessionResponse(false, null));

        mockMvc.perform(get("/api/v3/auth/session"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void showcaseEndpointIsPublic() throws Exception {
        when(taskApplicationService.showcaseCases()).thenReturn(Map.of("items", List.of()));

        mockMvc.perform(get("/api/v3/tasks/showcase"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void generationCatalogEndpointIsPublic() throws Exception {
        when(generationApplicationService.catalog()).thenReturn(Map.of("videoModels", List.of()));

        mockMvc.perform(get("/api/v3/generation/catalog"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.videoModels").isArray());
    }

    @Test
    void authSessionEndpointIssuesCsrfCookieAndHeader() throws Exception {
        when(authApplicationService.session(any())).thenReturn(new AuthSessionResponse(false, null));

        mockMvc.perform(get("/api/v3/auth/session"))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-XSRF-TOKEN"));
    }

    @Test
    void loginEndpointRejectsMissingCsrfToken() throws Exception {
        mockMvc.perform(post("/api/v3/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "tester",
                      "password": "secret123"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("csrf_invalid"));
    }

    @Test
    void loginEndpointAcceptsValidCsrfToken() throws Exception {
        when(authApplicationService.login(any(), any(), any())).thenReturn(new AuthSessionResponse(true, null));

        mockMvc.perform(post("/api/v3/auth/login")
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "tester",
                      "password": "secret123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    void anonymousUserGetsUnauthorizedForAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v3/admin/users"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void normalUserGetsForbiddenForAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v3/admin/users").with(user("tester").roles("USER")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("admin_forbidden"));
    }

    @Test
    void anonymousUserWithValidCsrfGetsUnauthorizedForGenerationEndpoints() throws Exception {
        mockMvc.perform(post("/api/v3/generation/runs")
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("unauthorized"));

        mockMvc.perform(post("/api/v3/tasks/generate-prompt")
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("unauthorized"));

        mockMvc.perform(post("/api/v3/tasks/generation")
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void authenticatedUserWithValidCsrfCanCallGenerationEndpoints() throws Exception {
        Map<String, Object> row = Map.of("id", "created");
        when(authRepository.findUserById(1L)).thenReturn(activeUser());
        when(generationApplicationService.createAsyncRun(any())).thenReturn(row);
        when(taskApplicationService.generateCreativePrompt(any())).thenReturn(row);
        when(taskApplicationService.createGenerationTask(any())).thenReturn(row);
        Authentication authentication = userAuthentication();

        mockMvc.perform(post("/api/v3/generation/runs")
                .with(authentication(authentication))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("created"));

        mockMvc.perform(post("/api/v3/tasks/generate-prompt")
                .with(authentication(authentication))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("created"));

        mockMvc.perform(post("/api/v3/tasks/generation")
                .with(authentication(authentication))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("created"));
    }

    @Test
    void adminPreflightRequestAllowsConfiguredAdminOrigin() throws Exception {
        mockMvc.perform(options("/api/v3/admin/users")
                .header("Origin", "http://localhost:5174")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,x-xsrf-token"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5174"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    private Authentication userAuthentication() {
        CurrentUserPrincipal principal = CurrentUserPrincipal.from(activeUser());
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    private SysUserEntity activeUser() {
        SysUserEntity user = new SysUserEntity();
        user.setId(1L);
        user.setUsername("tester");
        user.setDisplayName("Tester");
        user.setRole(UserRole.USER.value());
        user.setStatus(UserStatus.ACTIVE.value());
        return user;
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        JiandouAppProperties jiandouAppProperties() {
            JiandouAppProperties properties = new JiandouAppProperties();
            properties.setWebOrigin("http://localhost:5173,http://localhost:5174");
            return properties;
        }

        @Bean
        JiandouStorageProperties jiandouStorageProperties() {
            return new JiandouStorageProperties();
        }

        @Bean
        JiandouTaskOpsProperties jiandouTaskOpsProperties() {
            return new JiandouTaskOpsProperties();
        }
    }
}
