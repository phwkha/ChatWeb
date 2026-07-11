package com.web.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import org.springdoc.core.customizers.OpenApiCustomizer;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.List;

@Configuration
@Profile({"dev", "test"})
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi groupedOpenApi(@Value("${openapi.service.api-docs}") String apiDocs) {
        return GroupedOpenApi.builder().group(apiDocs)
                .packagesToScan("com.web.backend.controller")
                .build();
    }

    @Bean
    public OpenAPI openAPI(
            @Value("${openapi.service.title}") String title,
            @Value("${openapi.service.version}") String version,
            @Value("${openapi.service.server}") String serverUrl) {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .servers(List.of(new Server().url(serverUrl)))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                     securitySchemeName,
                                     new SecurityScheme()
                                             .type(SecurityScheme.Type.HTTP)
                                             .scheme("bearer")
                                             .bearerFormat("JWT")))
                .security(List.of(new SecurityRequirement().addList(securitySchemeName)))
                .info(new Info().title(title)
                        .version(version)
                        .description("API Document for backend")
                        .license(new License().name("Apace 2.0").url("https://springdoc.org")));
    }
    @Bean
    public OpenApiCustomizer customGlobalOpenApiCustomizer() {
        return openApi -> {
            if (openApi.getPaths() != null) {
                openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
                    ApiResponses apiResponses = operation.getResponses();
                    if (apiResponses == null) {
                        apiResponses = new ApiResponses();
                        operation.setResponses(apiResponses);
                    }
                    if (!apiResponses.containsKey("400")) {
                        apiResponses.addApiResponse("400", new ApiResponse().description("Bad Request - Dữ liệu không hợp lệ"));
                    }
                    if (!apiResponses.containsKey("401")) {
                        apiResponses.addApiResponse("401", new ApiResponse().description("Unauthorized - Chưa xác thực"));
                    }
                    if (!apiResponses.containsKey("403")) {
                        apiResponses.addApiResponse("403", new ApiResponse().description("Forbidden - Không có quyền truy cập"));
                    }
                    if (!apiResponses.containsKey("500")) {
                        apiResponses.addApiResponse("500", new ApiResponse().description("Internal Server Error - Lỗi hệ thống"));
                    }
                }));
            }
        };
    }
}
