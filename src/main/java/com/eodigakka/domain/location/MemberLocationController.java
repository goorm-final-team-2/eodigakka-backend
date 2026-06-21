package com.eodigakka.domain.location;

import com.eodigakka.global.response.ApiResponse;
import com.eodigakka.global.security.AuthUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments/{appointmentId}/locations")
public class MemberLocationController {

  private static final String GUEST_TOKEN_HEADER = "X-Guest-Token";

  private final MemberLocationService memberLocationService;

  public MemberLocationController(MemberLocationService memberLocationService) {
    this.memberLocationService = memberLocationService;
  }

  @PutMapping("/me")
  public ApiResponse<MemberLocationResponse> updateMine(
      @PathVariable Long appointmentId,
      @AuthenticationPrincipal AuthUser authUser,
      @RequestHeader(name = GUEST_TOKEN_HEADER, required = false) String guestToken,
      @Valid @RequestBody MemberLocationUpdateRequest request) {
    return ApiResponse.success(
        memberLocationService.updateMine(appointmentId, authUser, guestToken, request));
  }

  @GetMapping
  public ApiResponse<List<MemberLocationResponse>> findAll(
      @PathVariable Long appointmentId,
      @AuthenticationPrincipal AuthUser authUser,
      @RequestHeader(name = GUEST_TOKEN_HEADER, required = false) String guestToken) {
    return ApiResponse.success(memberLocationService.findAll(appointmentId, authUser, guestToken));
  }
}
