package com.eodigakka.domain.appointment;

public record GuestJoinResponse(AppointmentResponse appointment, GuestResponse guest) {

  public static GuestJoinResponse of(
      Appointment appointment, AppointmentMember appointmentMember, String guestToken) {
    return new GuestJoinResponse(
        AppointmentResponse.from(appointment, appointmentMember.getRole()),
        GuestResponse.of(appointmentMember, guestToken));
  }
}
