package com.eodigakka.domain.confirmedplace;

import java.time.Instant;

public record ConfirmedPlaceResponse(
    Long id,
    Long appointmentId,
    Long placeCandidateId,
    Long confirmedByUserId,
    Instant confirmedAt) {

  public static ConfirmedPlaceResponse from(ConfirmedPlace confirmedPlace) {
    return new ConfirmedPlaceResponse(
        confirmedPlace.getId(),
        confirmedPlace.getAppointmentId(),
        confirmedPlace.getPlaceCandidateId(),
        confirmedPlace.getConfirmedByUserId(),
        confirmedPlace.getConfirmedAt());
  }
}
