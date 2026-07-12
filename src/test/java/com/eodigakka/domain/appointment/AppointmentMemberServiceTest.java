package com.eodigakka.domain.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.eodigakka.domain.user.SocialProvider;
import com.eodigakka.domain.user.User;
import com.eodigakka.domain.user.UserRepository;
import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import com.eodigakka.global.security.AuthUser;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AppointmentMemberServiceTest {

  private static final Long APPOINTMENT_ID = 10L;
  private static final Long HOST_USER_ID = 1L;
  private static final Long MEMBER_USER_ID = 2L;
  private static final String GUEST_TOKEN = "guest-token";
  private static final Instant NOW = Instant.parse("2026-06-21T00:00:00Z");

  @Mock private AppointmentMemberRepository appointmentMemberRepository;
  @Mock private AppointmentMemberResolver appointmentMemberResolver;
  @Mock private GuestSessionService guestSessionService;
  @Mock private UserRepository userRepository;

  private AppointmentMemberService appointmentMemberService;

  @BeforeEach
  void setUp() {
    appointmentMemberService =
        new AppointmentMemberService(
            appointmentMemberRepository,
            appointmentMemberResolver,
            guestSessionService,
            userRepository,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void findAllReturnsUserAndGuestMembersWhenRequesterIsUserMember() {
    AuthUser authUser = new AuthUser(HOST_USER_ID);
    AppointmentMember hostMember = userMember(1L, HOST_USER_ID, AppointmentMemberRole.HOST, NOW);
    AppointmentMember userMember =
        userMember(2L, MEMBER_USER_ID, AppointmentMemberRole.MEMBER, NOW.plusSeconds(10));
    AppointmentMember guestMember = guestMember(3L, "철수", NOW.plusSeconds(20));
    User host = user(HOST_USER_ID, "방장", "https://example.com/host.png");
    User member = user(MEMBER_USER_ID, "참여자", "https://example.com/member.png");
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null)).willReturn(hostMember);
    given(
            appointmentMemberRepository.findByAppointmentIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc(
                APPOINTMENT_ID))
        .willReturn(List.of(hostMember, userMember, guestMember));
    given(userRepository.findAllById(List.of(HOST_USER_ID, MEMBER_USER_ID)))
        .willReturn(List.of(host, member));

    List<AppointmentMemberResponse> responses =
        appointmentMemberService.findAll(APPOINTMENT_ID, authUser, null);

    assertThat(responses)
        .extracting(AppointmentMemberResponse::memberId)
        .containsExactly(1L, 2L, 3L);
    assertThat(responses.get(0).memberType()).isEqualTo(AppointmentMemberType.USER);
    assertThat(responses.get(0).role()).isEqualTo(AppointmentMemberRole.HOST);
    assertThat(responses.get(0).displayName()).isEqualTo("방장");
    assertThat(responses.get(0).profileImage()).isEqualTo("https://example.com/host.png");
    assertThat(responses.get(1).displayName()).isEqualTo("참여자");
    assertThat(responses.get(2).memberType()).isEqualTo(AppointmentMemberType.GUEST);
    assertThat(responses.get(2).role()).isEqualTo(AppointmentMemberRole.MEMBER);
    assertThat(responses.get(2).displayName()).isEqualTo("철수");
    assertThat(responses.get(2).profileImage()).isNull();
  }

  @Test
  void findAllReturnsMembersWhenRequesterIsGuestMember() {
    AppointmentMember guestMember = guestMember(3L, "철수", NOW.plusSeconds(20));
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, null, GUEST_TOKEN))
        .willReturn(guestMember);
    given(
            appointmentMemberRepository.findByAppointmentIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc(
                APPOINTMENT_ID))
        .willReturn(List.of(guestMember));
    given(userRepository.findAllById(List.of())).willReturn(List.of());

    List<AppointmentMemberResponse> responses =
        appointmentMemberService.findAll(APPOINTMENT_ID, null, GUEST_TOKEN);

    assertThat(responses).hasSize(1);
    assertThat(responses.getFirst().memberId()).isEqualTo(3L);
    assertThat(responses.getFirst().memberType()).isEqualTo(AppointmentMemberType.GUEST);
    assertThat(responses.getFirst().displayName()).isEqualTo("철수");
  }

  @Test
  void findAllThrowsBusinessExceptionWhenRequesterIsNotAppointmentMember() {
    AuthUser authUser = new AuthUser(HOST_USER_ID);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willThrow(new BusinessException(ErrorCode.APPOINTMENT_MEMBER_NOT_FOUND));

    assertThatThrownBy(() -> appointmentMemberService.findAll(APPOINTMENT_ID, authUser, null))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void findAllThrowsBusinessExceptionWhenUserMemberHasNoUser() {
    AuthUser authUser = new AuthUser(HOST_USER_ID);
    AppointmentMember hostMember = userMember(1L, HOST_USER_ID, AppointmentMemberRole.HOST, NOW);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null)).willReturn(hostMember);
    given(
            appointmentMemberRepository.findByAppointmentIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc(
                APPOINTMENT_ID))
        .willReturn(List.of(hostMember));
    given(userRepository.findAllById(List.of(HOST_USER_ID))).willReturn(List.of());

    assertThatThrownBy(() -> appointmentMemberService.findAll(APPOINTMENT_ID, authUser, null))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void leaveMarksUserMemberAsLeft() {
    AuthUser authUser = new AuthUser(MEMBER_USER_ID);
    AppointmentMember member =
        userMember(2L, MEMBER_USER_ID, AppointmentMemberRole.MEMBER, NOW.minusSeconds(10));
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null)).willReturn(member);

    boolean guestLeft = appointmentMemberService.leave(APPOINTMENT_ID, authUser, null);

    assertThat(guestLeft).isFalse();
    assertThat(member.getLeftAt()).isEqualTo(NOW);
  }

  @Test
  void leaveRevokesGuestSessionAndMarksGuestMemberAsLeft() {
    AppointmentMember guestMember = guestMember(3L, "철수", NOW.minusSeconds(10));
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, null, GUEST_TOKEN))
        .willReturn(guestMember);

    boolean guestLeft = appointmentMemberService.leave(APPOINTMENT_ID, null, GUEST_TOKEN);

    assertThat(guestLeft).isTrue();
    assertThat(guestMember.getLeftAt()).isEqualTo(NOW);
    verify(guestSessionService).revoke(GUEST_TOKEN);
  }

  @Test
  void leaveThrowsBusinessExceptionWhenRequesterIsHost() {
    AuthUser authUser = new AuthUser(HOST_USER_ID);
    AppointmentMember hostMember =
        userMember(1L, HOST_USER_ID, AppointmentMemberRole.HOST, NOW.minusSeconds(10));
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null)).willReturn(hostMember);

    assertThatThrownBy(() -> appointmentMemberService.leave(APPOINTMENT_ID, authUser, null))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.APPOINTMENT_HOST_CANNOT_LEAVE);
  }

  private AppointmentMember userMember(
      Long memberId, Long userId, AppointmentMemberRole role, Instant joinedAt) {
    AppointmentMember appointmentMember =
        AppointmentMember.createUserMember(APPOINTMENT_ID, userId, role, joinedAt);
    ReflectionTestUtils.setField(appointmentMember, "id", memberId);
    return appointmentMember;
  }

  private AppointmentMember guestMember(Long memberId, String guestName, Instant joinedAt) {
    AppointmentMember appointmentMember =
        AppointmentMember.createGuest(APPOINTMENT_ID, guestName, "guest-token-hash", joinedAt);
    ReflectionTestUtils.setField(appointmentMember, "id", memberId);
    return appointmentMember;
  }

  private User user(Long userId, String nickname, String profileImage) {
    User user = User.create(SocialProvider.KAKAO, "social-" + userId, nickname, profileImage);
    ReflectionTestUtils.setField(user, "id", userId);
    return user;
  }
}
