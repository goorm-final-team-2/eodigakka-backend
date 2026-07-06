package com.eodigakka.domain.place;

import com.eodigakka.global.response.ApiResponse;
import com.eodigakka.global.security.AuthUser;
import com.eodigakka.global.security.GuestUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments/{appointmentId}/place-candidates")
public class PlaceCandidateController {

  private final PlaceCandidateService placeCandidateService;

  public PlaceCandidateController(PlaceCandidateService placeCandidateService) {
    this.placeCandidateService = placeCandidateService;
  }

  @PostMapping
  @Operation(
      summary = "장소 후보 등록",
      description =
          """
          약속방 참여자 또는 게스트가 장소 후보를 등록합니다.
          카카오 장소 검색 결과 item에서 `distance`를 제외한 필드를 전달해 약속 장소 후보로 등록합니다.
          장소 후보 등록은 약속방이 PLANNING 상태일 때만 가능합니다.
          응답의 `addedByMe`와 `deletable`은 현재 요청자 기준 상태입니다.
          """,
      responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "장소 후보 등록 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "등록 요청이 올바르지 않거나 이미 등록된 장소 후보",
            content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "약속방 참여자가 아님",
            content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "약속방을 찾을 수 없음",
            content = @Content)
      })
  public ResponseEntity<ApiResponse<PlaceCandidateResponse>> create(
      @PathVariable Long appointmentId,
      @AuthenticationPrincipal AuthUser authUser,
      @AuthenticationPrincipal GuestUser guestUser,
      @Valid @RequestBody PlaceCandidateCreateRequest request) {
    PlaceCandidateResponse response =
        placeCandidateService.create(
            appointmentId, authUser, guestSessionToken(guestUser), request);
    return ResponseEntity.created(
            URI.create("/api/appointments/" + appointmentId + "/place-candidates/" + response.id()))
        .body(ApiResponse.success(response));
  }

  @GetMapping
  @Operation(
      summary = "장소 후보 목록 조회",
      description =
          """
          약속방 참여자 또는 게스트가 약속 장소 후보 목록을 조회합니다.
          각 후보의 `addedByMe`와 `deletable`은 현재 요청자 기준 상태입니다.
          """)
  public ApiResponse<List<PlaceCandidateResponse>> findAll(
      @PathVariable Long appointmentId,
      @AuthenticationPrincipal AuthUser authUser,
      @AuthenticationPrincipal GuestUser guestUser) {
    return ApiResponse.success(
        placeCandidateService.findAll(appointmentId, authUser, guestSessionToken(guestUser)));
  }

  @DeleteMapping("/{placeCandidateId}")
  @Operation(
      summary = "장소 후보 삭제",
      description =
          """
          약속방 참여자 또는 게스트가 장소 후보를 삭제합니다.
          후보 등록자 또는 약속 호스트만 삭제할 수 있으며, 약속방이 PLANNING 상태일 때만 가능합니다.
          """)
  public ApiResponse<Void> delete(
      @PathVariable Long appointmentId,
      @PathVariable Long placeCandidateId,
      @AuthenticationPrincipal AuthUser authUser,
      @AuthenticationPrincipal GuestUser guestUser) {
    placeCandidateService.delete(
        appointmentId, placeCandidateId, authUser, guestSessionToken(guestUser));
    return ApiResponse.success();
  }

  private String guestSessionToken(GuestUser guestUser) {
    return guestUser == null ? null : guestUser.sessionToken();
  }
}
