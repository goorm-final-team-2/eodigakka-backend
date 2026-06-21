package com.eodigakka.domain.auth;

import com.eodigakka.domain.auth.kakao.KakaoAuthClient;
import com.eodigakka.domain.auth.kakao.KakaoProperties;
import com.eodigakka.domain.auth.kakao.KakaoUserInfo;
import com.eodigakka.domain.user.SocialProvider;
import com.eodigakka.domain.user.User;
import com.eodigakka.domain.user.UserRepository;
import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import com.eodigakka.global.security.JwtTokenProvider;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AuthService {

  private final KakaoAuthClient kakaoAuthClient;
  private final KakaoProperties kakaoProperties;
  private final UserRepository userRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenService refreshTokenService;
  private final TransactionTemplate transactionTemplate;

  public AuthService(
      KakaoAuthClient kakaoAuthClient,
      KakaoProperties kakaoProperties,
      UserRepository userRepository,
      JwtTokenProvider jwtTokenProvider,
      RefreshTokenService refreshTokenService,
      TransactionTemplate transactionTemplate) {
    this.kakaoAuthClient = kakaoAuthClient;
    this.kakaoProperties = kakaoProperties;
    this.userRepository = userRepository;
    this.jwtTokenProvider = jwtTokenProvider;
    this.refreshTokenService = refreshTokenService;
    this.transactionTemplate = transactionTemplate;
  }

  public AuthLoginResult loginWithKakao(String code, String redirectUri) {
    if (!kakaoProperties.isAllowedRedirectUri(redirectUri)) {
      throw new BusinessException(ErrorCode.AUTH_REDIRECT_URI_NOT_ALLOWED);
    }

    KakaoUserInfo kakaoUserInfo = kakaoAuthClient.getUserInfo(code, redirectUri);
    return Objects.requireNonNull(
        transactionTemplate.execute(status -> loginOrRegister(kakaoUserInfo)));
  }

  private AuthLoginResult loginOrRegister(KakaoUserInfo kakaoUserInfo) {
    Optional<User> foundUser =
        userRepository.findBySocialProviderAndSocialId(
            SocialProvider.KAKAO, kakaoUserInfo.socialId());
    boolean isNewUser = foundUser.isEmpty();
    User user = foundUser.orElseGet(() -> createKakaoUser(kakaoUserInfo));
    user.updateProfile(kakaoUserInfo.nickname(), kakaoUserInfo.profileImage());

    String accessToken = jwtTokenProvider.createAccessToken(user.getId());
    RefreshTokenIssue refreshTokenIssue = refreshTokenService.issue(user);
    return new AuthLoginResult(accessToken, refreshTokenIssue, user, isNewUser);
  }

  @Transactional
  public AuthRefreshResult refresh(String refreshToken) {
    User user = refreshTokenService.getUser(refreshToken);
    RefreshTokenIssue refreshTokenIssue = refreshTokenService.rotate(refreshToken);
    String accessToken = jwtTokenProvider.createAccessToken(user.getId());
    return new AuthRefreshResult(accessToken, refreshTokenIssue);
  }

  @Transactional
  public void logout(String refreshToken) {
    if (refreshToken != null && !refreshToken.isBlank()) {
      refreshTokenService.revoke(refreshToken);
    }
  }

  public long accessTokenExpirationSeconds() {
    return jwtTokenProvider.accessTokenExpirationSeconds();
  }

  private User createKakaoUser(KakaoUserInfo kakaoUserInfo) {
    return userRepository.save(
        User.create(
            SocialProvider.KAKAO,
            kakaoUserInfo.socialId(),
            kakaoUserInfo.nickname(),
            kakaoUserInfo.profileImage()));
  }
}
