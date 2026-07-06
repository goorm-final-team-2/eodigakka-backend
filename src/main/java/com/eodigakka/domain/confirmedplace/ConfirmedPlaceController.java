package com.eodigakka.domain.confirmedplace;

import com.eodigakka.global.response.ApiResponse;
import com.eodigakka.global.security.AuthUser;
import com.eodigakka.global.security.GuestUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP endpoints for selecting and reading an appointment's confirmed place.
 *
 * <p>Selection is host-only and allowed only while the appointment is in {@code PLANNING}. After a
 * place is confirmed, the appointment moves to {@code CONFIRMED}; place candidates and votes become
 * immutable while the confirmed place remains readable by appointment members and guests.
 */
@Tag(name = "Confirmed Places", description = "약속 확정 장소 API")
@RestController
@RequestMapping("/api/appointments/{appointmentId}/confirmed-place")
public class ConfirmedPlaceController {

  private final ConfirmedPlaceService confirmedPlaceService;

  public ConfirmedPlaceController(ConfirmedPlaceService confirmedPlaceService) {
    this.confirmedPlaceService = confirmedPlaceService;
  }

  /**
   * Confirms one place candidate as the appointment's final place.
   *
   * <p>The response includes both confirmation metadata and the selected candidate's place details.
   */
  @PutMapping
  @Operation(
      summary = "확정 장소 선택",
      description =
          """
          약속 호스트가 장소 후보 하나를 확정 장소로 선택합니다.

          요청은 약속방이 PLANNING 상태일 때만 가능하며, 성공하면 약속방 상태가 CONFIRMED로 변경됩니다.
          확정 이후 장소 후보 등록/삭제와 투표 생성/변경/취소는 불가능합니다.
          응답에는 확정 메타데이터와 선택된 장소 후보의 카카오 장소 정보, 주소, 좌표가 함께 포함됩니다.
          """,
      responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "확정 장소 선택 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "요청값이 올바르지 않거나 현재 상태에서 확정할 수 없음",
            content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증이 필요함",
            content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "약속방 호스트 권한이 없음",
            content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "약속방 또는 장소 후보를 찾을 수 없음",
            content = @Content)
      })
  public ApiResponse<ConfirmedPlaceResponse> confirm(
      @Parameter(description = "약속방 ID", example = "10") @PathVariable Long appointmentId,
      @AuthenticationPrincipal AuthUser authUser,
      @Valid @RequestBody ConfirmedPlaceRequest request) {
    return ApiResponse.success(
        confirmedPlaceService.confirm(appointmentId, authUser.userId(), request));
  }

  /**
   * Finds the appointment's confirmed place.
   *
   * <p>Authenticated members and guest members can read the confirmed place after it exists.
   */
  @GetMapping
  @Operation(
      summary = "확정 장소 조회",
      description =
          """
          약속방 참여자 또는 게스트가 확정 장소를 조회합니다.

          응답에는 확정 메타데이터와 선택된 장소 후보의 카카오 장소 정보, 주소, 좌표가 함께 포함됩니다.
          확정 장소가 아직 없으면 CONFIRMED_PLACE_NOT_FOUND로 실패합니다.
          """,
      responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "확정 장소 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증이 필요함",
            content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "약속방 참여자가 아님",
            content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "약속방 또는 확정 장소를 찾을 수 없음",
            content = @Content)
      })
  public ApiResponse<ConfirmedPlaceResponse> find(
      @Parameter(description = "약속방 ID", example = "10") @PathVariable Long appointmentId,
      @AuthenticationPrincipal AuthUser authUser,
      @AuthenticationPrincipal GuestUser guestUser) {
    return ApiResponse.success(
        confirmedPlaceService.find(appointmentId, authUser, guestSessionToken(guestUser)));
  }

  private String guestSessionToken(GuestUser guestUser) {
    return guestUser == null ? null : guestUser.sessionToken();
  }
}
