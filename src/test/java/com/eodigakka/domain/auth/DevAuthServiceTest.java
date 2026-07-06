package com.eodigakka.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.eodigakka.domain.user.SocialProvider;
import com.eodigakka.domain.user.User;
import com.eodigakka.domain.user.UserRepository;
import com.eodigakka.global.security.JwtTokenProvider;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DevAuthServiceTest {

  private static final Instant REFRESH_EXPIRES_AT = Instant.parse("2026-07-04T00:00:00Z");

  @Mock private UserRepository userRepository;
  @Mock private JwtTokenProvider jwtTokenProvider;
  @Mock private RefreshTokenService refreshTokenService;

  @Test
  void loginCreatesDevUserAndIssuesTokens() {
    DevAuthService devAuthService =
        new DevAuthService(userRepository, jwtTokenProvider, refreshTokenService);
    given(userRepository.findBySocialProviderAndSocialId(SocialProvider.KAKAO, "dev:frontend"))
        .willReturn(Optional.empty());
    given(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
        .willAnswer(
            invocation -> {
              User user = invocation.getArgument(0);
              ReflectionTestUtils.setField(user, "id", 10L);
              return user;
            });
    given(jwtTokenProvider.createAccessToken(10L)).willReturn("access-token");
    given(refreshTokenService.issue(org.mockito.ArgumentMatchers.any(User.class)))
        .willReturn(new RefreshTokenIssue("refresh-token", REFRESH_EXPIRES_AT));

    AuthLoginResult result = devAuthService.login(new DevLoginRequest(" frontend ", " 프론트 ", null));

    assertThat(result.accessToken()).isEqualTo("access-token");
    assertThat(result.refreshTokenIssue().token()).isEqualTo("refresh-token");
    assertThat(result.isNewUser()).isTrue();
    assertThat(result.user().getNickname()).isEqualTo("프론트");

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    assertThat(userCaptor.getValue().getSocialProvider()).isEqualTo(SocialProvider.KAKAO);
    assertThat(userCaptor.getValue().getSocialId()).isEqualTo("dev:frontend");
  }

  @Test
  void loginReusesExistingDevUser() {
    DevAuthService devAuthService =
        new DevAuthService(userRepository, jwtTokenProvider, refreshTokenService);
    User user = User.create(SocialProvider.KAKAO, "dev:default", "기존", null);
    ReflectionTestUtils.setField(user, "id", 20L);
    given(userRepository.findBySocialProviderAndSocialId(SocialProvider.KAKAO, "dev:default"))
        .willReturn(Optional.of(user));
    given(jwtTokenProvider.createAccessToken(20L)).willReturn("access-token");
    given(refreshTokenService.issue(user))
        .willReturn(new RefreshTokenIssue("refresh-token", REFRESH_EXPIRES_AT));

    AuthLoginResult result = devAuthService.login(null);

    assertThat(result.isNewUser()).isFalse();
    assertThat(result.user().getId()).isEqualTo(20L);
    assertThat(result.user().getNickname()).isEqualTo("테스트 사용자");
  }
}
