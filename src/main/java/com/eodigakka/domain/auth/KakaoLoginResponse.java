package com.eodigakka.domain.auth;

import com.eodigakka.domain.user.UserResponse;

public record KakaoLoginResponse(
    String accessToken, String tokenType, long expiresIn, UserResponse user, boolean isNewUser) {}
