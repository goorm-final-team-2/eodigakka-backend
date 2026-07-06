package com.eodigakka.global.security;

import com.eodigakka.domain.appointment.AppointmentMemberResolver;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class WebSocketSecurityInterceptor implements ChannelInterceptor {

  private static final Pattern LOCATION_DESTINATION_PATTERN =
      Pattern.compile("^/(?:topic|app)/appointments/(\\d+)/locations$");

  private final WebSocketAuthenticationService webSocketAuthenticationService;
  private final AppointmentMemberResolver appointmentMemberResolver;

  public WebSocketSecurityInterceptor(
      WebSocketAuthenticationService webSocketAuthenticationService,
      AppointmentMemberResolver appointmentMemberResolver) {
    this.webSocketAuthenticationService = webSocketAuthenticationService;
    this.appointmentMemberResolver = appointmentMemberResolver;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null) {
      accessor = StompHeaderAccessor.wrap(message);
    }
    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      Authentication authentication = webSocketAuthenticationService.authenticate(accessor);
      accessor.setUser(authentication);
      return message;
    }

    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())
        || StompCommand.SEND.equals(accessor.getCommand())) {
      validateLocationDestinationAccess(accessor);
    }

    return message;
  }

  private void validateLocationDestinationAccess(StompHeaderAccessor accessor) {
    String destination = accessor.getDestination();
    if (destination == null) {
      return;
    }

    Matcher matcher = LOCATION_DESTINATION_PATTERN.matcher(destination);
    if (!matcher.matches()) {
      return;
    }

    Long appointmentId = Long.valueOf(matcher.group(1));
    appointmentMemberResolver.resolve(
        appointmentId,
        webSocketAuthenticationService.authUser(accessor.getUser()),
        webSocketAuthenticationService.guestSessionToken(accessor.getUser()));
  }
}
