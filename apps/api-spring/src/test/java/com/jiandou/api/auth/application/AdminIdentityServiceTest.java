package com.jiandou.api.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jiandou.api.auth.domain.UserRole;
import com.jiandou.api.auth.domain.UserStatus;
import com.jiandou.api.auth.infrastructure.mybatis.MybatisAuthRepository;
import com.jiandou.api.auth.infrastructure.mybatis.SysUserEntity;
import com.jiandou.api.auth.web.dto.AdminUserResponse;
import com.jiandou.api.auth.web.dto.CreateAdminUserRequest;
import com.jiandou.api.auth.web.dto.UpdateAdminUserPasswordRequest;
import com.jiandou.api.auth.web.dto.UpdateAdminUserRequest;
import com.jiandou.api.common.exception.ApiException;
import com.jiandou.api.config.JiandouAuthProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
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

    private SysUserEntity user(Long id, String username, UserRole role, UserStatus status) {
        SysUserEntity user = new SysUserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setDisplayName(username);
        user.setRole(role.value());
        user.setStatus(status.value());
        return user;
    }
}
