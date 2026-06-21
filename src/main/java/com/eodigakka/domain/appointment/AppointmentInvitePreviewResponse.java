package com.eodigakka.domain.appointment;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentInvitePreviewResponse(
    String title,
    LocalDate appointmentDate,
    LocalTime appointmentTime,
    String preferredArea,
    AppointmentStatus status) {

  public static AppointmentInvitePreviewResponse from(Appointment appointment) {
    return new AppointmentInvitePreviewResponse(
        appointment.getTitle(),
        appointment.getAppointmentDate(),
        appointment.getAppointmentTime(),
        appointment.getPreferredArea(),
        appointment.getStatus());
  }
}
