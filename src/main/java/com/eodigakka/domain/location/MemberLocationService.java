package com.eodigakka.domain.location;

import com.eodigakka.domain.appointment.Appointment;
import com.eodigakka.domain.appointment.AppointmentMember;
import com.eodigakka.domain.appointment.AppointmentMemberResolver;
import com.eodigakka.domain.appointment.AppointmentRepository;
import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import com.eodigakka.global.security.AuthUser;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberLocationService {

  private final AppointmentRepository appointmentRepository;
  private final AppointmentMemberResolver appointmentMemberResolver;
  private final MemberLocationRepository memberLocationRepository;
  private final Clock clock;

  @Autowired
  public MemberLocationService(
      AppointmentRepository appointmentRepository,
      AppointmentMemberResolver appointmentMemberResolver,
      MemberLocationRepository memberLocationRepository) {
    this(
        appointmentRepository,
        appointmentMemberResolver,
        memberLocationRepository,
        Clock.systemUTC());
  }

  MemberLocationService(
      AppointmentRepository appointmentRepository,
      AppointmentMemberResolver appointmentMemberResolver,
      MemberLocationRepository memberLocationRepository,
      Clock clock) {
    this.appointmentRepository = appointmentRepository;
    this.appointmentMemberResolver = appointmentMemberResolver;
    this.memberLocationRepository = memberLocationRepository;
    this.clock = clock;
  }

  @Transactional
  public MemberLocationResponse updateMine(
      Long appointmentId,
      AuthUser authUser,
      String guestToken,
      MemberLocationUpdateRequest request) {
    AppointmentMember appointmentMember =
        appointmentMemberResolver.resolve(appointmentId, authUser, guestToken);
    Appointment appointment = getAppointment(appointmentId);
    appointment.validateConfirmed();

    Instant now = Instant.now(clock);
    MemberLocation memberLocation =
        memberLocationRepository
            .findByAppointmentIdAndMemberId(appointmentId, appointmentMember.getId())
            .map(
                existingLocation -> {
                  existingLocation.update(
                      request.latitude(), request.longitude(), request.accuracy(), now);
                  return existingLocation;
                })
            .orElseGet(
                () ->
                    memberLocationRepository.save(
                        MemberLocation.create(
                            appointmentId,
                            appointmentMember.getId(),
                            request.latitude(),
                            request.longitude(),
                            request.accuracy(),
                            now)));
    return MemberLocationResponse.from(memberLocation);
  }

  @Transactional(readOnly = true)
  public List<MemberLocationResponse> findAll(
      Long appointmentId, AuthUser authUser, String guestToken) {
    appointmentMemberResolver.resolve(appointmentId, authUser, guestToken);
    Appointment appointment = getAppointment(appointmentId);
    appointment.validateConfirmed();
    return memberLocationRepository
        .findByAppointmentIdOrderByUpdatedAtDescIdAsc(appointmentId)
        .stream()
        .map(MemberLocationResponse::from)
        .toList();
  }

  private Appointment getAppointment(Long appointmentId) {
    return appointmentRepository
        .findById(appointmentId)
        .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND));
  }
}
