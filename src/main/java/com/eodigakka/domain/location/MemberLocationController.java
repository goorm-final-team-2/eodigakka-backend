package com.eodigakka.domain.location;

import com.eodigakka.global.response.ApiResponse;
import com.eodigakka.global.security.AuthUser;
import com.eodigakka.global.security.GuestUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments/{appointmentId}/locations")
public class MemberLocationController {

  private final MemberLocationService memberLocationService;

  public MemberLocationController(MemberLocationService memberLocationService) {
    this.memberLocationService = memberLocationService;
  }

  @PutMapping("/me")
  @Operation(
      summary = "내 위치 공유/갱신",
      description =
          """
          약속방 참여자 또는 게스트가 자신의 최신 위치를 공유하거나 갱신합니다.
          위치 공유는 약속방이 CONFIRMED 상태일 때만 가능합니다.
          실시간 위치 공유는 WebSocket/STOMP를 우선 사용하며, 이 HTTP API는 연결 실패 시 fallback 또는 수동 갱신 용도로 사용할 수 있습니다.
          """)
  public ApiResponse<MemberLocationResponse> updateMine(
      @PathVariable Long appointmentId,
      @AuthenticationPrincipal AuthUser authUser,
      @AuthenticationPrincipal GuestUser guestUser,
      @Valid @RequestBody MemberLocationUpdateRequest request) {
    return ApiResponse.success(
        memberLocationService.updateMine(
            appointmentId, authUser, guestSessionToken(guestUser), request));
  }

  @GetMapping
  @Operation(
      summary = "참여자 위치 목록 조회",
      description =
          """
          약속방 참여자 또는 게스트가 참여자들의 최신 위치 목록을 조회합니다.
          위치 조회는 약속방이 CONFIRMED 상태일 때만 가능합니다.
          Redis의 최신 위치를 우선 조회하고, Redis 데이터가 없거나 조회할 수 없으면 DB의 마지막 위치 정보를 fallback으로 반환합니다.
          """)
  public ApiResponse<List<MemberLocationResponse>> findAll(
      @PathVariable Long appointmentId,
      @AuthenticationPrincipal AuthUser authUser,
      @AuthenticationPrincipal GuestUser guestUser) {
    return ApiResponse.success(
        memberLocationService.findAll(appointmentId, authUser, guestSessionToken(guestUser)));
  }

  private String guestSessionToken(GuestUser guestUser) {
    return guestUser == null ? null : guestUser.sessionToken();
  }
}
