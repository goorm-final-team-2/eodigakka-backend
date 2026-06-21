package com.eodigakka.domain.appointment;

import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AppointmentAccessValidator {

  private final AppointmentMemberRepository appointmentMemberRepository;

  public AppointmentAccessValidator(AppointmentMemberRepository appointmentMemberRepository) {
    this.appointmentMemberRepository = appointmentMemberRepository;
  }

  @Transactional(readOnly = true)
  public AppointmentMember validateMember(Long appointmentId, Long userId) {
    return findMember(appointmentId, userId);
  }

  @Transactional(readOnly = true)
  public AppointmentMember validateHost(Long appointmentId, Long userId) {
    AppointmentMember appointmentMember = findMember(appointmentId, userId);
    if (!appointmentMember.isHost()) {
      throw new BusinessException(ErrorCode.APPOINTMENT_HOST_REQUIRED);
    }
    return appointmentMember;
  }

  private AppointmentMember findMember(Long appointmentId, Long userId) {
    return appointmentMemberRepository
        .findByAppointmentIdAndUserId(appointmentId, userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_MEMBER_NOT_FOUND));
  }
}
