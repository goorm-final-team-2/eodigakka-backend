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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberLocationService {

  private static final Logger log = LoggerFactory.getLogger(MemberLocationService.class);

  private final AppointmentRepository appointmentRepository;
  private final AppointmentMemberResolver appointmentMemberResolver;
  private final MemberLocationRepository memberLocationRepository;
  private final RedisMemberLocationStore redisMemberLocationStore;
  private final Clock clock;

  @Autowired
  public MemberLocationService(
      AppointmentRepository appointmentRepository,
      AppointmentMemberResolver appointmentMemberResolver,
      MemberLocationRepository memberLocationRepository,
      RedisMemberLocationStore redisMemberLocationStore) {
    this(
        appointmentRepository,
        appointmentMemberResolver,
        memberLocationRepository,
        redisMemberLocationStore,
        Clock.systemUTC());
  }

  MemberLocationService(
      AppointmentRepository appointmentRepository,
      AppointmentMemberResolver appointmentMemberResolver,
      MemberLocationRepository memberLocationRepository,
      RedisMemberLocationStore redisMemberLocationStore,
      Clock clock) {
    this.appointmentRepository = appointmentRepository;
    this.appointmentMemberResolver = appointmentMemberResolver;
    this.memberLocationRepository = memberLocationRepository;
    this.redisMemberLocationStore = redisMemberLocationStore;
    this.clock = clock;
  }

  @Transactional
  public MemberLocationResponse updateMine(
      Long appointmentId,
      AuthUser authUser,
      String guestSessionToken,
      MemberLocationUpdateRequest request) {
    AppointmentMember appointmentMember =
        appointmentMemberResolver.resolve(appointmentId, authUser, guestSessionToken);
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
    MemberLocationResponse response = MemberLocationResponse.from(memberLocation);
    saveLatestLocation(response);
    return response;
  }

  @Transactional(readOnly = true)
  public List<MemberLocationResponse> findAll(
      Long appointmentId, AuthUser authUser, String guestSessionToken) {
    appointmentMemberResolver.resolve(appointmentId, authUser, guestSessionToken);
    Appointment appointment = getAppointment(appointmentId);
    appointment.validateConfirmed();
    List<MemberLocationResponse> latestLocations = findLatestLocations(appointmentId);
    if (!latestLocations.isEmpty()) {
      return latestLocations;
    }
    return memberLocationRepository
        .findByAppointmentIdOrderByUpdatedAtDescIdAsc(appointmentId)
        .stream()
        .map(MemberLocationResponse::from)
        .toList();
  }

  private void saveLatestLocation(MemberLocationResponse response) {
    try {
      redisMemberLocationStore.save(response);
    } catch (RuntimeException exception) {
      log.warn(
          "Failed to save member location to Redis. appointmentId={}, memberId={}",
          response.appointmentId(),
          response.memberId(),
          exception);
    }
  }

  private List<MemberLocationResponse> findLatestLocations(Long appointmentId) {
    try {
      List<MemberLocationResponse> latestLocations =
          redisMemberLocationStore.findAll(appointmentId);
      return latestLocations == null ? List.of() : latestLocations;
    } catch (RuntimeException exception) {
      log.warn(
          "Failed to find member locations from Redis. appointmentId={}", appointmentId, exception);
      return List.of();
    }
  }

  private Appointment getAppointment(Long appointmentId) {
    return appointmentRepository
        .findById(appointmentId)
        .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND));
  }
}
