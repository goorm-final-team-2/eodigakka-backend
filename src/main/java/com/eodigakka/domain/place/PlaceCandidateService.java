package com.eodigakka.domain.place;

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
public class PlaceCandidateService {

  private final AppointmentRepository appointmentRepository;
  private final AppointmentMemberResolver appointmentMemberResolver;
  private final PlaceCandidateRepository placeCandidateRepository;
  private final Clock clock;

  @Autowired
  public PlaceCandidateService(
      AppointmentRepository appointmentRepository,
      AppointmentMemberResolver appointmentMemberResolver,
      PlaceCandidateRepository placeCandidateRepository) {
    this(
        appointmentRepository,
        appointmentMemberResolver,
        placeCandidateRepository,
        Clock.systemUTC());
  }

  PlaceCandidateService(
      AppointmentRepository appointmentRepository,
      AppointmentMemberResolver appointmentMemberResolver,
      PlaceCandidateRepository placeCandidateRepository,
      Clock clock) {
    this.appointmentRepository = appointmentRepository;
    this.appointmentMemberResolver = appointmentMemberResolver;
    this.placeCandidateRepository = placeCandidateRepository;
    this.clock = clock;
  }

  @Transactional
  public PlaceCandidateResponse create(
      Long appointmentId,
      AuthUser authUser,
      String guestToken,
      PlaceCandidateCreateRequest request) {
    AppointmentMember appointmentMember =
        appointmentMemberResolver.resolve(appointmentId, authUser, guestToken);
    Appointment appointment = getAppointment(appointmentId);
    appointment.validatePlanning();
    if (placeCandidateRepository.existsByAppointmentIdAndKakaoPlaceId(
        appointmentId, request.kakaoPlaceId())) {
      throw new BusinessException(ErrorCode.PLACE_CANDIDATE_ALREADY_EXISTS);
    }

    PlaceCandidate placeCandidate =
        placeCandidateRepository.save(
            PlaceCandidate.create(
                appointmentId,
                request.kakaoPlaceId(),
                request.name(),
                request.address(),
                request.roadAddress(),
                request.category(),
                request.placeUrl(),
                request.phone(),
                request.latitude(),
                request.longitude(),
                appointmentMember.getId(),
                Instant.now(clock)));
    return PlaceCandidateResponse.from(placeCandidate);
  }

  @Transactional(readOnly = true)
  public List<PlaceCandidateResponse> findAll(
      Long appointmentId, AuthUser authUser, String guestToken) {
    appointmentMemberResolver.resolve(appointmentId, authUser, guestToken);
    getAppointment(appointmentId);
    return placeCandidateRepository
        .findByAppointmentIdOrderByCreatedAtAscIdAsc(appointmentId)
        .stream()
        .map(PlaceCandidateResponse::from)
        .toList();
  }

  private Appointment getAppointment(Long appointmentId) {
    return appointmentRepository
        .findById(appointmentId)
        .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND));
  }
}
