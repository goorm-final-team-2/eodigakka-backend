package com.eodigakka.global.security;

import com.eodigakka.domain.appointment.GuestCookieService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class GuestAuthenticationFilter extends OncePerRequestFilter {

  private static final String AUTHORIZATION_HEADER = "Authorization";

  private final GuestCookieService guestCookieService;

  public GuestAuthenticationFilter(GuestCookieService guestCookieService) {
    this.guestCookieService = guestCookieService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (SecurityContextHolder.getContext().getAuthentication() != null
        || StringUtils.hasText(request.getHeader(AUTHORIZATION_HEADER))) {
      filterChain.doFilter(request, response);
      return;
    }

    String guestSessionToken = findGuestSessionToken(request);
    if (StringUtils.hasText(guestSessionToken)) {
      SecurityContextHolder.getContext()
          .setAuthentication(new GuestAuthenticationToken(new GuestUser(guestSessionToken)));
    }

    filterChain.doFilter(request, response);
  }

  private String findGuestSessionToken(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    return Arrays.stream(cookies)
        .filter(cookie -> guestCookieService.cookieName().equals(cookie.getName()))
        .map(Cookie::getValue)
        .findFirst()
        .orElse(null);
  }
}
