package com.jiandou.api.config;

/**
 * 统一管理后端公开路径常量，避免多个控制器和 Web 配置重复硬编码。
 */
public final class ApiPathConstants {

    public static final String API_V3 = "/api/v3";
    public static final String API_V3_PATTERN = API_V3 + "/**";
    public static final String HEALTH = API_V3 + "/health";
    public static final String STORAGE = "/storage";
    public static final String STORAGE_PATTERN = STORAGE + "/**";
    public static final String AUTH = API_V3 + "/auth";
    public static final String AUTH_PATTERN = AUTH + "/**";
    public static final String AUTH_SESSION = AUTH + "/session";
    public static final String AUTH_LOGIN = AUTH + "/login";
    public static final String AUTH_LOGOUT = AUTH + "/logout";
    public static final String AUTH_ACTIVATE_INVITE = AUTH + "/activate-invite";
    public static final String TASKS = API_V3 + "/tasks";
    public static final String TASK_SHOWCASE = TASKS + "/showcase";
    public static final String WORKFLOWS = API_V3 + "/workflows";
    public static final String MATERIAL_ASSETS = API_V3 + "/material-assets";
    public static final String MATERIAL_CENTER = API_V3 + "/material-center";
    public static final String UPLOADS = API_V3 + "/uploads";
    public static final String ADMIN = API_V3 + "/admin";
    public static final String ADMIN_PATTERN = ADMIN + "/**";
    public static final String GENERATION = API_V3 + "/generation";
    public static final String GENERATION_CATALOG = GENERATION + "/catalog";

    private ApiPathConstants() {
    }
}
