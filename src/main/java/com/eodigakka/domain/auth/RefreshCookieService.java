package com.eodigakka.domain.auth;

import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshCookieService {

  private final RefreshCookieProperties properties;

  public RefreshCookieService(RefreshCookieProperties properties) {
    this.properties = properties;
  }

  public ResponseCookie createCookie(RefreshTokenIssue refreshTokenIssue) {
    return baseCookie(refreshTokenIssue.token())
        .maxAge(Duration.between(java.time.Instant.now(), refreshTokenIssue.expiresAt()))
        .build();
  }

  public ResponseCookie clearCookie() {
    return baseCookie("").maxAge(Duration.ZERO).build();
  }

  public String cookieName() {
    return properties.name();
  }

  private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
    return ResponseCookie.from(properties.name(), value)
        .httpOnly(true)
        .secure(properties.secure())
        .sameSite(properties.sameSite())
        .path(properties.path());
  }
}
