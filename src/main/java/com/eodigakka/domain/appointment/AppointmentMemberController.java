package com.eodigakka.domain.appointment;

import com.eodigakka.global.response.ApiResponse;
import com.eodigakka.global.security.AuthUser;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments/{appointmentId}/members")
public class AppointmentMemberController {

  private static final String GUEST_TOKEN_HEADER = "X-Guest-Token";

  private final AppointmentMemberService appointmentMemberService;

  public AppointmentMemberController(AppointmentMemberService appointmentMemberService) {
    this.appointmentMemberService = appointmentMemberService;
  }

  @GetMapping
  public ApiResponse<List<AppointmentMemberResponse>> findAll(
      @PathVariable Long appointmentId,
      @AuthenticationPrincipal AuthUser authUser,
      @RequestHeader(name = GUEST_TOKEN_HEADER, required = false) String guestToken) {
    return ApiResponse.success(
        appointmentMemberService.findAll(appointmentId, authUser, guestToken));
  }
}
