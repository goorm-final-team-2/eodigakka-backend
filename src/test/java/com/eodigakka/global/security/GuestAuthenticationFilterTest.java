package com.eodigakka.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.eodigakka.domain.appointment.GuestCookieProperties;
import com.eodigakka.domain.appointment.GuestCookieService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class GuestAuthenticationFilterTest {

  private GuestAuthenticationFilter guestAuthenticationFilter;

  @BeforeEach
  void setUp() {
    GuestCookieService guestCookieService =
        new GuestCookieService(new GuestCookieProperties("guestSession", "/", false, "Lax", 30));
    guestAuthenticationFilter = new GuestAuthenticationFilter(guestCookieService);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createsGuestAuthenticationWhenGuestSessionCookieExists()
      throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie("guestSession", "guest-session-token"));
    MockHttpServletResponse response = new MockHttpServletResponse();

    guestAuthenticationFilter.doFilter(request, response, new MockFilterChain());

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isInstanceOf(GuestAuthenticationToken.class);
    assertThat(authentication.getPrincipal()).isEqualTo(new GuestUser("guest-session-token"));
    assertThat(authentication.getAuthorities())
        .extracting("authority")
        .containsExactly(SecurityAuthority.GUEST);
    assertThat(authentication.isAuthenticated()).isTrue();
  }

  @Test
  void doesNotCreateAuthenticationWhenGuestSessionCookieIsMissing()
      throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    guestAuthenticationFilter.doFilter(request, response, new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void doesNotCreateGuestAuthenticationWhenAuthorizationHeaderExists()
      throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer access-token");
    request.setCookies(new Cookie("guestSession", "guest-session-token"));
    MockHttpServletResponse response = new MockHttpServletResponse();

    guestAuthenticationFilter.doFilter(request, response, new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void keepsExistingAuthenticationWhenSecurityContextAlreadyHasAuthentication()
      throws ServletException, IOException {
    Authentication existingAuthentication =
        new UsernamePasswordAuthenticationToken(new AuthUser(1L), null);
    SecurityContextHolder.getContext().setAuthentication(existingAuthentication);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie("guestSession", "guest-session-token"));
    MockHttpServletResponse response = new MockHttpServletResponse();

    guestAuthenticationFilter.doFilter(request, response, new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication())
        .isSameAs(existingAuthentication);
  }
}
