package com.eodigakka.domain.confirmedplace;

import com.eodigakka.domain.appointment.Appointment;
import com.eodigakka.domain.appointment.AppointmentAccessValidator;
import com.eodigakka.domain.appointment.AppointmentMemberResolver;
import com.eodigakka.domain.appointment.AppointmentRepository;
import com.eodigakka.domain.place.PlaceCandidate;
import com.eodigakka.domain.place.PlaceCandidateRepository;
import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import com.eodigakka.global.security.AuthUser;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfirmedPlaceService {

  private final AppointmentRepository appointmentRepository;
  private final AppointmentAccessValidator appointmentAccessValidator;
  private final AppointmentMemberResolver appointmentMemberResolver;
  private final PlaceCandidateRepository placeCandidateRepository;
  private final ConfirmedPlaceRepository confirmedPlaceRepository;
  private final Clock clock;

  @Autowired
  public ConfirmedPlaceService(
      AppointmentRepository appointmentRepository,
      AppointmentAccessValidator appointmentAccessValidator,
      AppointmentMemberResolver appointmentMemberResolver,
      PlaceCandidateRepository placeCandidateRepository,
      ConfirmedPlaceRepository confirmedPlaceRepository) {
    this(
        appointmentRepository,
        appointmentAccessValidator,
        appointmentMemberResolver,
        placeCandidateRepository,
        confirmedPlaceRepository,
        Clock.systemUTC());
  }

  ConfirmedPlaceService(
      AppointmentRepository appointmentRepository,
      AppointmentAccessValidator appointmentAccessValidator,
      AppointmentMemberResolver appointmentMemberResolver,
      PlaceCandidateRepository placeCandidateRepository,
      ConfirmedPlaceRepository confirmedPlaceRepository,
      Clock clock) {
    this.appointmentRepository = appointmentRepository;
    this.appointmentAccessValidator = appointmentAccessValidator;
    this.appointmentMemberResolver = appointmentMemberResolver;
    this.placeCandidateRepository = placeCandidateRepository;
    this.confirmedPlaceRepository = confirmedPlaceRepository;
    this.clock = clock;
  }

  @Transactional
  public ConfirmedPlaceResponse confirm(
      Long appointmentId, Long userId, ConfirmedPlaceRequest request) {
    appointmentAccessValidator.validateHost(appointmentId, userId);
    Appointment appointment = getAppointment(appointmentId);
    appointment.validatePlanning();
    PlaceCandidate placeCandidate = getPlaceCandidate(appointmentId, request.placeCandidateId());

    Instant now = Instant.now(clock);
    ConfirmedPlace confirmedPlace =
        confirmedPlaceRepository.save(
            ConfirmedPlace.create(appointmentId, request.placeCandidateId(), userId, now));
    appointment.confirm(now);
    return ConfirmedPlaceResponse.from(confirmedPlace, placeCandidate);
  }

  @Transactional(readOnly = true)
  public ConfirmedPlaceResponse find(
      Long appointmentId, AuthUser authUser, String guestSessionToken) {
    appointmentMemberResolver.resolve(appointmentId, authUser, guestSessionToken);
    getAppointment(appointmentId);
    ConfirmedPlace confirmedPlace =
        confirmedPlaceRepository
            .findByAppointmentId(appointmentId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CONFIRMED_PLACE_NOT_FOUND));
    PlaceCandidate placeCandidate =
        getPlaceCandidate(appointmentId, confirmedPlace.getPlaceCandidateId());
    return ConfirmedPlaceResponse.from(confirmedPlace, placeCandidate);
  }

  private PlaceCandidate getPlaceCandidate(Long appointmentId, Long placeCandidateId) {
    return placeCandidateRepository
        .findByIdAndAppointmentId(placeCandidateId, appointmentId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_CANDIDATE_NOT_FOUND));
  }

  private Appointment getAppointment(Long appointmentId) {
    return appointmentRepository
        .findById(appointmentId)
        .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND));
  }
}
