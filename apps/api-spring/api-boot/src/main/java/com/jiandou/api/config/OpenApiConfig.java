package com.jiandou.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 契约配置。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI jiandouOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("JianDou API")
                .version("v3")
                .description("JianDou modular monolith API contract"))
            .servers(List.of(new Server().url(ApiPathConstants.API_V3).description("API v3 base path")));
    }
}
