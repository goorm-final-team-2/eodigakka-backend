package com.eodigakka.domain.confirmedplace;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "확정 장소 선택 요청")
public record ConfirmedPlaceRequest(
    @Schema(description = "확정할 장소 후보 ID", example = "1000") @NotNull Long placeCandidateId) {}
