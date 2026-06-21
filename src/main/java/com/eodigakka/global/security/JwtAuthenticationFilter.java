package com.eodigakka.global.security;

import com.eodigakka.global.error.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenProvider jwtTokenProvider;
  private final CustomAuthenticationEntryPoint authenticationEntryPoint;

  public JwtAuthenticationFilter(
      JwtTokenProvider jwtTokenProvider, CustomAuthenticationEntryPoint authenticationEntryPoint) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.authenticationEntryPoint = authenticationEntryPoint;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

    if (!StringUtils.hasText(authorizationHeader)) {
      filterChain.doFilter(request, response);
      return;
    }

    if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
      reject(request, response);
      return;
    }

    try {
      Long userId =
          jwtTokenProvider.parseUserId(authorizationHeader.substring(BEARER_PREFIX.length()));
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(new AuthUser(userId), null, List.of());
      SecurityContextHolder.getContext().setAuthentication(authentication);
      filterChain.doFilter(request, response);
    } catch (BusinessException exception) {
      reject(request, response);
    }
  }

  private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
    SecurityContextHolder.clearContext();
    authenticationEntryPoint.commence(
        request, response, new BadCredentialsException("Invalid access token."));
  }
}
