package com.eodigakka.domain.auth.kakao;

public interface KakaoAuthClient {

  KakaoUserInfo getUserInfo(String code, String redirectUri);
}
