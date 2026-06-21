package com.eodigakka.domain.appointment;

import com.eodigakka.global.response.ApiResponse;
import com.eodigakka.global.security.AuthUser;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

  private final AppointmentService appointmentService;

  public AppointmentController(AppointmentService appointmentService) {
    this.appointmentService = appointmentService;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<AppointmentResponse>> create(
      @AuthenticationPrincipal AuthUser authUser,
      @Valid @RequestBody AppointmentCreateRequest request) {
    AppointmentResponse response = appointmentService.create(authUser.userId(), request);
    return ResponseEntity.created(URI.create("/api/appointments/" + response.id()))
        .body(ApiResponse.success(response));
  }

  @GetMapping
  public ApiResponse<List<AppointmentResponse>> findMyAppointments(
      @AuthenticationPrincipal AuthUser authUser) {
    return ApiResponse.success(appointmentService.findMyAppointments(authUser.userId()));
  }

  @GetMapping("/invite/{inviteCode}")
  public ApiResponse<AppointmentInvitePreviewResponse> findInvitePreview(
      @PathVariable String inviteCode) {
    return ApiResponse.success(appointmentService.findInvitePreview(inviteCode));
  }

  @PostMapping("/guests")
  public ApiResponse<GuestJoinResponse> joinAsGuest(@Valid @RequestBody GuestJoinRequest request) {
    return ApiResponse.success(appointmentService.joinAsGuest(request));
  }

  @GetMapping("/{appointmentId}")
  public ApiResponse<AppointmentResponse> findById(
      @AuthenticationPrincipal AuthUser authUser, @PathVariable Long appointmentId) {
    return ApiResponse.success(appointmentService.findById(appointmentId, authUser.userId()));
  }

  @PostMapping("/join")
  public ApiResponse<AppointmentResponse> join(
      @AuthenticationPrincipal AuthUser authUser,
      @Valid @RequestBody AppointmentJoinRequest request) {
    return ApiResponse.success(appointmentService.join(authUser.userId(), request));
  }

  @PatchMapping("/{appointmentId}")
  public ApiResponse<AppointmentResponse> update(
      @AuthenticationPrincipal AuthUser authUser,
      @PathVariable Long appointmentId,
      @Valid @RequestBody AppointmentUpdateRequest request) {
    return ApiResponse.success(
        appointmentService.update(appointmentId, authUser.userId(), request));
  }

  @DeleteMapping("/{appointmentId}")
  public ApiResponse<Void> delete(
      @AuthenticationPrincipal AuthUser authUser, @PathVariable Long appointmentId) {
    appointmentService.delete(appointmentId, authUser.userId());
    return ApiResponse.success();
  }
}
