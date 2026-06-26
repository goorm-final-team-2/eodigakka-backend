package com.eodigakka.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigTest {

  @Test
  void corsAllowsConfiguredFrontendOriginsWithCredentials() {
    SecurityConfig securityConfig = new SecurityConfig(null, null, null, null);
    CorsConfigurationSource source =
        securityConfig.corsConfigurationSource(
            "http://localhost:5173, https://eodigakka.example.com ");
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/auth/kakao");

    CorsConfiguration configuration = source.getCorsConfiguration(request);

    assertThat(configuration).isNotNull();
    assertThat(configuration.getAllowedOrigins())
        .containsExactly("http://localhost:5173", "https://eodigakka.example.com");
    assertThat(configuration.getAllowCredentials()).isTrue();
  }

  @Test
  void corsAllowsFrontendAuthHeadersAndHttpMethods() {
    SecurityConfig securityConfig = new SecurityConfig(null, null, null, null);
    CorsConfigurationSource source =
        securityConfig.corsConfigurationSource("http://localhost:5173");
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/appointments");

    CorsConfiguration configuration = source.getCorsConfiguration(request);

    assertThat(configuration).isNotNull();
    assertThat(configuration.getAllowedMethods())
        .containsExactly("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    assertThat(configuration.getAllowedHeaders()).containsExactly("*");
    assertThat(configuration.getExposedHeaders()).containsExactly("Location");
    assertThat(configuration.checkHeaders(List.of("Authorization", "Content-Type")))
        .containsExactly("Authorization", "Content-Type");
  }
}
