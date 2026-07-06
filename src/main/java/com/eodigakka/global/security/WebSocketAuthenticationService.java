package com.eodigakka.global.security;

import com.eodigakka.domain.appointment.GuestCookieService;
import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WebSocketAuthenticationService {

  public static final String GUEST_SESSION_ATTRIBUTE = "guestSession";

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String COOKIE_HEADER = "Cookie";
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenProvider jwtTokenProvider;
  private final GuestCookieService guestCookieService;

  public WebSocketAuthenticationService(
      JwtTokenProvider jwtTokenProvider, GuestCookieService guestCookieService) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.guestCookieService = guestCookieService;
  }

  public Authentication authenticate(StompHeaderAccessor accessor) {
    String authorizationHeader = firstNativeHeader(accessor, AUTHORIZATION_HEADER);
    if (StringUtils.hasText(authorizationHeader)) {
      return authenticateUser(authorizationHeader);
    }

    String guestSessionToken = sessionAttribute(accessor, GUEST_SESSION_ATTRIBUTE);
    if (!StringUtils.hasText(guestSessionToken)) {
      guestSessionToken = findGuestSessionToken(firstNativeHeader(accessor, COOKIE_HEADER));
    }
    if (StringUtils.hasText(guestSessionToken)) {
      return new GuestAuthenticationToken(new GuestUser(guestSessionToken));
    }

    throw new BusinessException(ErrorCode.UNAUTHORIZED);
  }

  public AuthUser authUser(Principal principal) {
    Object principalValue = principalValue(principal);
    return principalValue instanceof AuthUser authUser ? authUser : null;
  }

  public String guestSessionToken(Principal principal) {
    Object principalValue = principalValue(principal);
    return principalValue instanceof GuestUser guestUser ? guestUser.sessionToken() : null;
  }

  private Authentication authenticateUser(String authorizationHeader) {
    if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
      throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
    }

    Long userId =
        jwtTokenProvider.parseUserId(authorizationHeader.substring(BEARER_PREFIX.length()));
    return new UsernamePasswordAuthenticationToken(
        new AuthUser(userId), null, AuthorityUtils.createAuthorityList(SecurityAuthority.USER));
  }

  private String firstNativeHeader(StompHeaderAccessor accessor, String name) {
    List<String> values = accessor.getNativeHeader(name);
    if (values == null || values.isEmpty()) {
      return null;
    }
    return values.getFirst();
  }

  private String sessionAttribute(StompHeaderAccessor accessor, String name) {
    Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
    if (sessionAttributes == null) {
      return null;
    }
    Object value = sessionAttributes.get(name);
    return value instanceof String stringValue ? stringValue : null;
  }

  private String findGuestSessionToken(String cookieHeader) {
    if (!StringUtils.hasText(cookieHeader)) {
      return null;
    }

    String cookieName = guestCookieService.cookieName();
    for (String cookie : cookieHeader.split(";")) {
      String[] nameAndValue = cookie.trim().split("=", 2);
      if (nameAndValue.length == 2 && cookieName.equals(nameAndValue[0])) {
        return nameAndValue[1];
      }
    }
    return null;
  }

  private Object principalValue(Principal principal) {
    if (principal instanceof Authentication authentication) {
      return authentication.getPrincipal();
    }
    return null;
  }
}
