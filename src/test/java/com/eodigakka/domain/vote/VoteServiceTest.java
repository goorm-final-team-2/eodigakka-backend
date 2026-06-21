package com.eodigakka.domain.vote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.eodigakka.domain.appointment.Appointment;
import com.eodigakka.domain.appointment.AppointmentMember;
import com.eodigakka.domain.appointment.AppointmentMemberResolver;
import com.eodigakka.domain.appointment.AppointmentRepository;
import com.eodigakka.domain.appointment.AppointmentStatus;
import com.eodigakka.domain.place.PlaceCandidate;
import com.eodigakka.domain.place.PlaceCandidateRepository;
import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import com.eodigakka.global.security.AuthUser;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

  private static final Long APPOINTMENT_ID = 10L;
  private static final Long USER_ID = 1L;
  private static final Long MEMBER_ID = 100L;
  private static final Long PLACE_CANDIDATE_ID = 1000L;
  private static final Long OTHER_PLACE_CANDIDATE_ID = 1001L;
  private static final String GUEST_TOKEN = "guest-token";
  private static final Instant NOW = Instant.parse("2026-06-21T00:00:00Z");

  @Mock private AppointmentRepository appointmentRepository;
  @Mock private AppointmentMemberResolver appointmentMemberResolver;
  @Mock private PlaceCandidateRepository placeCandidateRepository;
  @Mock private VoteRepository voteRepository;

  private VoteService voteService;

  @BeforeEach
  void setUp() {
    voteService =
        new VoteService(
            appointmentRepository,
            appointmentMemberResolver,
            placeCandidateRepository,
            voteRepository,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void voteCreatesVoteWhenUserHasNotVoted() {
    AuthUser authUser = new AuthUser(USER_ID);
    VoteRequest request = new VoteRequest(PLACE_CANDIDATE_ID);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willReturn(userMember());
    given(appointmentRepository.findById(APPOINTMENT_ID))
        .willReturn(Optional.of(appointment(AppointmentStatus.PLANNING)));
    given(placeCandidateRepository.findByIdAndAppointmentId(PLACE_CANDIDATE_ID, APPOINTMENT_ID))
        .willReturn(Optional.of(placeCandidate(PLACE_CANDIDATE_ID)));
    given(voteRepository.findByAppointmentIdAndMemberId(APPOINTMENT_ID, MEMBER_ID))
        .willReturn(Optional.empty());
    given(voteRepository.save(any(Vote.class)))
        .willAnswer(
            invocation -> {
              Vote vote = invocation.getArgument(0);
              ReflectionTestUtils.setField(vote, "id", 1L);
              return vote;
            });

    VoteResponse response = voteService.vote(APPOINTMENT_ID, authUser, null, request);

    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.appointmentId()).isEqualTo(APPOINTMENT_ID);
    assertThat(response.placeCandidateId()).isEqualTo(PLACE_CANDIDATE_ID);
    assertThat(response.memberId()).isEqualTo(MEMBER_ID);
    assertThat(response.createdAt()).isEqualTo(NOW);
  }

  @Test
  void voteCreatesVoteWhenGuestHasNotVoted() {
    VoteRequest request = new VoteRequest(PLACE_CANDIDATE_ID);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, null, GUEST_TOKEN))
        .willReturn(guestMember());
    given(appointmentRepository.findById(APPOINTMENT_ID))
        .willReturn(Optional.of(appointment(AppointmentStatus.PLANNING)));
    given(placeCandidateRepository.findByIdAndAppointmentId(PLACE_CANDIDATE_ID, APPOINTMENT_ID))
        .willReturn(Optional.of(placeCandidate(PLACE_CANDIDATE_ID)));
    given(voteRepository.findByAppointmentIdAndMemberId(APPOINTMENT_ID, MEMBER_ID))
        .willReturn(Optional.empty());
    given(voteRepository.save(any(Vote.class))).willAnswer(invocation -> invocation.getArgument(0));

    VoteResponse response = voteService.vote(APPOINTMENT_ID, null, GUEST_TOKEN, request);

    assertThat(response.placeCandidateId()).isEqualTo(PLACE_CANDIDATE_ID);
    assertThat(response.memberId()).isEqualTo(MEMBER_ID);
  }

  @Test
  void voteReturnsExistingVoteWhenUserVotesSamePlaceCandidate() {
    AuthUser authUser = new AuthUser(USER_ID);
    Vote existingVote = vote(PLACE_CANDIDATE_ID);
    VoteRequest request = new VoteRequest(PLACE_CANDIDATE_ID);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willReturn(userMember());
    given(appointmentRepository.findById(APPOINTMENT_ID))
        .willReturn(Optional.of(appointment(AppointmentStatus.PLANNING)));
    given(placeCandidateRepository.findByIdAndAppointmentId(PLACE_CANDIDATE_ID, APPOINTMENT_ID))
        .willReturn(Optional.of(placeCandidate(PLACE_CANDIDATE_ID)));
    given(voteRepository.findByAppointmentIdAndMemberId(APPOINTMENT_ID, MEMBER_ID))
        .willReturn(Optional.of(existingVote));

    VoteResponse response = voteService.vote(APPOINTMENT_ID, authUser, null, request);

    assertThat(response.placeCandidateId()).isEqualTo(PLACE_CANDIDATE_ID);
    verify(voteRepository, never()).save(any());
  }

  @Test
  void voteChangesExistingVoteWhenUserVotesDifferentPlaceCandidate() {
    AuthUser authUser = new AuthUser(USER_ID);
    Vote existingVote = vote(PLACE_CANDIDATE_ID);
    VoteRequest request = new VoteRequest(OTHER_PLACE_CANDIDATE_ID);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willReturn(userMember());
    given(appointmentRepository.findById(APPOINTMENT_ID))
        .willReturn(Optional.of(appointment(AppointmentStatus.PLANNING)));
    given(
            placeCandidateRepository.findByIdAndAppointmentId(
                OTHER_PLACE_CANDIDATE_ID, APPOINTMENT_ID))
        .willReturn(Optional.of(placeCandidate(OTHER_PLACE_CANDIDATE_ID)));
    given(voteRepository.findByAppointmentIdAndMemberId(APPOINTMENT_ID, MEMBER_ID))
        .willReturn(Optional.of(existingVote));

    VoteResponse response = voteService.vote(APPOINTMENT_ID, authUser, null, request);

    assertThat(response.placeCandidateId()).isEqualTo(OTHER_PLACE_CANDIDATE_ID);
    assertThat(existingVote.getPlaceCandidateId()).isEqualTo(OTHER_PLACE_CANDIDATE_ID);
    verify(voteRepository, never()).save(any());
  }

  @Test
  void voteThrowsBusinessExceptionWhenRequesterIsNotAppointmentMember() {
    AuthUser authUser = new AuthUser(USER_ID);
    VoteRequest request = new VoteRequest(PLACE_CANDIDATE_ID);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willThrow(new BusinessException(ErrorCode.APPOINTMENT_MEMBER_NOT_FOUND));

    assertThatThrownBy(() -> voteService.vote(APPOINTMENT_ID, authUser, null, request))
        .isInstanceOf(BusinessException.class);
    verify(voteRepository, never()).save(any());
  }

  @Test
  void voteThrowsBusinessExceptionWhenAppointmentIsNotPlanning() {
    AuthUser authUser = new AuthUser(USER_ID);
    VoteRequest request = new VoteRequest(PLACE_CANDIDATE_ID);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willReturn(userMember());
    given(appointmentRepository.findById(APPOINTMENT_ID))
        .willReturn(Optional.of(appointment(AppointmentStatus.CONFIRMED)));

    assertThatThrownBy(() -> voteService.vote(APPOINTMENT_ID, authUser, null, request))
        .isInstanceOf(BusinessException.class);
    verify(voteRepository, never()).save(any());
  }

  @Test
  void voteThrowsBusinessExceptionWhenPlaceCandidateDoesNotExistInAppointment() {
    AuthUser authUser = new AuthUser(USER_ID);
    VoteRequest request = new VoteRequest(PLACE_CANDIDATE_ID);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willReturn(userMember());
    given(appointmentRepository.findById(APPOINTMENT_ID))
        .willReturn(Optional.of(appointment(AppointmentStatus.PLANNING)));
    given(placeCandidateRepository.findByIdAndAppointmentId(PLACE_CANDIDATE_ID, APPOINTMENT_ID))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> voteService.vote(APPOINTMENT_ID, authUser, null, request))
        .isInstanceOf(BusinessException.class);
    verify(voteRepository, never()).save(any());
  }

  @Test
  void findResultsReturnsVoteCountAndVotedByMeForEachPlaceCandidate() {
    AuthUser authUser = new AuthUser(USER_ID);
    PlaceCandidate firstCandidate = placeCandidate(PLACE_CANDIDATE_ID);
    PlaceCandidate secondCandidate = placeCandidate(OTHER_PLACE_CANDIDATE_ID);
    Vote myVote = vote(PLACE_CANDIDATE_ID);
    Vote otherVote = Vote.create(APPOINTMENT_ID, PLACE_CANDIDATE_ID, 200L, NOW);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willReturn(userMember());
    given(appointmentRepository.findById(APPOINTMENT_ID))
        .willReturn(Optional.of(appointment(AppointmentStatus.CONFIRMED)));
    given(placeCandidateRepository.findByAppointmentIdOrderByCreatedAtAscIdAsc(APPOINTMENT_ID))
        .willReturn(List.of(firstCandidate, secondCandidate));
    given(voteRepository.findByAppointmentId(APPOINTMENT_ID))
        .willReturn(List.of(myVote, otherVote));
    given(voteRepository.findByAppointmentIdAndMemberId(APPOINTMENT_ID, MEMBER_ID))
        .willReturn(Optional.of(myVote));

    List<VoteResultResponse> responses = voteService.findResults(APPOINTMENT_ID, authUser, null);

    assertThat(responses).hasSize(2);
    assertThat(responses.getFirst().placeCandidateId()).isEqualTo(PLACE_CANDIDATE_ID);
    assertThat(responses.getFirst().voteCount()).isEqualTo(2);
    assertThat(responses.getFirst().votedByMe()).isTrue();
    assertThat(responses.get(1).placeCandidateId()).isEqualTo(OTHER_PLACE_CANDIDATE_ID);
    assertThat(responses.get(1).voteCount()).isZero();
    assertThat(responses.get(1).votedByMe()).isFalse();
  }

  @Test
  void findResultsReturnsVoteResultsWhenGuestIsAppointmentMember() {
    PlaceCandidate placeCandidate = placeCandidate(PLACE_CANDIDATE_ID);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, null, GUEST_TOKEN))
        .willReturn(guestMember());
    given(appointmentRepository.findById(APPOINTMENT_ID))
        .willReturn(Optional.of(appointment(AppointmentStatus.CLOSED)));
    given(placeCandidateRepository.findByAppointmentIdOrderByCreatedAtAscIdAsc(APPOINTMENT_ID))
        .willReturn(List.of(placeCandidate));
    given(voteRepository.findByAppointmentId(APPOINTMENT_ID)).willReturn(List.of());
    given(voteRepository.findByAppointmentIdAndMemberId(APPOINTMENT_ID, MEMBER_ID))
        .willReturn(Optional.empty());

    List<VoteResultResponse> responses = voteService.findResults(APPOINTMENT_ID, null, GUEST_TOKEN);

    assertThat(responses).hasSize(1);
    assertThat(responses.getFirst().voteCount()).isZero();
    assertThat(responses.getFirst().votedByMe()).isFalse();
  }

  @Test
  void findResultsThrowsBusinessExceptionWhenRequesterIsNotAppointmentMember() {
    AuthUser authUser = new AuthUser(USER_ID);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willThrow(new BusinessException(ErrorCode.APPOINTMENT_MEMBER_NOT_FOUND));

    assertThatThrownBy(() -> voteService.findResults(APPOINTMENT_ID, authUser, null))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void findResultsThrowsBusinessExceptionWhenAppointmentDoesNotExist() {
    AuthUser authUser = new AuthUser(USER_ID);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willReturn(userMember());
    given(appointmentRepository.findById(APPOINTMENT_ID)).willReturn(Optional.empty());

    assertThatThrownBy(() -> voteService.findResults(APPOINTMENT_ID, authUser, null))
        .isInstanceOf(BusinessException.class);
  }

  private Appointment appointment(AppointmentStatus status) {
    Appointment appointment =
        Appointment.create(
            "강남 모임",
            LocalDate.of(2026, 7, 1),
            LocalTime.of(19, 0),
            "저녁 약속",
            "강남역",
            "늦지 않기",
            USER_ID,
            "A7K2P9QX",
            NOW);
    ReflectionTestUtils.setField(appointment, "id", APPOINTMENT_ID);
    ReflectionTestUtils.setField(appointment, "status", status);
    return appointment;
  }

  private AppointmentMember userMember() {
    AppointmentMember appointmentMember =
        AppointmentMember.createMember(APPOINTMENT_ID, USER_ID, NOW);
    ReflectionTestUtils.setField(appointmentMember, "id", MEMBER_ID);
    return appointmentMember;
  }

  private AppointmentMember guestMember() {
    AppointmentMember appointmentMember =
        AppointmentMember.createGuest(APPOINTMENT_ID, "guest", "guest-token-hash", NOW);
    ReflectionTestUtils.setField(appointmentMember, "id", MEMBER_ID);
    return appointmentMember;
  }

  private PlaceCandidate placeCandidate(Long placeCandidateId) {
    PlaceCandidate placeCandidate =
        PlaceCandidate.create(
            APPOINTMENT_ID,
            placeCandidateId.toString(),
            "강남역",
            "서울 강남구 강남대로 396",
            "서울 강남구 강남대로 396",
            "지하철역",
            "https://place.map.kakao.com/" + placeCandidateId,
            "02-123-4567",
            37.4979,
            127.0276,
            MEMBER_ID,
            NOW);
    ReflectionTestUtils.setField(placeCandidate, "id", placeCandidateId);
    return placeCandidate;
  }

  private Vote vote(Long placeCandidateId) {
    Vote vote = Vote.create(APPOINTMENT_ID, placeCandidateId, MEMBER_ID, NOW);
    ReflectionTestUtils.setField(vote, "id", 1L);
    return vote;
  }
}
