package com.eodigakka.domain.location;

import java.time.Instant;

public record RedisMemberLocation(
    Long appointmentId,
    Long memberId,
    double latitude,
    double longitude,
    double accuracy,
    Instant updatedAt) {

  static RedisMemberLocation from(MemberLocationResponse response) {
    return new RedisMemberLocation(
        response.appointmentId(),
        response.memberId(),
        response.latitude(),
        response.longitude(),
        response.accuracy(),
        response.updatedAt());
  }

  MemberLocationResponse toResponse() {
    return new MemberLocationResponse(
        appointmentId, memberId, latitude, longitude, accuracy, updatedAt);
  }
}
