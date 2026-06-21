package com.eodigakka.domain.appointment;

public record GuestResponse(Long memberId, String guestName, String guestToken) {

  public static GuestResponse of(AppointmentMember appointmentMember, String guestToken) {
    return new GuestResponse(
        appointmentMember.getId(), appointmentMember.getGuestName(), guestToken);
  }
}
