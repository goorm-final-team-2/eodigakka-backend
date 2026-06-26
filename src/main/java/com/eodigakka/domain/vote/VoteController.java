package com.eodigakka.domain.vote;

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
@RequestMapping("/api/appointments/{appointmentId}/votes")
public class VoteController {

  private final VoteService voteService;

  public VoteController(VoteService voteService) {
    this.voteService = voteService;
  }

  @PutMapping
  public ApiResponse<VoteResponse> vote(
      @PathVariable Long appointmentId,
      @AuthenticationPrincipal AuthUser authUser,
      @AuthenticationPrincipal GuestUser guestUser,
      @Valid @RequestBody VoteRequest request) {
    return ApiResponse.success(
        voteService.vote(appointmentId, authUser, guestSessionToken(guestUser), request));
  }

  @GetMapping("/results")
  public ApiResponse<List<VoteResultResponse>> findResults(
      @PathVariable Long appointmentId,
      @AuthenticationPrincipal AuthUser authUser,
      @AuthenticationPrincipal GuestUser guestUser) {
    return ApiResponse.success(
        voteService.findResults(appointmentId, authUser, guestSessionToken(guestUser)));
  }

  private String guestSessionToken(GuestUser guestUser) {
    return guestUser == null ? null : guestUser.sessionToken();
  }
}
