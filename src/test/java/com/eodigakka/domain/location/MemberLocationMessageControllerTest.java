package com.eodigakka.domain.location;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.eodigakka.global.security.AuthUser;
import com.eodigakka.global.security.WebSocketAuthenticationService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class MemberLocationMessageControllerTest {

  private static final Long APPOINTMENT_ID = 10L;

  @Mock private MemberLocationService memberLocationService;
  @Mock private WebSocketAuthenticationService webSocketAuthenticationService;
  @Mock private SimpMessagingTemplate messagingTemplate;

  @Test
  void updateMineStoresLocationAndBroadcastsEventToAppointmentTopic() {
    MemberLocationMessageController controller =
        new MemberLocationMessageController(
            memberLocationService, webSocketAuthenticationService, messagingTemplate);
    UsernamePasswordAuthenticationToken principal =
        new UsernamePasswordAuthenticationToken(new AuthUser(1L), null);
    AuthUser authUser = new AuthUser(1L);
    MemberLocationUpdateRequest request = new MemberLocationUpdateRequest(37.5, 127.1, 10.0);
    MemberLocationResponse response =
        new MemberLocationResponse(
            APPOINTMENT_ID, 100L, 37.5, 127.1, 10.0, Instant.parse("2026-06-21T00:00:00Z"));
    given(webSocketAuthenticationService.authUser(principal)).willReturn(authUser);
    given(memberLocationService.updateMine(APPOINTMENT_ID, authUser, null, request))
        .willReturn(response);

    controller.updateMine(APPOINTMENT_ID, principal, request);

    verify(messagingTemplate).convertAndSend("/topic/appointments/10/locations", response);
  }
}
