package com.eodigakka.domain.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.eodigakka.global.error.BusinessException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentAccessValidatorTest {

  private static final Long APPOINTMENT_ID = 1L;
  private static final Long USER_ID = 10L;

  @Mock private AppointmentMemberRepository appointmentMemberRepository;

  private AppointmentAccessValidator appointmentAccessValidator;

  @BeforeEach
  void setUp() {
    appointmentAccessValidator = new AppointmentAccessValidator(appointmentMemberRepository);
  }

  @Test
  void validateMemberReturnsAppointmentMemberWhenUserJoinedAppointment() {
    AppointmentMember appointmentMember = appointmentMember(AppointmentMemberRole.MEMBER);
    given(appointmentMemberRepository.findByAppointmentIdAndUserId(APPOINTMENT_ID, USER_ID))
        .willReturn(Optional.of(appointmentMember));

    AppointmentMember result = appointmentAccessValidator.validateMember(APPOINTMENT_ID, USER_ID);

    assertThat(result).isSameAs(appointmentMember);
  }

  @Test
  void validateMemberThrowsBusinessExceptionWhenUserHasNotJoinedAppointment() {
    given(appointmentMemberRepository.findByAppointmentIdAndUserId(APPOINTMENT_ID, USER_ID))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> appointmentAccessValidator.validateMember(APPOINTMENT_ID, USER_ID))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void validateHostReturnsAppointmentMemberWhenUserIsHost() {
    AppointmentMember appointmentMember = appointmentMember(AppointmentMemberRole.HOST);
    given(appointmentMemberRepository.findByAppointmentIdAndUserId(APPOINTMENT_ID, USER_ID))
        .willReturn(Optional.of(appointmentMember));

    AppointmentMember result = appointmentAccessValidator.validateHost(APPOINTMENT_ID, USER_ID);

    assertThat(result).isSameAs(appointmentMember);
  }

  @Test
  void validateHostThrowsBusinessExceptionWhenUserIsNotHost() {
    AppointmentMember appointmentMember = appointmentMember(AppointmentMemberRole.MEMBER);
    given(appointmentMemberRepository.findByAppointmentIdAndUserId(APPOINTMENT_ID, USER_ID))
        .willReturn(Optional.of(appointmentMember));

    assertThatThrownBy(() -> appointmentAccessValidator.validateHost(APPOINTMENT_ID, USER_ID))
        .isInstanceOf(BusinessException.class);
  }

  private AppointmentMember appointmentMember(AppointmentMemberRole role) {
    return AppointmentMember.createUserMember(
        APPOINTMENT_ID, USER_ID, role, Instant.parse("2026-06-21T00:00:00Z"));
  }
}
