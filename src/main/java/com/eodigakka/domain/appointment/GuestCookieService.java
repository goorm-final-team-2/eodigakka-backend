package com.eodigakka.domain.appointment;

import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class GuestCookieService {

  private final GuestCookieProperties properties;

  public GuestCookieService(GuestCookieProperties properties) {
    this.properties = properties;
  }

  public ResponseCookie createCookie(GuestSessionIssue guestSessionIssue) {
    return baseCookie(guestSessionIssue.token())
        .maxAge(Duration.ofDays(properties.maxAgeDays()))
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
