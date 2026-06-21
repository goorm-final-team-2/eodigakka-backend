package com.eodigakka.domain.appointment;

import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import com.eodigakka.global.security.AuthUser;
import com.eodigakka.global.security.MessageDigestSupport;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AppointmentMemberResolver {

  private final AppointmentMemberRepository appointmentMemberRepository;

  public AppointmentMemberResolver(AppointmentMemberRepository appointmentMemberRepository) {
    this.appointmentMemberRepository = appointmentMemberRepository;
  }

  @Transactional(readOnly = true)
  public AppointmentMember resolve(Long appointmentId, AuthUser authUser, String guestToken) {
    if (authUser != null) {
      return appointmentMemberRepository
          .findByAppointmentIdAndUserId(appointmentId, authUser.userId())
          .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_MEMBER_NOT_FOUND));
    }

    if (hasText(guestToken)) {
      return appointmentMemberRepository
          .findByAppointmentIdAndGuestTokenHashAndMemberType(
              appointmentId,
              MessageDigestSupport.sha256Hex(guestToken),
              AppointmentMemberType.GUEST)
          .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_MEMBER_NOT_FOUND));
    }

    throw new BusinessException(ErrorCode.APPOINTMENT_MEMBER_NOT_FOUND);
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
