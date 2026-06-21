package com.eodigakka.domain.auth;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(@NotBlank String code, @NotBlank String redirectUri) {}
