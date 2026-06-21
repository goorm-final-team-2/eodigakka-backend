package com.eodigakka.domain.appointment;

import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {

  private static final int MAX_INVITE_CODE_GENERATION_ATTEMPTS = 10;

  private final AppointmentRepository appointmentRepository;
  private final AppointmentMemberRepository appointmentMemberRepository;
  private final AppointmentAccessValidator appointmentAccessValidator;
  private final InviteCodeGenerator inviteCodeGenerator;
  private final Clock clock;

  @Autowired
  public AppointmentService(
      AppointmentRepository appointmentRepository,
      AppointmentMemberRepository appointmentMemberRepository,
      AppointmentAccessValidator appointmentAccessValidator,
      InviteCodeGenerator inviteCodeGenerator) {
    this(
        appointmentRepository,
        appointmentMemberRepository,
        appointmentAccessValidator,
        inviteCodeGenerator,
        Clock.systemUTC());
  }

  AppointmentService(
      AppointmentRepository appointmentRepository,
      AppointmentMemberRepository appointmentMemberRepository,
      AppointmentAccessValidator appointmentAccessValidator,
      InviteCodeGenerator inviteCodeGenerator,
      Clock clock) {
    this.appointmentRepository = appointmentRepository;
    this.appointmentMemberRepository = appointmentMemberRepository;
    this.appointmentAccessValidator = appointmentAccessValidator;
    this.inviteCodeGenerator = inviteCodeGenerator;
    this.clock = clock;
  }

  @Transactional
  public AppointmentResponse create(Long userId, AppointmentCreateRequest request) {
    Instant now = Instant.now(clock);
    Appointment appointment =
        appointmentRepository.save(
            Appointment.create(
                request.title(),
                request.appointmentDate(),
                request.appointmentTime(),
                request.description(),
                request.preferredArea(),
                request.notice(),
                userId,
                generateUniqueInviteCode(),
                now));
    appointmentMemberRepository.save(
        AppointmentMember.createHost(appointment.getId(), userId, now));
    return AppointmentResponse.from(appointment, AppointmentMemberRole.HOST);
  }

  @Transactional(readOnly = true)
  public List<AppointmentResponse> findMyAppointments(Long userId) {
    List<AppointmentMember> appointmentMembers = appointmentMemberRepository.findByUserId(userId);
    if (appointmentMembers.isEmpty()) {
      return List.of();
    }

    Map<Long, AppointmentMember> memberByAppointmentId =
        appointmentMembers.stream()
            .collect(
                Collectors.toMap(
                    AppointmentMember::getAppointmentId,
                    Function.identity(),
                    (first, second) -> first));

    return appointmentRepository.findByIdIn(memberByAppointmentId.keySet()).stream()
        .sorted(
            Comparator.comparing(Appointment::getAppointmentDate)
                .thenComparing(Appointment::getAppointmentTime)
                .thenComparing(Appointment::getId))
        .map(
            appointment ->
                AppointmentResponse.from(
                    appointment, memberByAppointmentId.get(appointment.getId()).getRole()))
        .toList();
  }

  @Transactional(readOnly = true)
  public AppointmentResponse findById(Long appointmentId, Long userId) {
    AppointmentMember appointmentMember =
        appointmentAccessValidator.validateMember(appointmentId, userId);
    Appointment appointment =
        appointmentRepository
            .findById(appointmentId)
            .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND));
    return AppointmentResponse.from(appointment, appointmentMember.getRole());
  }

  @Transactional
  public AppointmentResponse update(
      Long appointmentId, Long userId, AppointmentUpdateRequest request) {
    AppointmentMember appointmentMember =
        appointmentAccessValidator.validateHost(appointmentId, userId);
    Appointment appointment = getAppointment(appointmentId);
    appointment.update(
        request.title(),
        request.appointmentDate(),
        request.appointmentTime(),
        request.description(),
        request.preferredArea(),
        request.notice(),
        Instant.now(clock));
    return AppointmentResponse.from(appointment, appointmentMember.getRole());
  }

  @Transactional
  public void delete(Long appointmentId, Long userId) {
    appointmentAccessValidator.validateHost(appointmentId, userId);
    Appointment appointment = getAppointment(appointmentId);
    appointment.validatePlanning();
    appointmentRepository.delete(appointment);
  }

  private String generateUniqueInviteCode() {
    for (int attempt = 0; attempt < MAX_INVITE_CODE_GENERATION_ATTEMPTS; attempt++) {
      String inviteCode = inviteCodeGenerator.generate();
      if (!appointmentRepository.existsByInviteCode(inviteCode)) {
        return inviteCode;
      }
    }
    throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
  }

  private Appointment getAppointment(Long appointmentId) {
    return appointmentRepository
        .findById(appointmentId)
        .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND));
  }
}
