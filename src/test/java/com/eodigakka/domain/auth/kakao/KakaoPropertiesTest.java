package com.eodigakka.domain.auth.kakao;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class KakaoPropertiesTest {

  @Test
  void allowsOnlyConfiguredRedirectUri() {
    KakaoProperties properties =
        new KakaoProperties(
            "rest-api-key",
            "",
            URI.create("https://kauth.kakao.com/oauth/token"),
            URI.create("https://kapi.kakao.com/v2/user/me"),
            URI.create("https://dapi.kakao.com/v2/local/search/keyword.json"),
            List.of("http://localhost:5173/oauth/kakao/callback"));

    assertThat(properties.isAllowedRedirectUri("http://localhost:5173/oauth/kakao/callback"))
        .isTrue();
    assertThat(properties.isAllowedRedirectUri("http://localhost:3000/oauth/kakao/callback"))
        .isFalse();
  }

  @Test
  void trimsConfiguredRedirectUrisBeforeComparison() {
    KakaoProperties properties =
        new KakaoProperties(
            "rest-api-key",
            "",
            URI.create("https://kauth.kakao.com/oauth/token"),
            URI.create("https://kapi.kakao.com/v2/user/me"),
            URI.create("https://dapi.kakao.com/v2/local/search/keyword.json"),
            List.of(" http://localhost:5173/oauth/kakao/callback "));

    assertThat(properties.isAllowedRedirectUri("http://localhost:5173/oauth/kakao/callback"))
        .isTrue();
  }
}
