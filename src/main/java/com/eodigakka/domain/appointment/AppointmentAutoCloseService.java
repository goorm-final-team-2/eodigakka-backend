package com.eodigakka.domain.appointment;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentAutoCloseService {

  private static final Duration AUTO_CLOSE_DELAY = Duration.ofHours(12);
  private static final ZoneId APPOINTMENT_ZONE = ZoneId.of("Asia/Seoul");

  private final AppointmentRepository appointmentRepository;
  private final Clock clock;

  @Autowired
  public AppointmentAutoCloseService(AppointmentRepository appointmentRepository) {
    this(appointmentRepository, Clock.systemUTC());
  }

  AppointmentAutoCloseService(AppointmentRepository appointmentRepository, Clock clock) {
    this.appointmentRepository = appointmentRepository;
    this.clock = clock;
  }

  @Scheduled(
      initialDelayString = "${app.appointment.auto-close.initial-delay-ms:60000}",
      fixedDelayString = "${app.appointment.auto-close.fixed-delay-ms:300000}")
  @Transactional
  public void closeExpiredAppointments() {
    closeExpiredAppointments(Instant.now(clock));
  }

  @Transactional
  int closeExpiredAppointments(Instant now) {
    LocalDateTime cutoff = LocalDateTime.ofInstant(now.minus(AUTO_CLOSE_DELAY), APPOINTMENT_ZONE);
    List<Appointment> targets =
        appointmentRepository.findAutoCloseTargets(
            AppointmentStatus.CONFIRMED, cutoff.toLocalDate(), cutoff.toLocalTime());

    targets.forEach(appointment -> appointment.close(now));
    return targets.size();
  }
}
