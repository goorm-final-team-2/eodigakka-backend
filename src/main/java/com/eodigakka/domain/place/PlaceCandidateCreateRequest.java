package com.eodigakka.domain.place;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlaceCandidateCreateRequest(
    @NotBlank @Size(max = 255) String kakaoPlaceId,
    @NotBlank @Size(max = 255) String name,
    @NotBlank @Size(max = 500) String address,
    @Size(max = 500) String roadAddress,
    @Size(max = 255) String category,
    @Size(max = 500) String placeUrl,
    @Size(max = 50) String phone,
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude) {}
