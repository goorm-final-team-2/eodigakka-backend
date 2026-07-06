package com.eodigakka.domain.location;

import com.eodigakka.global.security.WebSocketAuthenticationService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class MemberLocationMessageController {

  private final MemberLocationService memberLocationService;
  private final WebSocketAuthenticationService webSocketAuthenticationService;
  private final SimpMessagingTemplate messagingTemplate;

  public MemberLocationMessageController(
      MemberLocationService memberLocationService,
      WebSocketAuthenticationService webSocketAuthenticationService,
      SimpMessagingTemplate messagingTemplate) {
    this.memberLocationService = memberLocationService;
    this.webSocketAuthenticationService = webSocketAuthenticationService;
    this.messagingTemplate = messagingTemplate;
  }

  @MessageMapping("/appointments/{appointmentId}/locations")
  public void updateMine(
      @DestinationVariable Long appointmentId,
      Principal principal,
      @Valid @Payload MemberLocationUpdateRequest request) {
    MemberLocationResponse response =
        memberLocationService.updateMine(
            appointmentId,
            webSocketAuthenticationService.authUser(principal),
            webSocketAuthenticationService.guestSessionToken(principal),
            request);

    messagingTemplate.convertAndSend(
        "/topic/appointments/%d/locations".formatted(appointmentId), response);
  }
}
