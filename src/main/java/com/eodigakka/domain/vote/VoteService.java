package com.eodigakka.domain.vote;

import com.eodigakka.domain.appointment.Appointment;
import com.eodigakka.domain.appointment.AppointmentMember;
import com.eodigakka.domain.appointment.AppointmentMemberResolver;
import com.eodigakka.domain.appointment.AppointmentRepository;
import com.eodigakka.domain.place.PlaceCandidate;
import com.eodigakka.domain.place.PlaceCandidateRepository;
import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import com.eodigakka.global.security.AuthUser;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoteService {

  private final AppointmentRepository appointmentRepository;
  private final AppointmentMemberResolver appointmentMemberResolver;
  private final PlaceCandidateRepository placeCandidateRepository;
  private final VoteRepository voteRepository;
  private final Clock clock;

  @Autowired
  public VoteService(
      AppointmentRepository appointmentRepository,
      AppointmentMemberResolver appointmentMemberResolver,
      PlaceCandidateRepository placeCandidateRepository,
      VoteRepository voteRepository) {
    this(
        appointmentRepository,
        appointmentMemberResolver,
        placeCandidateRepository,
        voteRepository,
        Clock.systemUTC());
  }

  VoteService(
      AppointmentRepository appointmentRepository,
      AppointmentMemberResolver appointmentMemberResolver,
      PlaceCandidateRepository placeCandidateRepository,
      VoteRepository voteRepository,
      Clock clock) {
    this.appointmentRepository = appointmentRepository;
    this.appointmentMemberResolver = appointmentMemberResolver;
    this.placeCandidateRepository = placeCandidateRepository;
    this.voteRepository = voteRepository;
    this.clock = clock;
  }

  @Transactional
  public VoteResponse vote(
      Long appointmentId, AuthUser authUser, String guestToken, VoteRequest request) {
    AppointmentMember appointmentMember =
        appointmentMemberResolver.resolve(appointmentId, authUser, guestToken);
    Appointment appointment = getAppointment(appointmentId);
    appointment.validatePlanning();
    validatePlaceCandidate(appointmentId, request.placeCandidateId());

    Vote vote =
        voteRepository
            .findByAppointmentIdAndMemberId(appointmentId, appointmentMember.getId())
            .map(existingVote -> changeVote(existingVote, request.placeCandidateId()))
            .orElseGet(
                () ->
                    voteRepository.save(
                        Vote.create(
                            appointmentId,
                            request.placeCandidateId(),
                            appointmentMember.getId(),
                            Instant.now(clock))));
    return VoteResponse.from(vote);
  }

  @Transactional(readOnly = true)
  public List<VoteResultResponse> findResults(
      Long appointmentId, AuthUser authUser, String guestToken) {
    AppointmentMember appointmentMember =
        appointmentMemberResolver.resolve(appointmentId, authUser, guestToken);
    getAppointment(appointmentId);

    List<PlaceCandidate> placeCandidates =
        placeCandidateRepository.findByAppointmentIdOrderByCreatedAtAscIdAsc(appointmentId);
    Map<Long, Long> voteCountByPlaceCandidateId =
        voteRepository.findByAppointmentId(appointmentId).stream()
            .collect(Collectors.groupingBy(Vote::getPlaceCandidateId, Collectors.counting()));
    Optional<Long> votedPlaceCandidateId =
        voteRepository
            .findByAppointmentIdAndMemberId(appointmentId, appointmentMember.getId())
            .map(Vote::getPlaceCandidateId);

    return placeCandidates.stream()
        .map(
            placeCandidate ->
                new VoteResultResponse(
                    placeCandidate.getId(),
                    voteCountByPlaceCandidateId.getOrDefault(placeCandidate.getId(), 0L),
                    votedPlaceCandidateId.filter(placeCandidate.getId()::equals).isPresent()))
        .toList();
  }

  private Vote changeVote(Vote vote, Long placeCandidateId) {
    if (!vote.hasPlaceCandidate(placeCandidateId)) {
      vote.changePlaceCandidate(placeCandidateId);
    }
    return vote;
  }

  private void validatePlaceCandidate(Long appointmentId, Long placeCandidateId) {
    if (placeCandidateRepository
        .findByIdAndAppointmentId(placeCandidateId, appointmentId)
        .isEmpty()) {
      throw new BusinessException(ErrorCode.PLACE_CANDIDATE_NOT_FOUND);
    }
  }

  private Appointment getAppointment(Long appointmentId) {
    return appointmentRepository
        .findById(appointmentId)
        .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND));
  }
}
