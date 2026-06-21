package com.eodigakka.domain.vote;

public record VoteResultResponse(Long placeCandidateId, long voteCount, boolean votedByMe) {}
