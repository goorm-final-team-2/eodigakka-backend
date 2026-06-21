package com.eodigakka.domain.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import com.eodigakka.global.security.MessageDigestSupport;
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
class AppointmentServiceTest {

  private static final Long USER_ID = 1L;
  private static final Long APPOINTMENT_ID = 10L;
  private static final Instant NOW = Instant.parse("2026-06-21T00:00:00Z");

  @Mock private AppointmentRepository appointmentRepository;
  @Mock private AppointmentMemberRepository appointmentMemberRepository;
  @Mock private AppointmentAccessValidator appointmentAccessValidator;
  @Mock private InviteCodeGenerator inviteCodeGenerator;
  @Mock private GuestTokenGenerator guestTokenGenerator;

  private AppointmentService appointmentService;

  @BeforeEach
  void setUp() {
    appointmentService =
        new AppointmentService(
            appointmentRepository,
            appointmentMemberRepository,
            appointmentAccessValidator,
            inviteCodeGenerator,
            guestTokenGenerator,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void createSavesAppointmentAndHostMember() {
    AppointmentCreateRequest request = createRequest();
    given(inviteCodeGenerator.generate()).willReturn("A7K2P9QX");
    given(appointmentRepository.existsByInviteCode("A7K2P9QX")).willReturn(false);
    given(appointmentRepository.save(any(Appointment.class)))
        .willAnswer(
            invocation -> {
              Appointment appointment = invocation.getArgument(0);
              ReflectionTestUtils.setField(appointment, "id", APPOINTMENT_ID);
              return appointment;
            });

    AppointmentResponse response = appointmentService.create(USER_ID, request);

    assertThat(response.id()).isEqualTo(APPOINTMENT_ID);
    assertThat(response.inviteCode()).isEqualTo("A7K2P9QX");
    assertThat(response.role()).isEqualTo(AppointmentMemberRole.HOST);

    ArgumentCaptor<AppointmentMember> memberCaptor =
        ArgumentCaptor.forClass(AppointmentMember.class);
    verify(appointmentMemberRepository).save(memberCaptor.capture());
    AppointmentMember hostMember = memberCaptor.getValue();
    assertThat(hostMember.getAppointmentId()).isEqualTo(APPOINTMENT_ID);
    assertThat(hostMember.getUserId()).isEqualTo(USER_ID);
    assertThat(hostMember.getRole()).isEqualTo(AppointmentMemberRole.HOST);
  }

  @Test
  void createRetriesInviteCodeWhenGeneratedCodeAlreadyExists() {
    AppointmentCreateRequest request = createRequest();
    given(inviteCodeGenerator.generate()).willReturn("DUPLICAT", "A7K2P9QX");
    given(appointmentRepository.existsByInviteCode("DUPLICAT")).willReturn(true);
    given(appointmentRepository.existsByInviteCode("A7K2P9QX")).willReturn(false);
    given(appointmentRepository.save(any(Appointment.class)))
        .willAnswer(
            invocation -> {
              Appointment appointment = invocation.getArgument(0);
              ReflectionTestUtils.setField(appointment, "id", APPOINTMENT_ID);
              return appointment;
            });

    AppointmentResponse response = appointmentService.create(USER_ID, request);

    assertThat(response.inviteCode()).isEqualTo("A7K2P9QX");
  }

  @Test
  void findMyAppointmentsReturnsAppointmentsJoinedByUser() {
    Appointment appointment = appointment(APPOINTMENT_ID, "강남 저녁 약속");
    AppointmentMember member =
        AppointmentMember.createUserMember(
            APPOINTMENT_ID, USER_ID, AppointmentMemberRole.MEMBER, NOW);
    given(appointmentMemberRepository.findByUserId(USER_ID)).willReturn(List.of(member));
    given(appointmentRepository.findByIdIn(any())).willReturn(List.of(appointment));

    List<AppointmentResponse> responses = appointmentService.findMyAppointments(USER_ID);

    assertThat(responses).hasSize(1);
    assertThat(responses.getFirst().id()).isEqualTo(APPOINTMENT_ID);
    assertThat(responses.getFirst().role()).isEqualTo(AppointmentMemberRole.MEMBER);
  }

  @Test
  void findByIdReturnsAppointmentWhenUserIsMember() {
    Appointment appointment = appointment(APPOINTMENT_ID, "강남 저녁 약속");
    AppointmentMember member =
        AppointmentMember.createUserMember(
            APPOINTMENT_ID, USER_ID, AppointmentMemberRole.MEMBER, NOW);
    given(appointmentAccessValidator.validateMember(APPOINTMENT_ID, USER_ID)).willReturn(member);
    given(appointmentRepository.findById(APPOINTMENT_ID)).willReturn(Optional.of(appointment));

    AppointmentResponse response = appointmentService.findById(APPOINTMENT_ID, USER_ID);

    assertThat(response.id()).isEqualTo(APPOINTMENT_ID);
    assertThat(response.role()).isEqualTo(AppointmentMemberRole.MEMBER);
  }

  @Test
  void findByIdThrowsBusinessExceptionWhenUserIsNotMember() {
    given(appointmentAccessValidator.validateMember(APPOINTMENT_ID, USER_ID))
        .willThrow(new BusinessException(ErrorCode.APPOINTMENT_MEMBER_NOT_FOUND));

    assertThatThrownBy(() -> appointmentService.findById(APPOINTMENT_ID, USER_ID))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void joinCreatesMemberWhenInviteCodeIsValidAndUserHasNotJoined() {
    Appointment appointment = appointment(APPOINTMENT_ID, "강남 저녁 약속");
    given(appointmentRepository.findByInviteCode("A7K2P9QX")).willReturn(Optional.of(appointment));
    given(appointmentMemberRepository.findByAppointmentIdAndUserId(APPOINTMENT_ID, USER_ID))
        .willReturn(Optional.empty());
    given(appointmentMemberRepository.save(any(AppointmentMember.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    AppointmentResponse response =
        appointmentService.join(USER_ID, new AppointmentJoinRequest("A7K2P9QX"));

    assertThat(response.id()).isEqualTo(APPOINTMENT_ID);
    assertThat(response.role()).isEqualTo(AppointmentMemberRole.MEMBER);
    ArgumentCaptor<AppointmentMember> memberCaptor =
        ArgumentCaptor.forClass(AppointmentMember.class);
    verify(appointmentMemberRepository).save(memberCaptor.capture());
    assertThat(memberCaptor.getValue().getAppointmentId()).isEqualTo(APPOINTMENT_ID);
    assertThat(memberCaptor.getValue().getUserId()).isEqualTo(USER_ID);
    assertThat(memberCaptor.getValue().getRole()).isEqualTo(AppointmentMemberRole.MEMBER);
  }

  @Test
  void joinReturnsExistingAppointmentWhenUserAlreadyJoined() {
    Appointment appointment = appointment(APPOINTMENT_ID, "강남 저녁 약속");
    AppointmentMember member =
        AppointmentMember.createUserMember(
            APPOINTMENT_ID, USER_ID, AppointmentMemberRole.MEMBER, NOW);
    given(appointmentRepository.findByInviteCode("A7K2P9QX")).willReturn(Optional.of(appointment));
    given(appointmentMemberRepository.findByAppointmentIdAndUserId(APPOINTMENT_ID, USER_ID))
        .willReturn(Optional.of(member));

    AppointmentResponse response =
        appointmentService.join(USER_ID, new AppointmentJoinRequest("A7K2P9QX"));

    assertThat(response.id()).isEqualTo(APPOINTMENT_ID);
    assertThat(response.role()).isEqualTo(AppointmentMemberRole.MEMBER);
    verify(appointmentMemberRepository, never()).save(any(AppointmentMember.class));
  }

  @Test
  void joinThrowsBusinessExceptionWhenInviteCodeDoesNotExist() {
    given(appointmentRepository.findByInviteCode("UNKNOWN")).willReturn(Optional.empty());

    assertThatThrownBy(
            () -> appointmentService.join(USER_ID, new AppointmentJoinRequest("UNKNOWN")))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void joinThrowsBusinessExceptionWhenAppointmentIsClosed() {
    Appointment appointment = appointment(APPOINTMENT_ID, "강남 저녁 약속");
    ReflectionTestUtils.setField(appointment, "status", AppointmentStatus.CLOSED);
    given(appointmentRepository.findByInviteCode("A7K2P9QX")).willReturn(Optional.of(appointment));

    assertThatThrownBy(
            () -> appointmentService.join(USER_ID, new AppointmentJoinRequest("A7K2P9QX")))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void findInvitePreviewReturnsPublicAppointmentPreview() {
    Appointment appointment = appointment(APPOINTMENT_ID, "강남 저녁 약속");
    given(appointmentRepository.findByInviteCode("A7K2P9QX")).willReturn(Optional.of(appointment));

    AppointmentInvitePreviewResponse response = appointmentService.findInvitePreview("A7K2P9QX");

    assertThat(response.title()).isEqualTo("강남 저녁 약속");
    assertThat(response.appointmentDate()).isEqualTo(LocalDate.of(2026, 7, 1));
    assertThat(response.appointmentTime()).isEqualTo(LocalTime.of(19, 0));
    assertThat(response.preferredArea()).isEqualTo("강남역");
    assertThat(response.status()).isEqualTo(AppointmentStatus.PLANNING);
  }

  @Test
  void findInvitePreviewThrowsBusinessExceptionWhenInviteCodeDoesNotExist() {
    given(appointmentRepository.findByInviteCode("UNKNOWN")).willReturn(Optional.empty());

    assertThatThrownBy(() -> appointmentService.findInvitePreview("UNKNOWN"))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void joinAsGuestCreatesGuestMemberAndReturnsRawGuestToken() {
    Appointment appointment = appointment(APPOINTMENT_ID, "강남 저녁 약속");
    given(appointmentRepository.findByInviteCode("A7K2P9QX")).willReturn(Optional.of(appointment));
    given(appointmentMemberRepository.existsByAppointmentIdAndGuestName(APPOINTMENT_ID, "철수"))
        .willReturn(false);
    given(guestTokenGenerator.generate()).willReturn("raw-guest-token");
    given(appointmentMemberRepository.save(any(AppointmentMember.class)))
        .willAnswer(
            invocation -> {
              AppointmentMember appointmentMember = invocation.getArgument(0);
              ReflectionTestUtils.setField(appointmentMember, "id", 100L);
              return appointmentMember;
            });

    GuestJoinResponse response =
        appointmentService.joinAsGuest(new GuestJoinRequest("A7K2P9QX", "철수"));

    assertThat(response.appointment().id()).isEqualTo(APPOINTMENT_ID);
    assertThat(response.appointment().role()).isEqualTo(AppointmentMemberRole.MEMBER);
    assertThat(response.guest().memberId()).isEqualTo(100L);
    assertThat(response.guest().guestName()).isEqualTo("철수");
    assertThat(response.guest().guestToken()).isEqualTo("raw-guest-token");

    ArgumentCaptor<AppointmentMember> memberCaptor =
        ArgumentCaptor.forClass(AppointmentMember.class);
    verify(appointmentMemberRepository).save(memberCaptor.capture());
    AppointmentMember savedMember = memberCaptor.getValue();
    assertThat(savedMember.getAppointmentId()).isEqualTo(APPOINTMENT_ID);
    assertThat(savedMember.getMemberType()).isEqualTo(AppointmentMemberType.GUEST);
    assertThat(savedMember.getRole()).isEqualTo(AppointmentMemberRole.MEMBER);
    assertThat(savedMember.getGuestName()).isEqualTo("철수");
    assertThat(savedMember.getGuestTokenHash())
        .isEqualTo(MessageDigestSupport.sha256Hex("raw-guest-token"));
  }

  @Test
  void joinAsGuestThrowsBusinessExceptionWhenGuestNameAlreadyExists() {
    Appointment appointment = appointment(APPOINTMENT_ID, "강남 저녁 약속");
    given(appointmentRepository.findByInviteCode("A7K2P9QX")).willReturn(Optional.of(appointment));
    given(appointmentMemberRepository.existsByAppointmentIdAndGuestName(APPOINTMENT_ID, "철수"))
        .willReturn(true);

    assertThatThrownBy(() -> appointmentService.joinAsGuest(new GuestJoinRequest("A7K2P9QX", "철수")))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void joinAsGuestThrowsBusinessExceptionWhenAppointmentIsClosed() {
    Appointment appointment = appointment(APPOINTMENT_ID, "강남 저녁 약속");
    ReflectionTestUtils.setField(appointment, "status", AppointmentStatus.CLOSED);
    given(appointmentRepository.findByInviteCode("A7K2P9QX")).willReturn(Optional.of(appointment));

    assertThatThrownBy(() -> appointmentService.joinAsGuest(new GuestJoinRequest("A7K2P9QX", "철수")))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void updateChangesAppointmentWhenUserIsHostAndAppointmentIsPlanning() {
    Appointment appointment = appointment(APPOINTMENT_ID, "강남 저녁 약속");
    AppointmentMember hostMember =
        AppointmentMember.createUserMember(
            APPOINTMENT_ID, USER_ID, AppointmentMemberRole.HOST, NOW);
    AppointmentUpdateRequest request =
        new AppointmentUpdateRequest(
            "홍대 점심 약속",
            LocalDate.of(2026, 7, 2),
            LocalTime.of(12, 30),
            "점심 먹을 장소 정하기",
            "홍대입구역",
            "우산 챙기기");
    given(appointmentAccessValidator.validateHost(APPOINTMENT_ID, USER_ID)).willReturn(hostMember);
    given(appointmentRepository.findById(APPOINTMENT_ID)).willReturn(Optional.of(appointment));

    AppointmentResponse response = appointmentService.update(APPOINTMENT_ID, USER_ID, request);

    assertThat(response.title()).isEqualTo("홍대 점심 약속");
    assertThat(response.appointmentDate()).isEqualTo(LocalDate.of(2026, 7, 2));
    assertThat(response.appointmentTime()).isEqualTo(LocalTime.of(12, 30));
    assertThat(response.role()).isEqualTo(AppointmentMemberRole.HOST);
  }

  @Test
  void updateThrowsBusinessExceptionWhenUserIsNotHost() {
    AppointmentUpdateRequest request = updateRequest();
    given(appointmentAccessValidator.validateHost(APPOINTMENT_ID, USER_ID))
        .willThrow(new BusinessException(ErrorCode.APPOINTMENT_HOST_REQUIRED));

    assertThatThrownBy(() -> appointmentService.update(APPOINTMENT_ID, USER_ID, request))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void updateThrowsBusinessExceptionWhenAppointmentIsConfirmed() {
    Appointment appointment = appointment(APPOINTMENT_ID, "강남 저녁 약속");
    ReflectionTestUtils.setField(appointment, "status", AppointmentStatus.CONFIRMED);
    AppointmentMember hostMember =
        AppointmentMember.createUserMember(
            APPOINTMENT_ID, USER_ID, AppointmentMemberRole.HOST, NOW);
    AppointmentUpdateRequest request = updateRequest();
    given(appointmentAccessValidator.validateHost(APPOINTMENT_ID, USER_ID)).willReturn(hostMember);
    given(appointmentRepository.findById(APPOINTMENT_ID)).willReturn(Optional.of(appointment));

    assertThatThrownBy(() -> appointmentService.update(APPOINTMENT_ID, USER_ID, request))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void deleteRemovesAppointmentWhenUserIsHostAndAppointmentIsPlanning() {
    Appointment appointment = appointment(APPOINTMENT_ID, "강남 저녁 약속");
    AppointmentMember hostMember =
        AppointmentMember.createUserMember(
            APPOINTMENT_ID, USER_ID, AppointmentMemberRole.HOST, NOW);
    given(appointmentAccessValidator.validateHost(APPOINTMENT_ID, USER_ID)).willReturn(hostMember);
    given(appointmentRepository.findById(APPOINTMENT_ID)).willReturn(Optional.of(appointment));

    appointmentService.delete(APPOINTMENT_ID, USER_ID);

    verify(appointmentRepository).delete(appointment);
  }

  @Test
  void deleteThrowsBusinessExceptionWhenUserIsNotHost() {
    given(appointmentAccessValidator.validateHost(APPOINTMENT_ID, USER_ID))
        .willThrow(new BusinessException(ErrorCode.APPOINTMENT_HOST_REQUIRED));

    assertThatThrownBy(() -> appointmentService.delete(APPOINTMENT_ID, USER_ID))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void deleteThrowsBusinessExceptionWhenAppointmentIsConfirmed() {
    Appointment appointment = appointment(APPOINTMENT_ID, "강남 저녁 약속");
    ReflectionTestUtils.setField(appointment, "status", AppointmentStatus.CONFIRMED);
    AppointmentMember hostMember =
        AppointmentMember.createUserMember(
            APPOINTMENT_ID, USER_ID, AppointmentMemberRole.HOST, NOW);
    given(appointmentAccessValidator.validateHost(APPOINTMENT_ID, USER_ID)).willReturn(hostMember);
    given(appointmentRepository.findById(APPOINTMENT_ID)).willReturn(Optional.of(appointment));

    assertThatThrownBy(() -> appointmentService.delete(APPOINTMENT_ID, USER_ID))
        .isInstanceOf(BusinessException.class);
  }

  private AppointmentCreateRequest createRequest() {
    return new AppointmentCreateRequest(
        "강남 저녁 약속", LocalDate.of(2026, 7, 1), LocalTime.of(19, 0), "저녁 먹을 장소 정하기", "강남역", "늦지 않기");
  }

  private AppointmentUpdateRequest updateRequest() {
    return new AppointmentUpdateRequest(
        "홍대 점심 약속",
        LocalDate.of(2026, 7, 2),
        LocalTime.of(12, 30),
        "점심 먹을 장소 정하기",
        "홍대입구역",
        "우산 챙기기");
  }

  private Appointment appointment(Long appointmentId, String title) {
    Appointment appointment =
        Appointment.create(
            title,
            LocalDate.of(2026, 7, 1),
            LocalTime.of(19, 0),
            "저녁 먹을 장소 정하기",
            "강남역",
            "늦지 않기",
            USER_ID,
            "A7K2P9QX",
            NOW);
    ReflectionTestUtils.setField(appointment, "id", appointmentId);
    return appointment;
  }
}
