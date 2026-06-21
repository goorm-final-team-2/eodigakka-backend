package com.eodigakka.domain.confirmedplace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.eodigakka.domain.appointment.Appointment;
import com.eodigakka.domain.appointment.AppointmentAccessValidator;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ConfirmedPlaceServiceTest {

  private static final Long APPOINTMENT_ID = 10L;
  private static final Long USER_ID = 1L;
  private static final Long MEMBER_ID = 100L;
  private static final Long PLACE_CANDIDATE_ID = 1000L;
  private static final String GUEST_TOKEN = "guest-token";
  private static final Instant NOW = Instant.parse("2026-06-21T00:00:00Z");

  @Mock private AppointmentRepository appointmentRepository;
  @Mock private AppointmentAccessValidator appointmentAccessValidator;
  @Mock private AppointmentMemberResolver appointmentMemberResolver;
  @Mock private PlaceCandidateRepository placeCandidateRepository;
  @Mock private ConfirmedPlaceRepository confirmedPlaceRepository;

  private ConfirmedPlaceService confirmedPlaceService;

  @BeforeEach
  void setUp() {
    confirmedPlaceService =
        new ConfirmedPlaceService(
            appointmentRepository,
            appointmentAccessValidator,
            appointmentMemberResolver,
            placeCandidateRepository,
            confirmedPlaceRepository,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void confirmSavesConfirmedPlaceWhenUserIsHostAndAppointmentIsPlanning() {
    Appointment appointment = appointment(AppointmentStatus.PLANNING);
    ConfirmedPlaceRequest request = new ConfirmedPlaceRequest(PLACE_CANDIDATE_ID);
    given(appointmentAccessValidator.validateHost(APPOINTMENT_ID, USER_ID))
        .willReturn(hostMember());
    given(appointmentRepository.findById(APPOINTMENT_ID)).willReturn(Optional.of(appointment));
    given(placeCandidateRepository.findByIdAndAppointmentId(PLACE_CANDIDATE_ID, APPOINTMENT_ID))
        .willReturn(Optional.of(placeCandidate()));
    given(confirmedPlaceRepository.save(any(ConfirmedPlace.class)))
        .willAnswer(
            invocation -> {
              ConfirmedPlace confirmedPlace = invocation.getArgument(0);
              ReflectionTestUtils.setField(confirmedPlace, "id", 1L);
              return confirmedPlace;
            });

    ConfirmedPlaceResponse response =
        confirmedPlaceService.confirm(APPOINTMENT_ID, USER_ID, request);

    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.appointmentId()).isEqualTo(APPOINTMENT_ID);
    assertThat(response.placeCandidateId()).isEqualTo(PLACE_CANDIDATE_ID);
    assertThat(response.confirmedByUserId()).isEqualTo(USER_ID);
    assertThat(response.confirmedAt()).isEqualTo(NOW);
    assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
  }

  @Test
  void confirmThrowsBusinessExceptionWhenUserIsNotHost() {
    ConfirmedPlaceRequest request = new ConfirmedPlaceRequest(PLACE_CANDIDATE_ID);
    given(appointmentAccessValidator.validateHost(APPOINTMENT_ID, USER_ID))
        .willThrow(new BusinessException(ErrorCode.APPOINTMENT_HOST_REQUIRED));

    assertThatThrownBy(() -> confirmedPlaceService.confirm(APPOINTMENT_ID, USER_ID, request))
        .isInstanceOf(BusinessException.class);
    verify(confirmedPlaceRepository, never()).save(any());
  }

  @Test
  void confirmThrowsBusinessExceptionWhenAppointmentIsNotPlanning() {
    Appointment appointment = appointment(AppointmentStatus.CONFIRMED);
    ConfirmedPlaceRequest request = new ConfirmedPlaceRequest(PLACE_CANDIDATE_ID);
    given(appointmentAccessValidator.validateHost(APPOINTMENT_ID, USER_ID))
        .willReturn(hostMember());
    given(appointmentRepository.findById(APPOINTMENT_ID)).willReturn(Optional.of(appointment));

    assertThatThrownBy(() -> confirmedPlaceService.confirm(APPOINTMENT_ID, USER_ID, request))
        .isInstanceOf(BusinessException.class);
    verify(confirmedPlaceRepository, never()).save(any());
  }

  @Test
  void confirmThrowsBusinessExceptionWhenPlaceCandidateDoesNotExistInAppointment() {
    Appointment appointment = appointment(AppointmentStatus.PLANNING);
    ConfirmedPlaceRequest request = new ConfirmedPlaceRequest(PLACE_CANDIDATE_ID);
    given(appointmentAccessValidator.validateHost(APPOINTMENT_ID, USER_ID))
        .willReturn(hostMember());
    given(appointmentRepository.findById(APPOINTMENT_ID)).willReturn(Optional.of(appointment));
    given(placeCandidateRepository.findByIdAndAppointmentId(PLACE_CANDIDATE_ID, APPOINTMENT_ID))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> confirmedPlaceService.confirm(APPOINTMENT_ID, USER_ID, request))
        .isInstanceOf(BusinessException.class);
    verify(confirmedPlaceRepository, never()).save(any());
  }

  @Test
  void findReturnsConfirmedPlaceWhenUserIsAppointmentMember() {
    AuthUser authUser = new AuthUser(USER_ID);
    ConfirmedPlace confirmedPlace = confirmedPlace();
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null)).willReturn(member());
    given(appointmentRepository.findById(APPOINTMENT_ID))
        .willReturn(Optional.of(appointment(AppointmentStatus.CONFIRMED)));
    given(confirmedPlaceRepository.findByAppointmentId(APPOINTMENT_ID))
        .willReturn(Optional.of(confirmedPlace));

    ConfirmedPlaceResponse response = confirmedPlaceService.find(APPOINTMENT_ID, authUser, null);

    assertThat(response.appointmentId()).isEqualTo(APPOINTMENT_ID);
    assertThat(response.placeCandidateId()).isEqualTo(PLACE_CANDIDATE_ID);
  }

  @Test
  void findReturnsConfirmedPlaceWhenGuestIsAppointmentMember() {
    ConfirmedPlace confirmedPlace = confirmedPlace();
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, null, GUEST_TOKEN))
        .willReturn(guestMember());
    given(appointmentRepository.findById(APPOINTMENT_ID))
        .willReturn(Optional.of(appointment(AppointmentStatus.CONFIRMED)));
    given(confirmedPlaceRepository.findByAppointmentId(APPOINTMENT_ID))
        .willReturn(Optional.of(confirmedPlace));

    ConfirmedPlaceResponse response = confirmedPlaceService.find(APPOINTMENT_ID, null, GUEST_TOKEN);

    assertThat(response.appointmentId()).isEqualTo(APPOINTMENT_ID);
    assertThat(response.placeCandidateId()).isEqualTo(PLACE_CANDIDATE_ID);
  }

  @Test
  void findThrowsBusinessExceptionWhenRequesterIsNotAppointmentMember() {
    AuthUser authUser = new AuthUser(USER_ID);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willThrow(new BusinessException(ErrorCode.APPOINTMENT_MEMBER_NOT_FOUND));

    assertThatThrownBy(() -> confirmedPlaceService.find(APPOINTMENT_ID, authUser, null))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void findThrowsBusinessExceptionWhenConfirmedPlaceDoesNotExist() {
    AuthUser authUser = new AuthUser(USER_ID);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null)).willReturn(member());
    given(appointmentRepository.findById(APPOINTMENT_ID))
        .willReturn(Optional.of(appointment(AppointmentStatus.PLANNING)));
    given(confirmedPlaceRepository.findByAppointmentId(APPOINTMENT_ID))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> confirmedPlaceService.find(APPOINTMENT_ID, authUser, null))
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

  private AppointmentMember hostMember() {
    AppointmentMember appointmentMember =
        AppointmentMember.createHost(APPOINTMENT_ID, USER_ID, NOW);
    ReflectionTestUtils.setField(appointmentMember, "id", MEMBER_ID);
    return appointmentMember;
  }

  private AppointmentMember member() {
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

  private PlaceCandidate placeCandidate() {
    PlaceCandidate placeCandidate =
        PlaceCandidate.create(
            APPOINTMENT_ID,
            PLACE_CANDIDATE_ID.toString(),
            "강남역",
            "서울 강남구 강남대로 396",
            "서울 강남구 강남대로 396",
            "지하철역",
            "https://place.map.kakao.com/" + PLACE_CANDIDATE_ID,
            "02-123-4567",
            37.4979,
            127.0276,
            MEMBER_ID,
            NOW);
    ReflectionTestUtils.setField(placeCandidate, "id", PLACE_CANDIDATE_ID);
    return placeCandidate;
  }

  private ConfirmedPlace confirmedPlace() {
    ConfirmedPlace confirmedPlace =
        ConfirmedPlace.create(APPOINTMENT_ID, PLACE_CANDIDATE_ID, USER_ID, NOW);
    ReflectionTestUtils.setField(confirmedPlace, "id", 1L);
    return confirmedPlace;
  }
}
