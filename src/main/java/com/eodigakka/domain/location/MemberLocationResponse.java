package com.eodigakka.domain.location;

import java.time.Instant;

public record MemberLocationResponse(
    Long appointmentId,
    Long memberId,
    double latitude,
    double longitude,
    double accuracy,
    Instant updatedAt) {

  public static MemberLocationResponse from(MemberLocation memberLocation) {
    return new MemberLocationResponse(
        memberLocation.getAppointmentId(),
        memberLocation.getMemberId(),
        memberLocation.getLatitude(),
        memberLocation.getLongitude(),
        memberLocation.getAccuracy(),
        memberLocation.getUpdatedAt());
  }
}
