package com.jiandou.api.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jiandou.api.auth.domain.UserRole;
import com.jiandou.api.auth.domain.UserStatus;
import com.jiandou.api.auth.infrastructure.mybatis.MybatisAuthRepository;
import com.jiandou.api.auth.infrastructure.mybatis.SysInviteCodeEntity;
import com.jiandou.api.auth.infrastructure.mybatis.SysUserEntity;
import com.jiandou.api.auth.security.CurrentUserPrincipal;
import com.jiandou.api.auth.web.dto.AdminInviteResponse;
import com.jiandou.api.auth.web.dto.AdminUserResponse;
import com.jiandou.api.auth.web.dto.CreateAdminUserRequest;
import com.jiandou.api.auth.web.dto.CreateInviteRequest;
import com.jiandou.api.auth.web.dto.UpdateAdminUserPasswordRequest;
import com.jiandou.api.auth.web.dto.UpdateAdminUserRequest;
import com.jiandou.api.common.exception.ApiException;
import com.jiandou.api.config.JiandouAuthProperties;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminIdentityServiceTest {

    @Test
    void listUsersSupportsFiltering() {
        MybatisAuthRepository repository = mock(MybatisAuthRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AdminIdentityService service = new AdminIdentityService(repository, new JiandouAuthProperties(), passwordEncoder);
        SysUserEntity admin = user(1L, "admin", UserRole.ADMIN, UserStatus.ACTIVE);

        when(repository.listUsers("adm", UserRole.ADMIN.value(), UserStatus.ACTIVE.value())).thenReturn(List.of(admin));

        List<AdminUserResponse> result = service.listUsers("adm", "admin", "active");

        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).username());
    }

    @Test
    void createUserEncodesPasswordAndPersistsUser() {
        MybatisAuthRepository repository = mock(MybatisAuthRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AdminIdentityService service = new AdminIdentityService(repository, new JiandouAuthProperties(), passwordEncoder);
        CreateAdminUserRequest request = new CreateAdminUserRequest("operator", "Operator", "secret123", "USER", "ACTIVE");
        SysUserEntity created = user(2L, "operator", UserRole.USER, UserStatus.ACTIVE);

        when(repository.findUserByUsername("operator")).thenReturn(null);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");
        when(repository.createUser("operator", "Operator", "encoded-password", UserRole.USER, UserStatus.ACTIVE)).thenReturn(created);

        AdminUserResponse result = service.createUser(request);

        assertEquals(2L, result.id());
        verify(passwordEncoder).encode("secret123");
    }

    @Test
    void updateUserRejectsDemotingLastAdmin() {
        MybatisAuthRepository repository = mock(MybatisAuthRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AdminIdentityService service = new AdminIdentityService(repository, new JiandouAuthProperties(), passwordEncoder);
        SysUserEntity admin = user(1L, "admin", UserRole.ADMIN, UserStatus.ACTIVE);

        when(repository.findUserById(1L)).thenReturn(admin);
        when(repository.countAdmins()).thenReturn(1L);

        ApiException exception = assertThrows(
            ApiException.class,
            () -> service.updateUser(1L, new UpdateAdminUserRequest("Admin", "USER", "ACTIVE"))
        );

        assertEquals("last_admin_guard", exception.code());
    }

    @Test
    void updatePasswordUsesPasswordEncoder() {
        MybatisAuthRepository repository = mock(MybatisAuthRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AdminIdentityService service = new AdminIdentityService(repository, new JiandouAuthProperties(), passwordEncoder);
        SysUserEntity admin = user(1L, "admin", UserRole.ADMIN, UserStatus.ACTIVE);

        when(repository.findUserById(1L)).thenReturn(admin);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");
        when(repository.updateUserPassword(1L, "encoded-password")).thenReturn(admin);

        AdminUserResponse result = service.updateUserPassword(1L, new UpdateAdminUserPasswordRequest("secret123"));

        assertEquals(1L, result.id());
        verify(repository).updateUserPassword(1L, "encoded-password");
    }

    @Test
    void deleteUserRejectsRemovingLastActiveAdmin() {
        MybatisAuthRepository repository = mock(MybatisAuthRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AdminIdentityService service = new AdminIdentityService(repository, new JiandouAuthProperties(), passwordEncoder);
        SysUserEntity admin = user(1L, "admin", UserRole.ADMIN, UserStatus.ACTIVE);

        when(repository.findUserById(1L)).thenReturn(admin);
        when(repository.countAdmins()).thenReturn(1L);

        ApiException exception = assertThrows(ApiException.class, () -> service.deleteUser(1L));

        assertEquals("last_admin_guard", exception.code());
    }

    @Test
    void createInviteUsesTwelveHourExpiryByDefault() {
        MybatisAuthRepository repository = mock(MybatisAuthRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JiandouAuthProperties properties = new JiandouAuthProperties();
        AdminIdentityService service = new AdminIdentityService(repository, properties, passwordEncoder);
        CurrentUserPrincipal actor = new CurrentUserPrincipal(1L, "admin", "Admin", "ADMIN", "ACTIVE");
        OffsetDateTime before = OffsetDateTime.now();
        SysInviteCodeEntity invite = invite(10L, "ABCD1234", UserRole.USER, before.plusHours(12));
        SysUserEntity admin = user(1L, "admin", UserRole.ADMIN, UserStatus.ACTIVE);
        Map<Long, SysUserEntity> usersById = new HashMap<>();
        usersById.put(1L, admin);
        ArgumentCaptor<OffsetDateTime> expiresAtCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);

        when(repository.createInvite(eq(UserRole.USER), any(), eq(1L))).thenReturn(invite);
        when(repository.findUsersByIds(Set.of(1L))).thenReturn(usersById);

        AdminInviteResponse result = service.createInvite(new CreateInviteRequest("USER"), actor);

        assertEquals("ABCD1234", result.code());
        assertEquals("USER", result.role());
        verify(repository).createInvite(eq(UserRole.USER), expiresAtCaptor.capture(), eq(1L));
        long expiryMinutes = Duration.between(before, expiresAtCaptor.getValue()).toMinutes();
        assertTrue(expiryMinutes >= 719 && expiryMinutes <= 720);
    }

    private SysUserEntity user(Long id, String username, UserRole role, UserStatus status) {
        SysUserEntity user = new SysUserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setDisplayName(username);
        user.setRole(role.value());
        user.setStatus(status.value());
        return user;
    }

    private SysInviteCodeEntity invite(Long id, String code, UserRole role, OffsetDateTime expiresAt) {
        SysInviteCodeEntity invite = new SysInviteCodeEntity();
        invite.setId(id);
        invite.setCode(code);
        invite.setRole(role.value());
        invite.setStatus("UNUSED");
        invite.setExpiresAt(expiresAt);
        invite.setCreatedBy(1L);
        invite.setCreatedAt(OffsetDateTime.now());
        invite.setUpdatedAt(OffsetDateTime.now());
        return invite;
    }
}
