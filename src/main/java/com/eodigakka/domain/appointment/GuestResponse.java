package com.eodigakka.domain.appointment;

public record GuestResponse(Long memberId, String guestName) {

  public static GuestResponse from(AppointmentMember appointmentMember) {
    return new GuestResponse(appointmentMember.getId(), appointmentMember.getGuestName());
  }
}
