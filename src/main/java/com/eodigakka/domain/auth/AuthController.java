package com.eodigakka.domain.auth;

import com.eodigakka.domain.user.UserResponse;
import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import com.eodigakka.global.response.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Arrays;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private static final String BEARER_TOKEN_TYPE = "Bearer";

  private final AuthService authService;
  private final RefreshCookieService refreshCookieService;

  public AuthController(AuthService authService, RefreshCookieService refreshCookieService) {
    this.authService = authService;
    this.refreshCookieService = refreshCookieService;
  }

  @PostMapping("/kakao")
  public ResponseEntity<ApiResponse<KakaoLoginResponse>> kakaoLogin(
      @Valid @RequestBody KakaoLoginRequest request) {
    AuthLoginResult result = authService.loginWithKakao(request.code(), request.redirectUri());
    KakaoLoginResponse response =
        new KakaoLoginResponse(
            result.accessToken(),
            BEARER_TOKEN_TYPE,
            authService.accessTokenExpirationSeconds(),
            UserResponse.from(result.user()),
            result.isNewUser());

    return ResponseEntity.ok()
        .header(
            HttpHeaders.SET_COOKIE,
            refreshCookieService.createCookie(result.refreshTokenIssue()).toString())
        .body(ApiResponse.success(response));
  }

  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(HttpServletRequest request) {
    AuthRefreshResult result = authService.refresh(getRefreshToken(request));
    TokenRefreshResponse response =
        new TokenRefreshResponse(
            result.accessToken(), BEARER_TOKEN_TYPE, authService.accessTokenExpirationSeconds());

    return ResponseEntity.ok()
        .header(
            HttpHeaders.SET_COOKIE,
            refreshCookieService.createCookie(result.refreshTokenIssue()).toString())
        .body(ApiResponse.success(response));
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
    authService.logout(findRefreshToken(request));
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshCookieService.clearCookie().toString())
        .body(ApiResponse.success());
  }

  private String getRefreshToken(HttpServletRequest request) {
    String refreshToken = findRefreshToken(request);
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
    }
    return refreshToken;
  }

  private String findRefreshToken(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    return Arrays.stream(cookies)
        .filter(cookie -> refreshCookieService.cookieName().equals(cookie.getName()))
        .map(Cookie::getValue)
        .findFirst()
        .orElse(null);
  }
}
