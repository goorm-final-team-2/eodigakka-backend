package com.eodigakka.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class RefreshCookieServiceTest {

  @Test
  void createCookieUsesFrontendRefreshTokenContract() {
    RefreshCookieService refreshCookieService =
        new RefreshCookieService(
            new RefreshCookieProperties("refreshToken", "/api/auth", false, "Lax"));
    RefreshTokenIssue refreshTokenIssue =
        new RefreshTokenIssue("refresh-token", Instant.now().plusSeconds(1209600));

    ResponseCookie cookie = refreshCookieService.createCookie(refreshTokenIssue);

    assertThat(cookie.getName()).isEqualTo("refreshToken");
    assertThat(cookie.getValue()).isEqualTo("refresh-token");
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.isSecure()).isFalse();
    assertThat(cookie.getSameSite()).isEqualTo("Lax");
    assertThat(cookie.getPath()).isEqualTo("/api/auth");
    assertThat(cookie.getMaxAge()).isPositive();
  }

  @Test
  void createCookieReflectsSecurePropertyForProductionCookie() {
    RefreshCookieService refreshCookieService =
        new RefreshCookieService(
            new RefreshCookieProperties("refreshToken", "/api/auth", true, "None"));
    RefreshTokenIssue refreshTokenIssue =
        new RefreshTokenIssue("refresh-token", Instant.now().plusSeconds(1209600));

    ResponseCookie cookie = refreshCookieService.createCookie(refreshTokenIssue);

    assertThat(cookie.isSecure()).isTrue();
    assertThat(cookie.getSameSite()).isEqualTo("None");
  }

  @Test
  void clearCookieExpiresRefreshCookieOnAuthPath() {
    RefreshCookieService refreshCookieService =
        new RefreshCookieService(
            new RefreshCookieProperties("refreshToken", "/api/auth", false, "Lax"));

    ResponseCookie cookie = refreshCookieService.clearCookie();

    assertThat(cookie.getName()).isEqualTo("refreshToken");
    assertThat(cookie.getValue()).isEmpty();
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.getPath()).isEqualTo("/api/auth");
    assertThat(cookie.getMaxAge()).isZero();
  }
}
