package com.eodigakka.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

  @Test
  void openApiDefinesBearerAndGuestSessionCookieSecuritySchemes() {
    OpenAPI openAPI = new OpenApiConfig("https://api.eodigakka.xyz").eodigakkaOpenApi();

    assertThat(openAPI.getServers()).hasSize(1);
    assertThat(openAPI.getServers().getFirst().getUrl()).isEqualTo("https://api.eodigakka.xyz");
    assertThat(openAPI.getComponents().getSecuritySchemes())
        .containsKeys("bearerAuth", "guestSessionCookie");
    assertThat(openAPI.getComponents().getSecuritySchemes().get("bearerAuth").getType())
        .isEqualTo(SecurityScheme.Type.HTTP);
    SecurityScheme guestSessionCookie =
        openAPI.getComponents().getSecuritySchemes().get("guestSessionCookie");
    assertThat(guestSessionCookie.getType()).isEqualTo(SecurityScheme.Type.APIKEY);
    assertThat(guestSessionCookie.getIn()).isEqualTo(SecurityScheme.In.COOKIE);
    assertThat(guestSessionCookie.getName()).isEqualTo("guestSession");
    assertThat(guestSessionCookie.getDescription())
        .contains("guestSession HttpOnly Cookie")
        .contains("X-Guest-Token")
        .contains("X-Guest-Session");
  }
}
