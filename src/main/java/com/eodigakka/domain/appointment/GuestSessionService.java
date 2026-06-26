package com.eodigakka.domain.appointment;

import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import com.eodigakka.global.security.MessageDigestSupport;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuestSessionService {

  private final GuestSessionRepository guestSessionRepository;
  private final GuestTokenGenerator guestTokenGenerator;
  private final GuestCookieProperties guestCookieProperties;
  private final Clock clock;

  @Autowired
  public GuestSessionService(
      GuestSessionRepository guestSessionRepository,
      GuestTokenGenerator guestTokenGenerator,
      GuestCookieProperties guestCookieProperties) {
    this(guestSessionRepository, guestTokenGenerator, guestCookieProperties, Clock.systemUTC());
  }

  GuestSessionService(
      GuestSessionRepository guestSessionRepository,
      GuestTokenGenerator guestTokenGenerator,
      GuestCookieProperties guestCookieProperties,
      Clock clock) {
    this.guestSessionRepository = guestSessionRepository;
    this.guestTokenGenerator = guestTokenGenerator;
    this.guestCookieProperties = guestCookieProperties;
    this.clock = clock;
  }

  @Transactional
  public GuestSessionIssue issue(AppointmentMember appointmentMember) {
    Instant now = Instant.now(clock);
    Instant expiresAt = now.plus(Duration.ofDays(guestCookieProperties.maxAgeDays()));
    String token = guestTokenGenerator.generate();
    guestSessionRepository.save(
        GuestSession.create(
            appointmentMember, MessageDigestSupport.sha256Hex(token), expiresAt, now));
    return new GuestSessionIssue(token, expiresAt);
  }

  @Transactional
  public AppointmentMember resolve(Long appointmentId, String sessionToken) {
    GuestSession guestSession =
        guestSessionRepository
            .findByTokenHash(MessageDigestSupport.sha256Hex(sessionToken))
            .orElseThrow(() -> new BusinessException(ErrorCode.GUEST_SESSION_INVALID));

    Instant now = Instant.now(clock);
    if (!guestSession.isValid(now)) {
      throw new BusinessException(ErrorCode.GUEST_SESSION_EXPIRED);
    }
    if (!guestSession.getAppointmentMember().getAppointmentId().equals(appointmentId)) {
      throw new BusinessException(ErrorCode.GUEST_SESSION_INVALID);
    }

    guestSession.recordUsedAt(now);
    return guestSession.getAppointmentMember();
  }
}
