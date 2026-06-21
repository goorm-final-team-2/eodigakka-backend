package com.eodigakka.domain.location;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberLocationServiceTest {

  private static final Long APPOINTMENT_ID = 10L;
  private static final Long USER_ID = 1L;
  private static final Long MEMBER_ID = 100L;
  private static final String GUEST_TOKEN = "guest-token";
  private static final Instant NOW = Instant.parse("2026-06-21T00:00:00Z");

  @Mock private AppointmentRepository appointmentRepository;
  @Mock private AppointmentMemberResolver appointmentMemberResolver;
  @Mock private MemberLocationRepository memberLocationRepository;

  private MemberLocationService memberLocationService;

  @BeforeEach
  void setUp() {
    memberLocationService =
        new MemberLocationService(
            appointmentRepository,
            appointmentMemberResolver,
            memberLocationRepository,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void updateMineCreatesLocationWhenUserHasNoLocation() {
    AuthUser authUser = new AuthUser(USER_ID);
    MemberLocationUpdateRequest request = updateRequest();
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willReturn(userMember());
    given(appointmentRepository.findById(APPOINTMENT_ID))
        .willReturn(Optional.of(appointment(AppointmentStatus.CONFIRMED)));
    given(memberLocationRepository.findByAppointmentIdAndMemberId(APPOINTMENT_ID, MEMBER_ID))
        .willReturn(Optional.empty());
    given(memberLocationRepository.save(any(MemberLocation.class)))
        .willAnswer(
            invocation -> {
              MemberLocation memberLocation = invocation.getArgument(0);
              ReflectionTestUtils.setField(memberLocation, "id", 1L);
              return memberLocation;
            });

    MemberLocationResponse response =
        memberLocationService.updateMine(APPOINTMENT_ID, authUser, null, request);

    assertThat(response.appointmentId()).isEqualTo(APPOINTMENT_ID);
    assertThat(response.memberId()).isEqualTo(MEMBER_ID);
    assertThat(response.latitude()).isEqualTo(37.4979);
    assertThat(response.longitude()).isEqualTo(127.0276);
    assertThat(response.accuracy()).isEqualTo(20.5);
    assertThat(response.updatedAt()).isEqualTo(NOW);
  }

  @Test
  void updateMineCreatesLocationWhenGuestHasNoLocation() {
    MemberLocationUpdateRequest request = updateRequest();
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, null, GUEST_TOKEN))
        .willReturn(guestMember());
    given(appointmentRepository.findById(APPOINTMENT_ID))
        .willReturn(Optional.of(appointment(AppointmentStatus.CONFIRMED)));
    given(memberLocationRepository.findByAppointmentIdAndMemberId(APPOINTMENT_ID, MEMBER_ID))
        .willReturn(Optional.empty());
    given(memberLocationRepository.save(any(MemberLocation.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    MemberLocationResponse response =
        memberLocationService.updateMine(APPOINTMENT_ID, null, GUEST_TOKEN, request);

    assertThat(response.memberId()).isEqualTo(MEMBER_ID);
    assertThat(response.updatedAt()).isEqualTo(NOW);
  }

  @Test
  void updateMineUpdatesExistingLocation() {
    AuthUser authUser = new AuthUser(USER_ID);
    MemberLocation memberLocation = memberLocation();
    MemberLocationUpdateRequest request = new MemberLocationUpdateRequest(37.5, 127.1, 10.0);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willReturn(userMember());
    given(appointmentRepository.findById(APPOINTMENT_ID))
        .willReturn(Optional.of(appointment(AppointmentStatus.CONFIRMED)));
    given(memberLocationRepository.findByAppointmentIdAndMemberId(APPOINTMENT_ID, MEMBER_ID))
        .willReturn(Optional.of(memberLocation));

    MemberLocationResponse response =
        memberLocationService.updateMine(APPOINTMENT_ID, authUser, null, request);

    assertThat(response.latitude()).isEqualTo(37.5);
    assertThat(response.longitude()).isEqualTo(127.1);
    assertThat(response.accuracy()).isEqualTo(10.0);
    assertThat(response.updatedAt()).isEqualTo(NOW);
    verify(memberLocationRepository, never()).save(any());
  }

  @Test
  void updateMineThrowsBusinessExceptionWhenAppointmentIsNotConfirmed() {
    AuthUser authUser = new AuthUser(USER_ID);
    MemberLocationUpdateRequest request = updateRequest();
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willReturn(userMember());
    given(appointmentRepository.findById(APPOINTMENT_ID))
        .willReturn(Optional.of(appointment(AppointmentStatus.PLANNING)));

    assertThatThrownBy(
            () -> memberLocationService.updateMine(APPOINTMENT_ID, authUser, null, request))
        .isInstanceOf(BusinessException.class);
    verify(memberLocationRepository, never()).save(any());
  }

  @Test
  void updateMineThrowsBusinessExceptionWhenRequesterIsNotAppointmentMember() {
    AuthUser authUser = new AuthUser(USER_ID);
    MemberLocationUpdateRequest request = updateRequest();
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willThrow(new BusinessException(ErrorCode.APPOINTMENT_MEMBER_NOT_FOUND));

    assertThatThrownBy(
            () -> memberLocationService.updateMine(APPOINTMENT_ID, authUser, null, request))
        .isInstanceOf(BusinessException.class);
    verify(memberLocationRepository, never()).save(any());
  }

  @Test
  void findAllReturnsLocationsWhenUserIsAppointmentMember() {
    AuthUser authUser = new AuthUser(USER_ID);
    MemberLocation memberLocation = memberLocation();
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willReturn(userMember());
    given(appointmentRepository.findById(APPOINTMENT_ID))
        .willReturn(Optional.of(appointment(AppointmentStatus.CONFIRMED)));
    given(memberLocationRepository.findByAppointmentIdOrderByUpdatedAtDescIdAsc(APPOINTMENT_ID))
        .willReturn(List.of(memberLocation));

    List<MemberLocationResponse> responses =
        memberLocationService.findAll(APPOINTMENT_ID, authUser, null);

    assertThat(responses).hasSize(1);
    assertThat(responses.getFirst().memberId()).isEqualTo(MEMBER_ID);
  }

  @Test
  void findAllReturnsLocationsWhenGuestIsAppointmentMember() {
    MemberLocation memberLocation = memberLocation();
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, null, GUEST_TOKEN))
        .willReturn(guestMember());
    given(appointmentRepository.findById(APPOINTMENT_ID))
        .willReturn(Optional.of(appointment(AppointmentStatus.CONFIRMED)));
    given(memberLocationRepository.findByAppointmentIdOrderByUpdatedAtDescIdAsc(APPOINTMENT_ID))
        .willReturn(List.of(memberLocation));

    List<MemberLocationResponse> responses =
        memberLocationService.findAll(APPOINTMENT_ID, null, GUEST_TOKEN);

    assertThat(responses).hasSize(1);
    assertThat(responses.getFirst().memberId()).isEqualTo(MEMBER_ID);
  }

  @Test
  void findAllThrowsBusinessExceptionWhenAppointmentIsNotConfirmed() {
    AuthUser authUser = new AuthUser(USER_ID);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willReturn(userMember());
    given(appointmentRepository.findById(APPOINTMENT_ID))
        .willReturn(Optional.of(appointment(AppointmentStatus.CLOSED)));

    assertThatThrownBy(() -> memberLocationService.findAll(APPOINTMENT_ID, authUser, null))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void findAllThrowsBusinessExceptionWhenRequesterIsNotAppointmentMember() {
    AuthUser authUser = new AuthUser(USER_ID);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willThrow(new BusinessException(ErrorCode.APPOINTMENT_MEMBER_NOT_FOUND));

    assertThatThrownBy(() -> memberLocationService.findAll(APPOINTMENT_ID, authUser, null))
        .isInstanceOf(BusinessException.class);
  }

  private MemberLocationUpdateRequest updateRequest() {
    return new MemberLocationUpdateRequest(37.4979, 127.0276, 20.5);
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

  private MemberLocation memberLocation() {
    MemberLocation memberLocation =
        MemberLocation.create(APPOINTMENT_ID, MEMBER_ID, 37.4979, 127.0276, 20.5, NOW);
    ReflectionTestUtils.setField(memberLocation, "id", 1L);
    return memberLocation;
  }
}
