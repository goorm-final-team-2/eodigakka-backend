package com.eodigakka.global.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.eodigakka.domain.appointment.AppointmentMemberResolver;
import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class WebSocketSecurityInterceptorTest {

  private static final Long APPOINTMENT_ID = 10L;

  @Mock private WebSocketAuthenticationService webSocketAuthenticationService;
  @Mock private AppointmentMemberResolver appointmentMemberResolver;

  @Test
  void subscribeToLocationTopicRequiresAppointmentMembership() {
    WebSocketSecurityInterceptor interceptor =
        new WebSocketSecurityInterceptor(webSocketAuthenticationService, appointmentMemberResolver);
    UsernamePasswordAuthenticationToken principal =
        new UsernamePasswordAuthenticationToken(new AuthUser(1L), null);
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination("/topic/appointments/10/locations");
    accessor.setUser(principal);
    given(webSocketAuthenticationService.authUser(principal)).willReturn(new AuthUser(1L));

    interceptor.preSend(message(accessor), null);

    verify(appointmentMemberResolver).resolve(APPOINTMENT_ID, new AuthUser(1L), null);
  }

  @Test
  void subscribeToLocationTopicRejectsNonParticipant() {
    WebSocketSecurityInterceptor interceptor =
        new WebSocketSecurityInterceptor(webSocketAuthenticationService, appointmentMemberResolver);
    UsernamePasswordAuthenticationToken principal =
        new UsernamePasswordAuthenticationToken(new AuthUser(1L), null);
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination("/topic/appointments/10/locations");
    accessor.setUser(principal);
    given(webSocketAuthenticationService.authUser(principal)).willReturn(new AuthUser(1L));
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, new AuthUser(1L), null))
        .willThrow(new BusinessException(ErrorCode.APPOINTMENT_MEMBER_NOT_FOUND));

    assertThatThrownBy(() -> interceptor.preSend(message(accessor), null))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void sendToLocationApplicationDestinationRequiresAppointmentMembership() {
    WebSocketSecurityInterceptor interceptor =
        new WebSocketSecurityInterceptor(webSocketAuthenticationService, appointmentMemberResolver);
    GuestAuthenticationToken principal = new GuestAuthenticationToken(new GuestUser("guest-token"));
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
    accessor.setDestination("/app/appointments/10/locations");
    accessor.setUser(principal);
    given(webSocketAuthenticationService.guestSessionToken(principal)).willReturn("guest-token");

    interceptor.preSend(message(accessor), null);

    verify(appointmentMemberResolver).resolve(APPOINTMENT_ID, null, "guest-token");
  }

  private Message<byte[]> message(StompHeaderAccessor accessor) {
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }
}
