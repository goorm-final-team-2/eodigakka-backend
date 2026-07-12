package com.eodigakka.domain.appointment;

import com.eodigakka.domain.user.User;
import com.eodigakka.domain.user.UserRepository;
import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import com.eodigakka.global.security.AuthUser;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentMemberService {

  private final AppointmentMemberRepository appointmentMemberRepository;
  private final AppointmentMemberResolver appointmentMemberResolver;
  private final GuestSessionService guestSessionService;
  private final UserRepository userRepository;
  private final Clock clock;

  @Autowired
  public AppointmentMemberService(
      AppointmentMemberRepository appointmentMemberRepository,
      AppointmentMemberResolver appointmentMemberResolver,
      GuestSessionService guestSessionService,
      UserRepository userRepository) {
    this(
        appointmentMemberRepository,
        appointmentMemberResolver,
        guestSessionService,
        userRepository,
        Clock.systemUTC());
  }

  AppointmentMemberService(
      AppointmentMemberRepository appointmentMemberRepository,
      AppointmentMemberResolver appointmentMemberResolver,
      GuestSessionService guestSessionService,
      UserRepository userRepository,
      Clock clock) {
    this.appointmentMemberRepository = appointmentMemberRepository;
    this.appointmentMemberResolver = appointmentMemberResolver;
    this.guestSessionService = guestSessionService;
    this.userRepository = userRepository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<AppointmentMemberResponse> findAll(
      Long appointmentId, AuthUser authUser, String guestSessionToken) {
    appointmentMemberResolver.resolve(appointmentId, authUser, guestSessionToken);

    List<AppointmentMember> appointmentMembers =
        appointmentMemberRepository.findByAppointmentIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc(
            appointmentId);
    Map<Long, User> usersById = findUsersById(appointmentMembers);

    return appointmentMembers.stream()
        .map(appointmentMember -> toResponse(appointmentMember, usersById))
        .toList();
  }

  @Transactional
  public boolean leave(Long appointmentId, AuthUser authUser, String guestSessionToken) {
    AppointmentMember appointmentMember =
        appointmentMemberResolver.resolve(appointmentId, authUser, guestSessionToken);
    if (appointmentMember.isHost()) {
      throw new BusinessException(ErrorCode.APPOINTMENT_HOST_CANNOT_LEAVE);
    }

    appointmentMember.leave(Instant.now(clock));
    if (hasText(guestSessionToken)) {
      guestSessionService.revoke(guestSessionToken);
      return true;
    }
    return false;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private Map<Long, User> findUsersById(List<AppointmentMember> appointmentMembers) {
    List<Long> userIds =
        appointmentMembers.stream()
            .filter(
                appointmentMember ->
                    appointmentMember.getMemberType() == AppointmentMemberType.USER)
            .map(AppointmentMember::getUserId)
            .distinct()
            .toList();

    return userRepository.findAllById(userIds).stream()
        .collect(Collectors.toMap(User::getId, Function.identity()));
  }

  private AppointmentMemberResponse toResponse(
      AppointmentMember appointmentMember, Map<Long, User> usersById) {
    if (appointmentMember.getMemberType() == AppointmentMemberType.GUEST) {
      return AppointmentMemberResponse.fromGuest(appointmentMember);
    }

    User user = usersById.get(appointmentMember.getUserId());
    if (user == null) {
      throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }
    return AppointmentMemberResponse.fromUser(appointmentMember, user);
  }
}
