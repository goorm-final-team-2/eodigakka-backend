package com.eodigakka.domain.appointment;

import com.eodigakka.global.response.ApiResponse;
import com.eodigakka.global.security.AuthUser;
import com.eodigakka.global.security.GuestUser;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments/{appointmentId}/members")
public class AppointmentMemberController {

  private final AppointmentMemberService appointmentMemberService;
  private final GuestCookieService guestCookieService;

  public AppointmentMemberController(
      AppointmentMemberService appointmentMemberService, GuestCookieService guestCookieService) {
    this.appointmentMemberService = appointmentMemberService;
    this.guestCookieService = guestCookieService;
  }

  @GetMapping
  public ApiResponse<List<AppointmentMemberResponse>> findAll(
      @PathVariable Long appointmentId,
      @AuthenticationPrincipal AuthUser authUser,
      @AuthenticationPrincipal GuestUser guestUser) {
    return ApiResponse.success(
        appointmentMemberService.findAll(appointmentId, authUser, guestSessionToken(guestUser)));
  }

  @DeleteMapping("/me")
  public ResponseEntity<ApiResponse<Void>> leave(
      @PathVariable Long appointmentId,
      @AuthenticationPrincipal AuthUser authUser,
      @AuthenticationPrincipal GuestUser guestUser) {
    boolean guestLeft =
        appointmentMemberService.leave(appointmentId, authUser, guestSessionToken(guestUser));
    ResponseEntity.BodyBuilder response = ResponseEntity.ok();
    if (guestLeft) {
      response.header(HttpHeaders.SET_COOKIE, guestCookieService.clearCookie().toString());
    }
    return response.body(ApiResponse.success());
  }

  private String guestSessionToken(GuestUser guestUser) {
    return guestUser == null ? null : guestUser.sessionToken();
  }
}
