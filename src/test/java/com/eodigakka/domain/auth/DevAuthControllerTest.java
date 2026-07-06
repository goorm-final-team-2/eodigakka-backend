package com.eodigakka.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.eodigakka.domain.user.SocialProvider;
import com.eodigakka.domain.user.User;
import com.eodigakka.global.response.ApiResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

class DevAuthControllerTest {

  @Test
  void loginReturnsAccessTokenAndHttpOnlyRefreshCookie() {
    DevAuthService devAuthService = org.mockito.Mockito.mock(DevAuthService.class);
    RefreshCookieService refreshCookieService =
        new RefreshCookieService(
            new RefreshCookieProperties("refreshToken", "/api/auth", false, "Lax"));
    DevAuthController controller = new DevAuthController(devAuthService, refreshCookieService);
    User user = User.create(SocialProvider.KAKAO, "dev:frontend", "프론트", null);
    ReflectionTestUtils.setField(user, "id", 10L);
    given(devAuthService.login(new DevLoginRequest("frontend", "프론트", null)))
        .willReturn(
            new AuthLoginResult(
                "access-token",
                new RefreshTokenIssue("refresh-token", Instant.parse("2026-07-04T00:00:00Z")),
                user,
                true));
    given(devAuthService.accessTokenExpirationSeconds()).willReturn(1800L);

    ResponseEntity<ApiResponse<KakaoLoginResponse>> response =
        controller.login(new DevLoginRequest("frontend", "프론트", null));

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().data().accessToken()).isEqualTo("access-token");
    assertThat(response.getBody().data().tokenType()).isEqualTo("Bearer");
    assertThat(response.getBody().data().user().id()).isEqualTo(10L);
    assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
        .contains("refreshToken=refresh-token", "Path=/api/auth", "HttpOnly", "SameSite=Lax");
  }
}
