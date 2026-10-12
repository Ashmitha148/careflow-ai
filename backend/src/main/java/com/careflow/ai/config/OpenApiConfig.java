package com.careflow.ai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI careFlowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CareFlow AI — Clinical Coordination REST API")
                        .description("Production-grade healthcare continuity platform connecting Doctors, Nurses, Caregivers, and Patients. Features an append-only immutable clinical timeline, STOMP-over-WebSocket event streaming, and resilient Gemini AI clinical copilots.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("CareFlow AI Team")
                                .url("https://careflow-ai-beta.vercel.app"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
