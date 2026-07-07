package com.eodigakka.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.ServerBaseUrlCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private static final String BEARER_AUTH = "bearerAuth";
  private static final String GUEST_SESSION_COOKIE = "guestSessionCookie";

  private final String serverUrl;

  public OpenApiConfig(@Value("${app.openapi.server-url}") String serverUrl) {
    this.serverUrl = serverUrl;
  }

  @Bean
  public OpenAPI eodigakkaOpenApi() {
    SecurityScheme bearerAuthScheme =
        new SecurityScheme()
            .name(BEARER_AUTH)
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("로그인 사용자는 Access Token을 Authorization: Bearer 형식으로 전달합니다.");
    SecurityScheme guestSessionCookieScheme =
        new SecurityScheme()
            .name("guestSession")
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.COOKIE)
            .description(
                "게스트 사용자는 게스트 입장 API에서 발급된 guestSession HttpOnly Cookie로 인증합니다. "
                    + "X-Guest-Token 또는 X-Guest-Session 헤더는 사용하지 않습니다.");

    return new OpenAPI()
        .info(
            new Info()
                .title("어디가까 API")
                .description(
                    """
                    어디가까 백엔드 REST API 문서입니다.

                    로그인 사용자는 bearerAuth를 사용합니다.
                    게스트 사용자는 guestSession HttpOnly Cookie를 사용하며, 프론트 요청에는 credentials 포함이 필요합니다.
                    게스트 인증에 X-Guest-Token 또는 X-Guest-Session 헤더를 사용하지 않습니다.
                    """)
                .version("v1"))
        .components(
            new Components()
                .addSecuritySchemes(BEARER_AUTH, bearerAuthScheme)
                .addSecuritySchemes(GUEST_SESSION_COOKIE, guestSessionCookieScheme))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
        .addSecurityItem(new SecurityRequirement().addList(GUEST_SESSION_COOKIE));
  }

  @Bean
  public ServerBaseUrlCustomizer serverBaseUrlCustomizer() {
    return (serverBaseUrl, request) -> serverUrl;
  }
}
