package com.eodigakka.domain.vote;

import com.eodigakka.global.response.ApiResponse;
import com.eodigakka.global.security.AuthUser;
import com.eodigakka.global.security.GuestUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP endpoints for appointment place-candidate voting.
 *
 * <p>Every operation is scoped to the current appointment member resolved from either an
 * authenticated user principal or a guest session principal.
 */
@Tag(name = "Votes", description = "약속 장소 후보 투표 API")
@RestController
@RequestMapping("/api/appointments/{appointmentId}/votes")
public class VoteController {

  private final VoteService voteService;

  public VoteController(VoteService voteService) {
    this.voteService = voteService;
  }

  /**
   * Creates or changes the current member's vote for one place candidate.
   *
   * <p>Submitting the same candidate again is idempotent and returns the existing vote.
   */
  @PutMapping
  @Operation(
      summary = "장소 후보 투표",
      description =
          """
          약속방 참여자 또는 게스트가 장소 후보에 투표합니다.

          참여자별로 약속방당 하나의 투표만 유지됩니다.
          이미 투표한 참여자가 다른 후보에 다시 투표하면 기존 투표의 후보가 변경됩니다.
          같은 후보로 다시 요청하면 기존 투표를 그대로 반환합니다.
          투표 생성과 변경은 약속방이 PLANNING 상태일 때만 가능합니다.
          """,
      responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "투표 생성 또는 변경 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "요청값이 올바르지 않거나 현재 상태에서 투표할 수 없음",
            content = @Content),
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
            description = "약속방 또는 장소 후보를 찾을 수 없음",
            content = @Content)
      })
  public ApiResponse<VoteResponse> vote(
      @Parameter(description = "약속방 ID", example = "10") @PathVariable Long appointmentId,
      @AuthenticationPrincipal AuthUser authUser,
      @AuthenticationPrincipal GuestUser guestUser,
      @Valid @RequestBody VoteRequest request) {
    return ApiResponse.success(
        voteService.vote(appointmentId, authUser, guestSessionToken(guestUser), request));
  }

  /**
   * Finds vote results for every place candidate in the appointment.
   *
   * <p>Results are sorted by vote count descending, then candidate creation time ascending, then
   * candidate id ascending.
   */
  @GetMapping("/results")
  @Operation(
      summary = "투표 결과 조회",
      description =
          """
          약속방 참여자 또는 게스트가 장소 후보별 투표 결과를 조회합니다.

          모든 장소 후보가 반환되며, 투표가 없는 후보는 `voteCount=0`으로 표시됩니다.
          결과는 `voteCount` 내림차순, 후보 생성 시각 오름차순, 후보 ID 오름차순으로 정렬됩니다.
          `votedByMe`는 현재 요청자의 투표 여부를 나타냅니다.
          """,
      responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "투표 결과 조회 성공"),
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
            description = "약속방을 찾을 수 없음",
            content = @Content)
      })
  public ApiResponse<List<VoteResultResponse>> findResults(
      @Parameter(description = "약속방 ID", example = "10") @PathVariable Long appointmentId,
      @AuthenticationPrincipal AuthUser authUser,
      @AuthenticationPrincipal GuestUser guestUser) {
    return ApiResponse.success(
        voteService.findResults(appointmentId, authUser, guestSessionToken(guestUser)));
  }

  /**
   * Deletes the current member's vote in the appointment.
   *
   * <p>If the current member has not voted, the operation succeeds without changing data.
   */
  @DeleteMapping
  @Operation(
      summary = "내 투표 취소",
      description =
          """
          약속방 참여자 또는 게스트가 자신의 투표를 취소합니다.

          삭제 대상은 현재 요청자의 투표로 한정됩니다.
          아직 투표하지 않은 참여자의 요청도 성공으로 처리됩니다.
          투표 취소는 약속방이 PLANNING 상태일 때만 가능합니다.
          """,
      responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "투표 취소 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "현재 상태에서 투표를 취소할 수 없음",
            content = @Content),
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
            description = "약속방을 찾을 수 없음",
            content = @Content)
      })
  public ApiResponse<Void> cancel(
      @Parameter(description = "약속방 ID", example = "10") @PathVariable Long appointmentId,
      @AuthenticationPrincipal AuthUser authUser,
      @AuthenticationPrincipal GuestUser guestUser) {
    voteService.cancel(appointmentId, authUser, guestSessionToken(guestUser));
    return ApiResponse.success();
  }

  private String guestSessionToken(GuestUser guestUser) {
    return guestUser == null ? null : guestUser.sessionToken();
  }
}
