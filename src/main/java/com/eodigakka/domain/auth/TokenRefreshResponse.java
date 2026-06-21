package com.eodigakka.domain.auth;

public record TokenRefreshResponse(String accessToken, String tokenType, long expiresIn) {}
