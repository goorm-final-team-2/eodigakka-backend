package com.eodigakka.domain.auth;

import com.eodigakka.domain.user.SocialProvider;
import com.eodigakka.domain.user.User;
import com.eodigakka.domain.user.UserRepository;
import com.eodigakka.global.security.JwtTokenProvider;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Profile({"local", "dev"})
@Service
public class DevAuthService {

  private static final String DEFAULT_SOCIAL_ID = "default";
  private static final String DEFAULT_NICKNAME = "테스트 사용자";
  private static final String DEV_SOCIAL_ID_PREFIX = "dev:";

  private final UserRepository userRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenService refreshTokenService;

  public DevAuthService(
      UserRepository userRepository,
      JwtTokenProvider jwtTokenProvider,
      RefreshTokenService refreshTokenService) {
    this.userRepository = userRepository;
    this.jwtTokenProvider = jwtTokenProvider;
    this.refreshTokenService = refreshTokenService;
  }

  @Transactional
  public AuthLoginResult login(DevLoginRequest request) {
    String socialId =
        DEV_SOCIAL_ID_PREFIX
            + normalize(request == null ? null : request.socialId(), DEFAULT_SOCIAL_ID);
    String nickname = normalize(request == null ? null : request.nickname(), DEFAULT_NICKNAME);
    String profileImage = request == null ? null : normalizeNullable(request.profileImage());

    Optional<User> foundUser =
        userRepository.findBySocialProviderAndSocialId(SocialProvider.KAKAO, socialId);
    boolean isNewUser = foundUser.isEmpty();
    User user =
        foundUser.orElseGet(
            () ->
                userRepository.save(
                    User.create(SocialProvider.KAKAO, socialId, nickname, profileImage)));
    user.updateProfile(nickname, profileImage);

    String accessToken = jwtTokenProvider.createAccessToken(user.getId());
    RefreshTokenIssue refreshTokenIssue = refreshTokenService.issue(user);
    return new AuthLoginResult(accessToken, refreshTokenIssue, user, isNewUser);
  }

  public long accessTokenExpirationSeconds() {
    return jwtTokenProvider.accessTokenExpirationSeconds();
  }

  private String normalize(String value, String defaultValue) {
    String normalized = normalizeNullable(value);
    return normalized == null ? defaultValue : normalized;
  }

  private String normalizeNullable(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
