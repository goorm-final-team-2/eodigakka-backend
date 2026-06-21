package com.eodigakka.domain.confirmedplace;

import com.eodigakka.global.response.ApiResponse;
import com.eodigakka.global.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments/{appointmentId}/confirmed-place")
public class ConfirmedPlaceController {

  private static final String GUEST_TOKEN_HEADER = "X-Guest-Token";

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
      @RequestHeader(name = GUEST_TOKEN_HEADER, required = false) String guestToken) {
    return ApiResponse.success(confirmedPlaceService.find(appointmentId, authUser, guestToken));
  }
}
