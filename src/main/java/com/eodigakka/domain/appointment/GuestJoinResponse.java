package com.eodigakka.domain.appointment;

public record GuestJoinResponse(AppointmentResponse appointment, GuestResponse guest) {

  public static GuestJoinResponse from(
      Appointment appointment, AppointmentMember appointmentMember) {
    return new GuestJoinResponse(
        AppointmentResponse.from(appointment, appointmentMember.getRole()),
        GuestResponse.from(appointmentMember));
  }
}
