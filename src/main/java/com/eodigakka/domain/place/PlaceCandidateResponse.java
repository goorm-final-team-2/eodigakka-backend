package com.eodigakka.domain.place;

import com.eodigakka.domain.appointment.AppointmentMember;
import java.time.Instant;

public record PlaceCandidateResponse(
    Long id,
    Long appointmentId,
    String kakaoPlaceId,
    String name,
    String address,
    String roadAddress,
    String category,
    String placeUrl,
    String phone,
    double latitude,
    double longitude,
    Long addedByMemberId,
    boolean addedByMe,
    boolean deletable,
    Instant createdAt) {

  public static PlaceCandidateResponse from(PlaceCandidate placeCandidate) {
    return from(placeCandidate, false, false);
  }

  public static PlaceCandidateResponse from(
      PlaceCandidate placeCandidate, AppointmentMember appointmentMember) {
    boolean addedByMe = placeCandidate.isAddedBy(appointmentMember.getId());
    boolean deletable = appointmentMember.isHost() || addedByMe;
    return from(placeCandidate, addedByMe, deletable);
  }

  private static PlaceCandidateResponse from(
      PlaceCandidate placeCandidate, boolean addedByMe, boolean deletable) {
    return new PlaceCandidateResponse(
        placeCandidate.getId(),
        placeCandidate.getAppointmentId(),
        placeCandidate.getKakaoPlaceId(),
        placeCandidate.getName(),
        placeCandidate.getAddress(),
        placeCandidate.getRoadAddress(),
        placeCandidate.getCategory(),
        placeCandidate.getPlaceUrl(),
        placeCandidate.getPhone(),
        placeCandidate.getLatitude(),
        placeCandidate.getLongitude(),
        placeCandidate.getAddedByMemberId(),
        addedByMe,
        deletable,
        placeCandidate.getCreatedAt());
  }
}
