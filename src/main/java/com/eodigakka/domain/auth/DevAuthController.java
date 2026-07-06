package com.eodigakka.domain.auth;

import com.eodigakka.domain.user.UserResponse;
import com.eodigakka.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile({"local", "dev"})
@Tag(name = "Dev Auth", description = "local/dev 전용 테스트 인증 API")
@RestController
@RequestMapping("/api/dev/auth")
public class DevAuthController {

  private static final String BEARER_TOKEN_TYPE = "Bearer";

  private final DevAuthService devAuthService;
  private final RefreshCookieService refreshCookieService;

  public DevAuthController(
      DevAuthService devAuthService, RefreshCookieService refreshCookieService) {
    this.devAuthService = devAuthService;
    this.refreshCookieService = refreshCookieService;
  }

  @PostMapping("/login")
  @Operation(
      summary = "테스트 로그인",
      description =
          """
          local/dev 프로필에서만 활성화되는 테스트 로그인 API입니다.

          카카오 OAuth code 교환 없이 테스트 사용자를 생성하거나 재사용하고, 기존 카카오 로그인과 동일하게 access token을 응답 body로 반환합니다.
          refresh token은 기존 인증 흐름과 동일하게 HttpOnly Cookie로 설정됩니다.
          요청 body를 생략하면 기본 테스트 사용자로 로그인합니다.
          """,
      responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "테스트 로그인 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "요청값이 올바르지 않음",
            content = @Content)
      })
  public ResponseEntity<ApiResponse<KakaoLoginResponse>> login(
      @Valid @RequestBody(required = false) DevLoginRequest request) {
    AuthLoginResult result = devAuthService.login(request);
    KakaoLoginResponse response =
        new KakaoLoginResponse(
            result.accessToken(),
            BEARER_TOKEN_TYPE,
            devAuthService.accessTokenExpirationSeconds(),
            UserResponse.from(result.user()),
            result.isNewUser());

    return ResponseEntity.ok()
        .header(
            HttpHeaders.SET_COOKIE,
            refreshCookieService.createCookie(result.refreshTokenIssue()).toString())
        .body(ApiResponse.success(response));
  }
}
