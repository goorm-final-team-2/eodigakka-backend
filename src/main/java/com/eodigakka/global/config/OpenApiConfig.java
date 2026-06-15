package com.eodigakka.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private static final String BEARER_AUTH = "bearerAuth";

  @Bean
  public OpenAPI eodigakkaOpenApi() {
    SecurityScheme securityScheme =
        new SecurityScheme()
            .name(BEARER_AUTH)
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT");

    return new OpenAPI()
        .info(new Info().title("어디가까 API").description("어디가까 백엔드 REST API 문서").version("v1"))
        .components(new Components().addSecuritySchemes(BEARER_AUTH, securityScheme))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
  }
}
