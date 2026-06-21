package com.eodigakka.domain.vote;

import com.eodigakka.global.response.ApiResponse;
import com.eodigakka.global.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments/{appointmentId}/votes")
public class VoteController {

  private static final String GUEST_TOKEN_HEADER = "X-Guest-Token";

  private final VoteService voteService;

  public VoteController(VoteService voteService) {
    this.voteService = voteService;
  }

  @PutMapping
  public ApiResponse<VoteResponse> vote(
      @PathVariable Long appointmentId,
      @AuthenticationPrincipal AuthUser authUser,
      @RequestHeader(name = GUEST_TOKEN_HEADER, required = false) String guestToken,
      @Valid @RequestBody VoteRequest request) {
    return ApiResponse.success(voteService.vote(appointmentId, authUser, guestToken, request));
  }
}
