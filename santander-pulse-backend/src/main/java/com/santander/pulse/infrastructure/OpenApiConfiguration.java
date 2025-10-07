package com.santander.pulse.infrastructure;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for API documentation.
 * Configures JWT authentication and API metadata.
 */
@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Santander Pulse Backend API")
                .version("1.0.0")
                .description("Banking backend API with H2 database and JWT authentication")
                .contact(new Contact()
                    .name("Cezi Cola Senior Software Engineer")
                    .email("support@santander.com")
                    .url("https://santander.com")
                )
                .license(new License()
                    .name("Proprietary")
                    .url("https://santander.com/license")
                )
            )
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT token for API authentication")
                )
            );
    }
}