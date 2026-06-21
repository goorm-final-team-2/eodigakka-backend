package com.eodigakka.domain.place;

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
    Instant createdAt) {

  public static PlaceCandidateResponse from(PlaceCandidate placeCandidate) {
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
        placeCandidate.getCreatedAt());
  }
}
