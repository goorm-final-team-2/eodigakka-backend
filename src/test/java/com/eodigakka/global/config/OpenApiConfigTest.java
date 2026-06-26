package com.eodigakka.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

  @Test
  void openApiDefinesBearerAndGuestSessionCookieSecuritySchemes() {
    OpenAPI openAPI = new OpenApiConfig().eodigakkaOpenApi();

    assertThat(openAPI.getComponents().getSecuritySchemes())
        .containsKeys("bearerAuth", "guestSessionCookie");
    assertThat(openAPI.getComponents().getSecuritySchemes().get("bearerAuth").getType())
        .isEqualTo(SecurityScheme.Type.HTTP);
    SecurityScheme guestSessionCookie =
        openAPI.getComponents().getSecuritySchemes().get("guestSessionCookie");
    assertThat(guestSessionCookie.getType()).isEqualTo(SecurityScheme.Type.APIKEY);
    assertThat(guestSessionCookie.getIn()).isEqualTo(SecurityScheme.In.COOKIE);
    assertThat(guestSessionCookie.getName()).isEqualTo("guestSession");
  }
}
