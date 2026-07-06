package com.eodigakka.global.security;

import com.eodigakka.domain.appointment.GuestCookieService;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class GuestSessionHandshakeInterceptor implements HandshakeInterceptor {

  private final GuestCookieService guestCookieService;

  public GuestSessionHandshakeInterceptor(GuestCookieService guestCookieService) {
    this.guestCookieService = guestCookieService;
  }

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) {
    String guestSessionToken = findGuestSessionToken(request);
    if (StringUtils.hasText(guestSessionToken)) {
      attributes.put(WebSocketAuthenticationService.GUEST_SESSION_ATTRIBUTE, guestSessionToken);
    }
    return true;
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {}

  private String findGuestSessionToken(ServerHttpRequest request) {
    if (!(request instanceof ServletServerHttpRequest servletRequest)) {
      return null;
    }
    Cookie[] cookies = servletRequest.getServletRequest().getCookies();
    if (cookies == null) {
      return null;
    }
    String cookieName = guestCookieService.cookieName();
    for (Cookie cookie : cookies) {
      if (cookieName.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }
}
