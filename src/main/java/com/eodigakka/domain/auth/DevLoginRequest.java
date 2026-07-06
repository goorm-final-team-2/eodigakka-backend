package com.eodigakka.domain.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record DevLoginRequest(
    @Schema(description = "테스트 사용자 식별자. 실제 저장값은 dev:{socialId} 형식입니다.", example = "frontend")
        @Size(max = 100)
        String socialId,
    @Schema(description = "테스트 사용자 닉네임", example = "프론트테스트") @Size(max = 50) String nickname,
    @Schema(description = "테스트 사용자 프로필 이미지 URL", example = "https://example.com/profile.png")
        @Size(max = 500)
        String profileImage) {}
