package com.eodigakka.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.eodigakka.domain.appointment.GuestCookieProperties;
import com.eodigakka.domain.appointment.GuestCookieService;
import jakarta.servlet.http.Cookie;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.socket.WebSocketHandler;

class GuestSessionHandshakeInterceptorTest {

  private final GuestCookieService guestCookieService =
      new GuestCookieService(new GuestCookieProperties("guestSession", "/", false, "Lax", 30));
  private final GuestSessionHandshakeInterceptor interceptor =
      new GuestSessionHandshakeInterceptor(guestCookieService);

  @Test
  void storesGuestSessionCookieInHandshakeAttributes() {
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    servletRequest.setCookies(new Cookie("guestSession", "guest-token"));
    ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
    Map<String, Object> attributes = new HashMap<>();

    boolean result =
        interceptor.beforeHandshake(request, null, mock(WebSocketHandler.class), attributes);

    assertThat(result).isTrue();
    assertThat(attributes)
        .containsEntry(WebSocketAuthenticationService.GUEST_SESSION_ATTRIBUTE, "guest-token");
  }

  @Test
  void doesNotStoreBlankGuestSessionCookie() {
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    servletRequest.setCookies(new Cookie("guestSession", " "));
    ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
    Map<String, Object> attributes = new HashMap<>();

    boolean result =
        interceptor.beforeHandshake(request, null, mock(WebSocketHandler.class), attributes);

    assertThat(result).isTrue();
    assertThat(attributes)
        .doesNotContainKey(WebSocketAuthenticationService.GUEST_SESSION_ATTRIBUTE);
  }

  @Test
  void ignoresNonServletHandshakeRequest() {
    ServerHttpRequest request = mock(ServerHttpRequest.class);
    Map<String, Object> attributes = new HashMap<>();

    boolean result =
        interceptor.beforeHandshake(request, null, mock(WebSocketHandler.class), attributes);

    assertThat(result).isTrue();
    assertThat(attributes)
        .doesNotContainKey(WebSocketAuthenticationService.GUEST_SESSION_ATTRIBUTE);
  }
}
