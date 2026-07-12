package com.eodigakka.domain.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AppointmentAutoCloseServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-13T12:00:00Z");

  @Mock private AppointmentRepository appointmentRepository;

  private AppointmentAutoCloseService appointmentAutoCloseService;

  @BeforeEach
  void setUp() {
    appointmentAutoCloseService =
        new AppointmentAutoCloseService(appointmentRepository, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void closeExpiredAppointmentsClosesConfirmedAppointmentsPastTwelveHours() {
    Appointment appointment = appointment(AppointmentStatus.CONFIRMED);
    given(
            appointmentRepository.findAutoCloseTargets(
                AppointmentStatus.CONFIRMED, LocalDate.of(2026, 7, 13), LocalTime.of(9, 0)))
        .willReturn(List.of(appointment));

    int closedCount = appointmentAutoCloseService.closeExpiredAppointments(NOW);

    assertThat(closedCount).isEqualTo(1);
    assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CLOSED);
    assertThat(appointment.getUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  void closeExpiredAppointmentsReturnsZeroWhenNoTargetExists() {
    given(
            appointmentRepository.findAutoCloseTargets(
                AppointmentStatus.CONFIRMED, LocalDate.of(2026, 7, 13), LocalTime.of(9, 0)))
        .willReturn(List.of());

    int closedCount = appointmentAutoCloseService.closeExpiredAppointments(NOW);

    assertThat(closedCount).isZero();
  }

  private Appointment appointment(AppointmentStatus status) {
    Appointment appointment =
        Appointment.create(
            "강남 저녁 약속",
            LocalDate.of(2026, 7, 12),
            LocalTime.of(20, 0),
            "저녁 먹을 장소 정하기",
            "강남역",
            "늦지 않기",
            1L,
            "A7K2P9QX",
            NOW.minusSeconds(3600));
    ReflectionTestUtils.setField(appointment, "id", 10L);
    ReflectionTestUtils.setField(appointment, "status", status);
    return appointment;
  }
}
