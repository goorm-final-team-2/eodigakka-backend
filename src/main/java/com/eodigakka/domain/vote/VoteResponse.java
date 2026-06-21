package com.eodigakka.domain.vote;

import java.time.Instant;

public record VoteResponse(
    Long id, Long appointmentId, Long placeCandidateId, Long memberId, Instant createdAt) {

  public static VoteResponse from(Vote vote) {
    return new VoteResponse(
        vote.getId(),
        vote.getAppointmentId(),
        vote.getPlaceCandidateId(),
        vote.getMemberId(),
        vote.getCreatedAt());
  }
}
