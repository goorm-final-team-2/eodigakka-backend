package com.eodigakka.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eodigakka.domain.appointment.GuestCookieProperties;
import com.eodigakka.domain.appointment.GuestCookieService;
import com.eodigakka.global.error.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;

class WebSocketAuthenticationServiceTest {

  private static final Instant NOW = Instant.parse("2026-06-21T00:00:00Z");

  private final JwtTokenProvider jwtTokenProvider =
      new JwtTokenProvider(
          new JwtProperties("test-secret-key", 1800, 1209600),
          new ObjectMapper(),
          Clock.fixed(NOW, ZoneOffset.UTC));
  private final GuestCookieService guestCookieService =
      new GuestCookieService(new GuestCookieProperties("guestSession", "/", false, "Lax", 30));
  private final WebSocketAuthenticationService authenticationService =
      new WebSocketAuthenticationService(jwtTokenProvider, guestCookieService);

  @Test
  void authenticateUserFromBearerToken() {
    String token = jwtTokenProvider.createAccessToken(1L);
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.addNativeHeader("Authorization", "Bearer " + token);

    Authentication authentication = authenticationService.authenticate(accessor);

    assertThat(authentication.getPrincipal()).isEqualTo(new AuthUser(1L));
    assertThat(authentication.getAuthorities())
        .extracting("authority")
        .containsExactly(SecurityAuthority.USER);
  }

  @Test
  void authenticateGuestFromCookieHeader() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.addNativeHeader("Cookie", "other=value; guestSession=guest-token");

    Authentication authentication = authenticationService.authenticate(accessor);

    assertThat(authentication.getPrincipal()).isEqualTo(new GuestUser("guest-token"));
    assertThat(authentication.getAuthorities())
        .extracting("authority")
        .containsExactly(SecurityAuthority.GUEST);
  }

  @Test
  void authenticateGuestFromHandshakeSessionAttribute() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.setSessionAttributes(
        new HashMap<>(
            Map.of(WebSocketAuthenticationService.GUEST_SESSION_ATTRIBUTE, "guest-token")));

    Authentication authentication = authenticationService.authenticate(accessor);

    assertThat(authentication.getPrincipal()).isEqualTo(new GuestUser("guest-token"));
  }

  @Test
  void authenticateThrowsBusinessExceptionWhenCredentialsAreMissing() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);

    assertThatThrownBy(() -> authenticationService.authenticate(accessor))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void authenticateDoesNotUseGuestSessionNativeHeader() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.addNativeHeader("X-Guest-Session", "guest-token");

    assertThatThrownBy(() -> authenticationService.authenticate(accessor))
        .isInstanceOf(BusinessException.class);
  }
}
