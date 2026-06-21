package com.eodigakka.domain.appointment;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentResponse(
    Long id,
    String title,
    LocalDate appointmentDate,
    LocalTime appointmentTime,
    String description,
    String preferredArea,
    String notice,
    String inviteCode,
    AppointmentStatus status,
    AppointmentMemberRole role) {

  public static AppointmentResponse from(
      Appointment appointment, AppointmentMemberRole appointmentMemberRole) {
    return new AppointmentResponse(
        appointment.getId(),
        appointment.getTitle(),
        appointment.getAppointmentDate(),
        appointment.getAppointmentTime(),
        appointment.getDescription(),
        appointment.getPreferredArea(),
        appointment.getNotice(),
        appointment.getInviteCode(),
        appointment.getStatus(),
        appointmentMemberRole);
  }
}
