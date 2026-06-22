package com.eodigakka.domain.appointment;

import com.eodigakka.domain.user.User;
import java.time.Instant;

public record AppointmentMemberResponse(
    Long memberId,
    AppointmentMemberType memberType,
    AppointmentMemberRole role,
    String displayName,
    String profileImage,
    Instant joinedAt) {

  public static AppointmentMemberResponse fromUser(AppointmentMember appointmentMember, User user) {
    return new AppointmentMemberResponse(
        appointmentMember.getId(),
        appointmentMember.getMemberType(),
        appointmentMember.getRole(),
        user.getNickname(),
        user.getProfileImage(),
        appointmentMember.getJoinedAt());
  }

  public static AppointmentMemberResponse fromGuest(AppointmentMember appointmentMember) {
    return new AppointmentMemberResponse(
        appointmentMember.getId(),
        appointmentMember.getMemberType(),
        appointmentMember.getRole(),
        appointmentMember.getGuestName(),
        null,
        appointmentMember.getJoinedAt());
  }
}
