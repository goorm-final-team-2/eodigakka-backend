package com.eodigakka.domain.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.security.AuthUser;
import com.eodigakka.global.security.MessageDigestSupport;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentMemberResolverTest {

  private static final Long APPOINTMENT_ID = 1L;
  private static final Long USER_ID = 10L;
  private static final String GUEST_TOKEN = "guest-token";
  private static final Instant JOINED_AT = Instant.parse("2026-06-21T00:00:00Z");

  @Mock private AppointmentMemberRepository appointmentMemberRepository;

  private AppointmentMemberResolver appointmentMemberResolver;

  @BeforeEach
  void setUp() {
    appointmentMemberResolver = new AppointmentMemberResolver(appointmentMemberRepository);
  }

  @Test
  void resolveReturnsUserAppointmentMemberWhenAuthUserExists() {
    AuthUser authUser = new AuthUser(USER_ID);
    AppointmentMember appointmentMember =
        AppointmentMember.createMember(APPOINTMENT_ID, USER_ID, JOINED_AT);
    given(appointmentMemberRepository.findByAppointmentIdAndUserId(APPOINTMENT_ID, USER_ID))
        .willReturn(Optional.of(appointmentMember));

    AppointmentMember result =
        appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, GUEST_TOKEN);

    assertThat(result).isSameAs(appointmentMember);
    then(appointmentMemberRepository)
        .should(never())
        .findByAppointmentIdAndGuestTokenHashAndMemberType(
            APPOINTMENT_ID,
            MessageDigestSupport.sha256Hex(GUEST_TOKEN),
            AppointmentMemberType.GUEST);
  }

  @Test
  void resolveReturnsGuestAppointmentMemberWhenGuestTokenExists() {
    String guestTokenHash = MessageDigestSupport.sha256Hex(GUEST_TOKEN);
    AppointmentMember appointmentMember =
        AppointmentMember.createGuest(APPOINTMENT_ID, "guest", guestTokenHash, JOINED_AT);
    given(
            appointmentMemberRepository.findByAppointmentIdAndGuestTokenHashAndMemberType(
                APPOINTMENT_ID, guestTokenHash, AppointmentMemberType.GUEST))
        .willReturn(Optional.of(appointmentMember));

    AppointmentMember result = appointmentMemberResolver.resolve(APPOINTMENT_ID, null, GUEST_TOKEN);

    assertThat(result).isSameAs(appointmentMember);
  }

  @Test
  void resolveThrowsBusinessExceptionWhenAuthUserHasNotJoinedAppointment() {
    AuthUser authUser = new AuthUser(USER_ID);
    given(appointmentMemberRepository.findByAppointmentIdAndUserId(APPOINTMENT_ID, USER_ID))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void resolveThrowsBusinessExceptionWhenGuestTokenIsInvalid() {
    String guestTokenHash = MessageDigestSupport.sha256Hex(GUEST_TOKEN);
    given(
            appointmentMemberRepository.findByAppointmentIdAndGuestTokenHashAndMemberType(
                APPOINTMENT_ID, guestTokenHash, AppointmentMemberType.GUEST))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> appointmentMemberResolver.resolve(APPOINTMENT_ID, null, GUEST_TOKEN))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void resolveThrowsBusinessExceptionWhenRequesterIsMissing() {
    assertThatThrownBy(() -> appointmentMemberResolver.resolve(APPOINTMENT_ID, null, null))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void resolveThrowsBusinessExceptionWhenGuestTokenIsBlank() {
    assertThatThrownBy(() -> appointmentMemberResolver.resolve(APPOINTMENT_ID, null, " "))
        .isInstanceOf(BusinessException.class);
  }
}
