package com.eodigakka.domain.auth.kakao;

import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth.kakao")
public record KakaoProperties(
    String restApiKey,
    String clientSecret,
    URI tokenUri,
    URI userInfoUri,
    URI localKeywordSearchUri,
    List<String> allowedRedirectUris) {

  public boolean isAllowedRedirectUri(String redirectUri) {
    return allowedRedirectUris.stream().map(String::trim).anyMatch(redirectUri::equals);
  }
}
