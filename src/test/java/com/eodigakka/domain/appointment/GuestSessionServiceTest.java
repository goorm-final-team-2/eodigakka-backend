package com.eodigakka.domain.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.security.MessageDigestSupport;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GuestSessionServiceTest {

  private static final Long APPOINTMENT_ID = 10L;
  private static final Long OTHER_APPOINTMENT_ID = 20L;
  private static final String SESSION_TOKEN = "guest-session-token";
  private static final Instant NOW = Instant.parse("2026-06-26T00:00:00Z");

  @Mock private GuestSessionRepository guestSessionRepository;
  @Mock private GuestTokenGenerator guestTokenGenerator;

  private GuestSessionService guestSessionService;

  @BeforeEach
  void setUp() {
    guestSessionService =
        new GuestSessionService(
            guestSessionRepository,
            guestTokenGenerator,
            new GuestCookieProperties("guestSession", "/", false, "Lax", 30),
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void issueSavesGuestSessionAndReturnsSessionToken() {
    AppointmentMember appointmentMember = guestMember(APPOINTMENT_ID);
    given(guestTokenGenerator.generate()).willReturn(SESSION_TOKEN);

    GuestSessionIssue issue = guestSessionService.issue(appointmentMember);

    assertThat(issue.token()).isEqualTo(SESSION_TOKEN);
    assertThat(issue.expiresAt()).isEqualTo(NOW.plusSeconds(30L * 24 * 60 * 60));

    ArgumentCaptor<GuestSession> sessionCaptor = ArgumentCaptor.forClass(GuestSession.class);
    verify(guestSessionRepository).save(sessionCaptor.capture());
    GuestSession guestSession = sessionCaptor.getValue();
    assertThat(guestSession.getAppointmentMember()).isSameAs(appointmentMember);
  }

  @Test
  void resolveReturnsAppointmentMemberWhenGuestSessionIsValid() {
    AppointmentMember appointmentMember = guestMember(APPOINTMENT_ID);
    GuestSession guestSession =
        GuestSession.create(
            appointmentMember,
            MessageDigestSupport.sha256Hex(SESSION_TOKEN),
            NOW.plusSeconds(3600),
            NOW);
    given(guestSessionRepository.findByTokenHash(MessageDigestSupport.sha256Hex(SESSION_TOKEN)))
        .willReturn(Optional.of(guestSession));

    AppointmentMember result = guestSessionService.resolve(APPOINTMENT_ID, SESSION_TOKEN);

    assertThat(result).isSameAs(appointmentMember);
  }

  @Test
  void resolveThrowsBusinessExceptionWhenGuestSessionExpired() {
    AppointmentMember appointmentMember = guestMember(APPOINTMENT_ID);
    GuestSession guestSession =
        GuestSession.create(
            appointmentMember,
            MessageDigestSupport.sha256Hex(SESSION_TOKEN),
            NOW.minusSeconds(1),
            NOW.minusSeconds(3600));
    given(guestSessionRepository.findByTokenHash(MessageDigestSupport.sha256Hex(SESSION_TOKEN)))
        .willReturn(Optional.of(guestSession));

    assertThatThrownBy(() -> guestSessionService.resolve(APPOINTMENT_ID, SESSION_TOKEN))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void resolveThrowsBusinessExceptionWhenSessionBelongsToOtherAppointment() {
    AppointmentMember appointmentMember = guestMember(OTHER_APPOINTMENT_ID);
    GuestSession guestSession =
        GuestSession.create(
            appointmentMember,
            MessageDigestSupport.sha256Hex(SESSION_TOKEN),
            NOW.plusSeconds(3600),
            NOW);
    given(guestSessionRepository.findByTokenHash(MessageDigestSupport.sha256Hex(SESSION_TOKEN)))
        .willReturn(Optional.of(guestSession));

    assertThatThrownBy(() -> guestSessionService.resolve(APPOINTMENT_ID, SESSION_TOKEN))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void resolveThrowsBusinessExceptionWhenGuestSessionDoesNotExist() {
    given(guestSessionRepository.findByTokenHash(any())).willReturn(Optional.empty());

    assertThatThrownBy(() -> guestSessionService.resolve(APPOINTMENT_ID, SESSION_TOKEN))
        .isInstanceOf(BusinessException.class);
  }

  private AppointmentMember guestMember(Long appointmentId) {
    AppointmentMember appointmentMember =
        AppointmentMember.createGuest(
            appointmentId, "철수", MessageDigestSupport.sha256Hex("legacy-token"), NOW);
    ReflectionTestUtils.setField(appointmentMember, "id", 100L);
    return appointmentMember;
  }
}
