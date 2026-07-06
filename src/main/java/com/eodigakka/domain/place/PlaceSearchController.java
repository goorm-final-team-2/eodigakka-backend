package com.eodigakka.domain.place;

import com.eodigakka.global.response.ApiResponse;
import com.eodigakka.global.security.AuthUser;
import com.eodigakka.global.security.GuestUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HTTP endpoint for appointment-scoped Kakao place search. */
@Tag(name = "Place Search", description = "약속 장소 검색 API")
@RestController
@RequestMapping("/api/appointments/{appointmentId}/places/search")
public class PlaceSearchController {

  private final PlaceSearchService placeSearchService;

  public PlaceSearchController(PlaceSearchService placeSearchService) {
    this.placeSearchService = placeSearchService;
  }

  @GetMapping
  @Operation(
      summary = "카카오 장소 검색",
      description =
          """
          약속방 참여자 또는 게스트가 카카오 Local API를 통해 장소를 검색합니다.

          검색 결과는 저장되지 않으며, 프론트에서 선택한 장소를 장소 후보 등록 API에 전달할 때 사용할 수 있는 형태로 반환됩니다.
          `sort=distance` 또는 `radius`를 사용할 때는 `x`와 `y`를 함께 전달해야 합니다.
          """,
      responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "장소 검색 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "검색 조건이 올바르지 않음",
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
            content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "502",
            description = "카카오 장소 검색 실패",
            content = @Content)
      })
  public ApiResponse<PlaceSearchResponse> search(
      @Parameter(description = "약속방 ID", example = "10") @PathVariable Long appointmentId,
      @AuthenticationPrincipal AuthUser authUser,
      @AuthenticationPrincipal GuestUser guestUser,
      @Parameter(description = "검색 키워드", example = "강남역") @RequestParam String query,
      @Parameter(description = "중심 좌표 경도", example = "127.0276") @RequestParam(required = false)
          Double x,
      @Parameter(description = "중심 좌표 위도", example = "37.4979") @RequestParam(required = false)
          Double y,
      @Parameter(description = "중심 좌표 기준 검색 반경(m), 최대 20000", example = "20000")
          @RequestParam(required = false)
          Integer radius,
      @Parameter(description = "결과 페이지 번호, 1~45", example = "1") @RequestParam(required = false)
          Integer page,
      @Parameter(description = "페이지당 결과 수, 1~15", example = "15") @RequestParam(required = false)
          Integer size,
      @Parameter(description = "정렬 방식: accuracy 또는 distance", example = "distance")
          @RequestParam(required = false)
          String sort,
      @Parameter(description = "카카오 카테고리 그룹 코드", example = "FD6") @RequestParam(required = false)
          String categoryGroupCode) {
    PlaceSearchRequest request =
        PlaceSearchRequest.of(query, x, y, radius, page, size, sort, categoryGroupCode);
    return ApiResponse.success(
        placeSearchService.search(appointmentId, authUser, guestSessionToken(guestUser), request));
  }

  private String guestSessionToken(GuestUser guestUser) {
    return guestUser == null ? null : guestUser.sessionToken();
  }
}
