package com.eodigakka.domain.location;

import com.eodigakka.global.response.ApiResponse;
import com.eodigakka.global.security.AuthUser;
import com.eodigakka.global.security.GuestUser;
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
