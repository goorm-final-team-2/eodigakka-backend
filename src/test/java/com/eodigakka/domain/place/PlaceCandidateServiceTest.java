package com.eodigakka.domain.place;

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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlaceCandidateServiceTest {

  private static final Long APPOINTMENT_ID = 10L;
  private static final Long USER_ID = 1L;
  private static final Long MEMBER_ID = 100L;
  private static final Long OTHER_MEMBER_ID = 101L;
  private static final Long PLACE_CANDIDATE_ID = 1000L;
  private static final String GUEST_TOKEN = "guest-token";
  private static final Instant NOW = Instant.parse("2026-06-21T00:00:00Z");

  @Mock private AppointmentRepository appointmentRepository;
  @Mock private AppointmentMemberResolver appointmentMemberResolver;
  @Mock private PlaceCandidateRepository placeCandidateRepository;

  private PlaceCandidateService placeCandidateService;

  @BeforeEach
  void setUp() {
    placeCandidateService =
        new PlaceCandidateService(
            appointmentRepository,
            appointmentMemberResolver,
            placeCandidateRepository,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void createSavesPlaceCandidateWhenUserIsAppointmentMemberAndAppointmentIsPlanning() {
    AuthUser authUser = new AuthUser(USER_ID);
    Appointment appointment = appointment(AppointmentStatus.PLANNING);
    AppointmentMember appointmentMember = userMember();
    PlaceCandidateCreateRequest request = createRequest();
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willReturn(appointmentMember);
    given(appointmentRepository.findById(APPOINTMENT_ID)).willReturn(Optional.of(appointment));
    given(placeCandidateRepository.existsByAppointmentIdAndKakaoPlaceId(APPOINTMENT_ID, "12345"))
        .willReturn(false);
    given(placeCandidateRepository.save(any(PlaceCandidate.class)))
        .willAnswer(
            invocation -> {
              PlaceCandidate placeCandidate = invocation.getArgument(0);
              ReflectionTestUtils.setField(placeCandidate, "id", 1L);
              return placeCandidate;
            });

    PlaceCandidateResponse response =
        placeCandidateService.create(APPOINTMENT_ID, authUser, null, request);

    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.appointmentId()).isEqualTo(APPOINTMENT_ID);
    assertThat(response.kakaoPlaceId()).isEqualTo("12345");
    assertThat(response.name()).isEqualTo("강남역");
    assertThat(response.addedByMemberId()).isEqualTo(MEMBER_ID);
    assertThat(response.createdAt()).isEqualTo(NOW);

    ArgumentCaptor<PlaceCandidate> candidateCaptor = ArgumentCaptor.forClass(PlaceCandidate.class);
    verify(placeCandidateRepository).save(candidateCaptor.capture());
    assertThat(candidateCaptor.getValue().getAddedByMemberId()).isEqualTo(MEMBER_ID);
  }

  @Test
  void createSavesPlaceCandidateWhenGuestIsAppointmentMemberAndAppointmentIsPlanning() {
    Appointment appointment = appointment(AppointmentStatus.PLANNING);
    AppointmentMember appointmentMember = guestMember();
    PlaceCandidateCreateRequest request = createRequest();
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, null, GUEST_TOKEN))
        .willReturn(appointmentMember);
    given(appointmentRepository.findById(APPOINTMENT_ID)).willReturn(Optional.of(appointment));
    given(placeCandidateRepository.existsByAppointmentIdAndKakaoPlaceId(APPOINTMENT_ID, "12345"))
        .willReturn(false);
    given(placeCandidateRepository.save(any(PlaceCandidate.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    PlaceCandidateResponse response =
        placeCandidateService.create(APPOINTMENT_ID, null, GUEST_TOKEN, request);

    assertThat(response.addedByMemberId()).isEqualTo(MEMBER_ID);
  }

  @Test
  void createThrowsBusinessExceptionWhenRequesterIsNotAppointmentMember() {
    AuthUser authUser = new AuthUser(USER_ID);
    PlaceCandidateCreateRequest request = createRequest();
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willThrow(new BusinessException(ErrorCode.APPOINTMENT_MEMBER_NOT_FOUND));

    assertThatThrownBy(() -> placeCandidateService.create(APPOINTMENT_ID, authUser, null, request))
        .isInstanceOf(BusinessException.class);
    verify(placeCandidateRepository, never()).save(any());
  }

  @Test
  void createThrowsBusinessExceptionWhenAppointmentIsNotPlanning() {
    AuthUser authUser = new AuthUser(USER_ID);
    Appointment appointment = appointment(AppointmentStatus.CONFIRMED);
    PlaceCandidateCreateRequest request = createRequest();
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willReturn(userMember());
    given(appointmentRepository.findById(APPOINTMENT_ID)).willReturn(Optional.of(appointment));

    assertThatThrownBy(() -> placeCandidateService.create(APPOINTMENT_ID, authUser, null, request))
        .isInstanceOf(BusinessException.class);
    verify(placeCandidateRepository, never()).save(any());
  }

  @Test
  void createThrowsBusinessExceptionWhenPlaceCandidateAlreadyExists() {
    AuthUser authUser = new AuthUser(USER_ID);
    Appointment appointment = appointment(AppointmentStatus.PLANNING);
    PlaceCandidateCreateRequest request = createRequest();
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willReturn(userMember());
    given(appointmentRepository.findById(APPOINTMENT_ID)).willReturn(Optional.of(appointment));
    given(placeCandidateRepository.existsByAppointmentIdAndKakaoPlaceId(APPOINTMENT_ID, "12345"))
        .willReturn(true);

    assertThatThrownBy(() -> placeCandidateService.create(APPOINTMENT_ID, authUser, null, request))
        .isInstanceOf(BusinessException.class);
    verify(placeCandidateRepository, never()).save(any());
  }

  @Test
  void findAllReturnsPlaceCandidatesWhenRequesterIsAppointmentMember() {
    AuthUser authUser = new AuthUser(USER_ID);
    Appointment appointment = appointment(AppointmentStatus.CLOSED);
    PlaceCandidate placeCandidate = placeCandidate();
    ReflectionTestUtils.setField(placeCandidate, "id", 1L);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willReturn(userMember());
    given(appointmentRepository.findById(APPOINTMENT_ID)).willReturn(Optional.of(appointment));
    given(placeCandidateRepository.findByAppointmentIdOrderByCreatedAtAscIdAsc(APPOINTMENT_ID))
        .willReturn(List.of(placeCandidate));

    List<PlaceCandidateResponse> responses =
        placeCandidateService.findAll(APPOINTMENT_ID, authUser, null);

    assertThat(responses).hasSize(1);
    assertThat(responses.getFirst().id()).isEqualTo(1L);
    assertThat(responses.getFirst().name()).isEqualTo("강남역");
  }

  @Test
  void deleteRemovesPlaceCandidateWhenRequesterAddedCandidate() {
    AuthUser authUser = new AuthUser(USER_ID);
    Appointment appointment = appointment(AppointmentStatus.PLANNING);
    AppointmentMember appointmentMember = userMember();
    PlaceCandidate placeCandidate = placeCandidate(MEMBER_ID);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willReturn(appointmentMember);
    given(appointmentRepository.findById(APPOINTMENT_ID)).willReturn(Optional.of(appointment));
    given(placeCandidateRepository.findByIdAndAppointmentId(PLACE_CANDIDATE_ID, APPOINTMENT_ID))
        .willReturn(Optional.of(placeCandidate));

    placeCandidateService.delete(APPOINTMENT_ID, PLACE_CANDIDATE_ID, authUser, null);

    verify(placeCandidateRepository).delete(placeCandidate);
  }

  @Test
  void deleteRemovesPlaceCandidateWhenRequesterIsHost() {
    AuthUser authUser = new AuthUser(USER_ID);
    Appointment appointment = appointment(AppointmentStatus.PLANNING);
    AppointmentMember hostMember = hostMember();
    PlaceCandidate placeCandidate = placeCandidate(OTHER_MEMBER_ID);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null)).willReturn(hostMember);
    given(appointmentRepository.findById(APPOINTMENT_ID)).willReturn(Optional.of(appointment));
    given(placeCandidateRepository.findByIdAndAppointmentId(PLACE_CANDIDATE_ID, APPOINTMENT_ID))
        .willReturn(Optional.of(placeCandidate));

    placeCandidateService.delete(APPOINTMENT_ID, PLACE_CANDIDATE_ID, authUser, null);

    verify(placeCandidateRepository).delete(placeCandidate);
  }

  @Test
  void deleteRemovesPlaceCandidateWhenGuestAddedCandidate() {
    Appointment appointment = appointment(AppointmentStatus.PLANNING);
    AppointmentMember appointmentMember = guestMember();
    PlaceCandidate placeCandidate = placeCandidate(MEMBER_ID);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, null, GUEST_TOKEN))
        .willReturn(appointmentMember);
    given(appointmentRepository.findById(APPOINTMENT_ID)).willReturn(Optional.of(appointment));
    given(placeCandidateRepository.findByIdAndAppointmentId(PLACE_CANDIDATE_ID, APPOINTMENT_ID))
        .willReturn(Optional.of(placeCandidate));

    placeCandidateService.delete(APPOINTMENT_ID, PLACE_CANDIDATE_ID, null, GUEST_TOKEN);

    verify(placeCandidateRepository).delete(placeCandidate);
  }

  @Test
  void deleteThrowsBusinessExceptionWhenRequesterDidNotAddCandidate() {
    AuthUser authUser = new AuthUser(USER_ID);
    Appointment appointment = appointment(AppointmentStatus.PLANNING);
    AppointmentMember appointmentMember = userMember();
    PlaceCandidate placeCandidate = placeCandidate(OTHER_MEMBER_ID);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willReturn(appointmentMember);
    given(appointmentRepository.findById(APPOINTMENT_ID)).willReturn(Optional.of(appointment));
    given(placeCandidateRepository.findByIdAndAppointmentId(PLACE_CANDIDATE_ID, APPOINTMENT_ID))
        .willReturn(Optional.of(placeCandidate));

    assertThatThrownBy(
            () -> placeCandidateService.delete(APPOINTMENT_ID, PLACE_CANDIDATE_ID, authUser, null))
        .isInstanceOf(BusinessException.class);
    verify(placeCandidateRepository, never()).delete(any());
  }

  @Test
  void deleteThrowsBusinessExceptionWhenGuestDidNotAddCandidate() {
    Appointment appointment = appointment(AppointmentStatus.PLANNING);
    AppointmentMember appointmentMember = guestMember();
    PlaceCandidate placeCandidate = placeCandidate(OTHER_MEMBER_ID);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, null, GUEST_TOKEN))
        .willReturn(appointmentMember);
    given(appointmentRepository.findById(APPOINTMENT_ID)).willReturn(Optional.of(appointment));
    given(placeCandidateRepository.findByIdAndAppointmentId(PLACE_CANDIDATE_ID, APPOINTMENT_ID))
        .willReturn(Optional.of(placeCandidate));

    assertThatThrownBy(
            () ->
                placeCandidateService.delete(APPOINTMENT_ID, PLACE_CANDIDATE_ID, null, GUEST_TOKEN))
        .isInstanceOf(BusinessException.class);
    verify(placeCandidateRepository, never()).delete(any());
  }

  @Test
  void deleteThrowsBusinessExceptionWhenAppointmentIsNotPlanning() {
    AuthUser authUser = new AuthUser(USER_ID);
    Appointment appointment = appointment(AppointmentStatus.CONFIRMED);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willReturn(userMember());
    given(appointmentRepository.findById(APPOINTMENT_ID)).willReturn(Optional.of(appointment));

    assertThatThrownBy(
            () -> placeCandidateService.delete(APPOINTMENT_ID, PLACE_CANDIDATE_ID, authUser, null))
        .isInstanceOf(BusinessException.class);
    verify(placeCandidateRepository, never()).delete(any());
  }

  @Test
  void deleteThrowsBusinessExceptionWhenPlaceCandidateDoesNotExist() {
    AuthUser authUser = new AuthUser(USER_ID);
    Appointment appointment = appointment(AppointmentStatus.PLANNING);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willReturn(userMember());
    given(appointmentRepository.findById(APPOINTMENT_ID)).willReturn(Optional.of(appointment));
    given(placeCandidateRepository.findByIdAndAppointmentId(PLACE_CANDIDATE_ID, APPOINTMENT_ID))
        .willReturn(Optional.empty());

    assertThatThrownBy(
            () -> placeCandidateService.delete(APPOINTMENT_ID, PLACE_CANDIDATE_ID, authUser, null))
        .isInstanceOf(BusinessException.class);
    verify(placeCandidateRepository, never()).delete(any());
  }

  private PlaceCandidateCreateRequest createRequest() {
    return new PlaceCandidateCreateRequest(
        "12345",
        "강남역",
        "서울 강남구 강남대로 396",
        "서울 강남구 강남대로 396",
        "지하철역",
        "https://place.map.kakao.com/12345",
        "02-123-4567",
        37.4979,
        127.0276);
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

  private AppointmentMember hostMember() {
    AppointmentMember appointmentMember =
        AppointmentMember.createHost(APPOINTMENT_ID, USER_ID, NOW);
    ReflectionTestUtils.setField(appointmentMember, "id", MEMBER_ID);
    return appointmentMember;
  }

  private AppointmentMember guestMember() {
    AppointmentMember appointmentMember =
        AppointmentMember.createGuest(APPOINTMENT_ID, "guest", "guest-token-hash", NOW);
    ReflectionTestUtils.setField(appointmentMember, "id", MEMBER_ID);
    return appointmentMember;
  }

  private PlaceCandidate placeCandidate() {
    return placeCandidate(MEMBER_ID);
  }

  private PlaceCandidate placeCandidate(Long addedByMemberId) {
    return PlaceCandidate.create(
        APPOINTMENT_ID,
        "12345",
        "강남역",
        "서울 강남구 강남대로 396",
        "서울 강남구 강남대로 396",
        "지하철역",
        "https://place.map.kakao.com/12345",
        "02-123-4567",
        37.4979,
        127.0276,
        addedByMemberId,
        NOW);
  }
}
