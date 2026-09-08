package com.xucheng.aicareer.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI aiCareerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI职途后端接口")
                        .description("大学生智能职业成长平台 REST API")
                        .version("1.0"))
                .components(new Components().addSecuritySchemes(
                        SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    @Bean
    public GroupedOpenApi aiCareerApiGroup() {
        return GroupedOpenApi.builder()
                .group("ai-career")
                .displayName("AI职途 API")
                .pathsToMatch("/api/**")
                .packagesToScan("com.xucheng.aicareer.controller")
                .build();
    }
}
