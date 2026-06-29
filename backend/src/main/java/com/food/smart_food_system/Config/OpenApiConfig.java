package com.food.smart_food_system.Config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures Swagger / OpenAPI 3 docs at /swagger-ui.html and /v3/api-docs.
 * Registers the JWT bearer security scheme so the "Authorize" button works.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartFoodOpenAPI() {
        final String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Food System API")
                        .version("v1")
                        .description("REST API cho hệ thống website quản lý đồ ăn tích hợp AI. "
                                + "Tính năng AI cốt lõi: gợi ý món ăn dựa trên BMI và tình trạng "
                                + "sức khỏe của người dùng (endpoint /api/health/*).")
                        .contact(new Contact()
                                .name("Lê Trần Tiến Bảo")
                                .email("2251172247@e.tlu.edu.vn")))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
