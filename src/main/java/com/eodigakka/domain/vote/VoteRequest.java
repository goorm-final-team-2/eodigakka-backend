package com.eodigakka.domain.vote;

import jakarta.validation.constraints.NotNull;

public record VoteRequest(@NotNull Long placeCandidateId) {}
