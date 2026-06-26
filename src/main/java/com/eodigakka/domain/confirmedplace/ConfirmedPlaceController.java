package com.eodigakka.domain.confirmedplace;

import com.eodigakka.global.response.ApiResponse;
import com.eodigakka.global.security.AuthUser;
import com.eodigakka.global.security.GuestUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments/{appointmentId}/confirmed-place")
public class ConfirmedPlaceController {

  private final ConfirmedPlaceService confirmedPlaceService;

  public ConfirmedPlaceController(ConfirmedPlaceService confirmedPlaceService) {
    this.confirmedPlaceService = confirmedPlaceService;
  }

  @PutMapping
  public ApiResponse<ConfirmedPlaceResponse> confirm(
      @PathVariable Long appointmentId,
      @AuthenticationPrincipal AuthUser authUser,
      @Valid @RequestBody ConfirmedPlaceRequest request) {
    return ApiResponse.success(
        confirmedPlaceService.confirm(appointmentId, authUser.userId(), request));
  }

  @GetMapping
  public ApiResponse<ConfirmedPlaceResponse> find(
      @PathVariable Long appointmentId,
      @AuthenticationPrincipal AuthUser authUser,
      @AuthenticationPrincipal GuestUser guestUser) {
    return ApiResponse.success(
        confirmedPlaceService.find(appointmentId, authUser, guestSessionToken(guestUser)));
  }

  private String guestSessionToken(GuestUser guestUser) {
    return guestUser == null ? null : guestUser.sessionToken();
  }
}
