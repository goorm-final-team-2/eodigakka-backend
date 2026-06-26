package com.eodigakka.domain.place;

import com.eodigakka.global.response.ApiResponse;
import com.eodigakka.global.security.AuthUser;
import com.eodigakka.global.security.GuestUser;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments/{appointmentId}/place-candidates")
public class PlaceCandidateController {

  private final PlaceCandidateService placeCandidateService;

  public PlaceCandidateController(PlaceCandidateService placeCandidateService) {
    this.placeCandidateService = placeCandidateService;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<PlaceCandidateResponse>> create(
      @PathVariable Long appointmentId,
      @AuthenticationPrincipal AuthUser authUser,
      @AuthenticationPrincipal GuestUser guestUser,
      @Valid @RequestBody PlaceCandidateCreateRequest request) {
    PlaceCandidateResponse response =
        placeCandidateService.create(
            appointmentId, authUser, guestSessionToken(guestUser), request);
    return ResponseEntity.created(
            URI.create("/api/appointments/" + appointmentId + "/place-candidates/" + response.id()))
        .body(ApiResponse.success(response));
  }

  @GetMapping
  public ApiResponse<List<PlaceCandidateResponse>> findAll(
      @PathVariable Long appointmentId,
      @AuthenticationPrincipal AuthUser authUser,
      @AuthenticationPrincipal GuestUser guestUser) {
    return ApiResponse.success(
        placeCandidateService.findAll(appointmentId, authUser, guestSessionToken(guestUser)));
  }

  @DeleteMapping("/{placeCandidateId}")
  public ApiResponse<Void> delete(
      @PathVariable Long appointmentId,
      @PathVariable Long placeCandidateId,
      @AuthenticationPrincipal AuthUser authUser,
      @AuthenticationPrincipal GuestUser guestUser) {
    placeCandidateService.delete(
        appointmentId, placeCandidateId, authUser, guestSessionToken(guestUser));
    return ApiResponse.success();
  }

  private String guestSessionToken(GuestUser guestUser) {
    return guestUser == null ? null : guestUser.sessionToken();
  }
}
