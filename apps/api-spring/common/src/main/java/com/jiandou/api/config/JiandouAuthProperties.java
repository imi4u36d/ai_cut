package com.jiandou.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 鉴权相关配置。
 */
@ConfigurationProperties(prefix = "jiandou.auth")
public class JiandouAuthProperties {

    private int inviteExpiryHours = 12;
    private String credentialSecret = "dev-user-credential-secret-change-me";
    private final Bootstrap bootstrap = new Bootstrap();

    public int getInviteExpiryHours() {
        return inviteExpiryHours;
    }

    public void setInviteExpiryHours(int inviteExpiryHours) {
        this.inviteExpiryHours = inviteExpiryHours <= 0 ? 12 : inviteExpiryHours;
    }

    public String getCredentialSecret() {
        return credentialSecret;
    }

    public void setCredentialSecret(String credentialSecret) {
        this.credentialSecret = credentialSecret == null ? "" : credentialSecret.trim();
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    /**
     * 首个管理员引导配置。
     */
    public static class Bootstrap {

        private final InitialAdmin initialAdmin = new InitialAdmin();

        public InitialAdmin getInitialAdmin() {
            return initialAdmin;
        }
    }

    /**
     * 首个管理员配置。
     */
    public static class InitialAdmin {

        private String username = "admin";
        private String displayName = "系统管理员";
        private String password = "";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username == null ? "admin" : username.trim();
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName == null ? "系统管理员" : displayName.trim();
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password == null ? "" : password.trim();
        }
    }
}
