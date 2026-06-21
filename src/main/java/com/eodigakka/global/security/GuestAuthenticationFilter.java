package com.eodigakka.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class GuestAuthenticationFilter extends OncePerRequestFilter {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String GUEST_TOKEN_HEADER = "X-Guest-Token";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (SecurityContextHolder.getContext().getAuthentication() != null
        || StringUtils.hasText(request.getHeader(AUTHORIZATION_HEADER))) {
      filterChain.doFilter(request, response);
      return;
    }

    String guestToken = request.getHeader(GUEST_TOKEN_HEADER);
    if (StringUtils.hasText(guestToken)) {
      SecurityContextHolder.getContext()
          .setAuthentication(new GuestAuthenticationToken(new GuestUser(guestToken)));
    }

    filterChain.doFilter(request, response);
  }
}
