package com.eodigakka.domain.appointment;

import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import com.eodigakka.global.security.AuthUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AppointmentMemberResolver {

  private final AppointmentMemberRepository appointmentMemberRepository;
  private final GuestSessionService guestSessionService;

  public AppointmentMemberResolver(
      AppointmentMemberRepository appointmentMemberRepository,
      GuestSessionService guestSessionService) {
    this.appointmentMemberRepository = appointmentMemberRepository;
    this.guestSessionService = guestSessionService;
  }

  @Transactional(readOnly = true)
  public AppointmentMember resolve(
      Long appointmentId, AuthUser authUser, String guestSessionToken) {
    if (authUser != null) {
      return appointmentMemberRepository
          .findByAppointmentIdAndUserIdAndLeftAtIsNull(appointmentId, authUser.userId())
          .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_MEMBER_NOT_FOUND));
    }

    if (hasText(guestSessionToken)) {
      AppointmentMember appointmentMember =
          guestSessionService.resolve(appointmentId, guestSessionToken);
      if (appointmentMember.isLeft()) {
        throw new BusinessException(ErrorCode.APPOINTMENT_MEMBER_NOT_FOUND);
      }
      return appointmentMember;
    }

    throw new BusinessException(ErrorCode.APPOINTMENT_MEMBER_NOT_FOUND);
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
