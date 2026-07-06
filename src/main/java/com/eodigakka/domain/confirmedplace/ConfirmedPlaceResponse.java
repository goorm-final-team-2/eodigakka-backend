package com.eodigakka.domain.confirmedplace;

import com.eodigakka.domain.place.PlaceCandidate;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Response for the appointment's confirmed place.
 *
 * <p>The first fields identify the confirmation event. The remaining place fields mirror the
 * selected {@link PlaceCandidate}, allowing clients to render the final place without an additional
 * candidate lookup.
 */
@Schema(description = "확정 장소 응답")
public record ConfirmedPlaceResponse(
    @Schema(description = "확정 장소 ID", example = "1") Long id,
    @Schema(description = "약속방 ID", example = "10") Long appointmentId,
    @Schema(description = "확정된 장소 후보 ID", example = "1000") Long placeCandidateId,
    @Schema(description = "확정한 호스트 사용자 ID", example = "1") Long confirmedByUserId,
    @Schema(description = "확정 시각", example = "2026-06-21T00:00:00Z") Instant confirmedAt,
    @Schema(description = "카카오 장소 ID", example = "26338954") String kakaoPlaceId,
    @Schema(description = "장소명", example = "강남역") String name,
    @Schema(description = "지번 주소", example = "서울 강남구 역삼동 858") String address,
    @Schema(description = "도로명 주소", example = "서울 강남구 강남대로 396") String roadAddress,
    @Schema(description = "카테고리", example = "교통,수송 > 지하철,전철 > 수도권2호선") String category,
    @Schema(description = "카카오 장소 URL", example = "https://place.map.kakao.com/26338954")
        String placeUrl,
    @Schema(description = "전화번호", example = "02-6110-2221") String phone,
    @Schema(description = "위도", example = "37.4979") double latitude,
    @Schema(description = "경도", example = "127.0276") double longitude) {

  /**
   * Creates a response from the confirmation row and its selected place candidate.
   *
   * @param confirmedPlace persisted confirmation metadata
   * @param placeCandidate place candidate referenced by {@code confirmedPlace.placeCandidateId}
   * @return response containing confirmation metadata and selected place details
   */
  public static ConfirmedPlaceResponse from(
      ConfirmedPlace confirmedPlace, PlaceCandidate placeCandidate) {
    return new ConfirmedPlaceResponse(
        confirmedPlace.getId(),
        confirmedPlace.getAppointmentId(),
        confirmedPlace.getPlaceCandidateId(),
        confirmedPlace.getConfirmedByUserId(),
        confirmedPlace.getConfirmedAt(),
        placeCandidate.getKakaoPlaceId(),
        placeCandidate.getName(),
        placeCandidate.getAddress(),
        placeCandidate.getRoadAddress(),
        placeCandidate.getCategory(),
        placeCandidate.getPlaceUrl(),
        placeCandidate.getPhone(),
        placeCandidate.getLatitude(),
        placeCandidate.getLongitude());
  }
}
