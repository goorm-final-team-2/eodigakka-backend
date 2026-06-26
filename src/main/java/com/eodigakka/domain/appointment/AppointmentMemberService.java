package com.eodigakka.domain.appointment;

import com.eodigakka.domain.user.User;
import com.eodigakka.domain.user.UserRepository;
import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import com.eodigakka.global.security.AuthUser;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentMemberService {

  private final AppointmentMemberRepository appointmentMemberRepository;
  private final AppointmentMemberResolver appointmentMemberResolver;
  private final UserRepository userRepository;

  public AppointmentMemberService(
      AppointmentMemberRepository appointmentMemberRepository,
      AppointmentMemberResolver appointmentMemberResolver,
      UserRepository userRepository) {
    this.appointmentMemberRepository = appointmentMemberRepository;
    this.appointmentMemberResolver = appointmentMemberResolver;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<AppointmentMemberResponse> findAll(
      Long appointmentId, AuthUser authUser, String guestSessionToken) {
    appointmentMemberResolver.resolve(appointmentId, authUser, guestSessionToken);

    List<AppointmentMember> appointmentMembers =
        appointmentMemberRepository.findByAppointmentIdOrderByJoinedAtAscIdAsc(appointmentId);
    Map<Long, User> usersById = findUsersById(appointmentMembers);

    return appointmentMembers.stream()
        .map(appointmentMember -> toResponse(appointmentMember, usersById))
        .toList();
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
