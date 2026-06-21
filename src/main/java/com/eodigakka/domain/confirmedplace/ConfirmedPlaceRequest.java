package com.eodigakka.domain.confirmedplace;

import jakarta.validation.constraints.NotNull;

public record ConfirmedPlaceRequest(@NotNull Long placeCandidateId) {}
