package com.eodigakka.domain.auth.kakao;

import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RestKakaoAuthClient implements KakaoAuthClient {

  private static final String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";

  private final KakaoProperties kakaoProperties;
  private final RestClient restClient;

  public RestKakaoAuthClient(
      KakaoProperties kakaoProperties, RestClient.Builder restClientBuilder) {
    this.kakaoProperties = kakaoProperties;
    this.restClient = restClientBuilder.build();
  }

  @Override
  public KakaoUserInfo getUserInfo(String code, String redirectUri) {
    try {
      String accessToken = requestAccessToken(code, redirectUri);
      JsonNode userInfo = requestUserInfo(accessToken);
      JsonNode profile = userInfo.path("kakao_account").path("profile");
      return new KakaoUserInfo(
          userInfo.path("id").asText(),
          profile.path("nickname").asText("카카오 사용자"),
          profile.path("profile_image_url").asText(null));
    } catch (RestClientException exception) {
      throw new BusinessException(ErrorCode.AUTH_KAKAO_FAILED);
    }
  }

  private String requestAccessToken(String code, String redirectUri) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", AUTHORIZATION_CODE_GRANT_TYPE);
    form.add("client_id", kakaoProperties.restApiKey());
    form.add("redirect_uri", redirectUri);
    form.add("code", code);
    if (StringUtils.hasText(kakaoProperties.clientSecret())) {
      form.add("client_secret", kakaoProperties.clientSecret());
    }

    JsonNode tokenResponse =
        restClient
            .post()
            .uri(kakaoProperties.tokenUri())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(JsonNode.class);
    if (tokenResponse == null
        || !StringUtils.hasText(tokenResponse.path("access_token").asText())) {
      throw new BusinessException(ErrorCode.AUTH_KAKAO_FAILED);
    }
    return tokenResponse.path("access_token").asText();
  }

  private JsonNode requestUserInfo(String accessToken) {
    JsonNode userInfo =
        restClient
            .get()
            .uri(kakaoProperties.userInfoUri())
            .headers(headers -> headers.setBearerAuth(accessToken))
            .retrieve()
            .body(JsonNode.class);
    if (userInfo == null || userInfo.path("id").isMissingNode()) {
      throw new BusinessException(ErrorCode.AUTH_KAKAO_FAILED);
    }
    return userInfo;
  }
}
