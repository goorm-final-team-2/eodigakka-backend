package com.eodigakka.domain.appointment;

import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import com.eodigakka.global.security.MessageDigestSupport;
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
  private final GuestTokenGenerator guestTokenGenerator;
  private final GuestSessionService guestSessionService;
  private final Clock clock;

  @Autowired
  public AppointmentService(
      AppointmentRepository appointmentRepository,
      AppointmentMemberRepository appointmentMemberRepository,
      AppointmentAccessValidator appointmentAccessValidator,
      InviteCodeGenerator inviteCodeGenerator,
      GuestTokenGenerator guestTokenGenerator,
      GuestSessionService guestSessionService) {
    this(
        appointmentRepository,
        appointmentMemberRepository,
        appointmentAccessValidator,
        inviteCodeGenerator,
        guestTokenGenerator,
        guestSessionService,
        Clock.systemUTC());
  }

  AppointmentService(
      AppointmentRepository appointmentRepository,
      AppointmentMemberRepository appointmentMemberRepository,
      AppointmentAccessValidator appointmentAccessValidator,
      InviteCodeGenerator inviteCodeGenerator,
      GuestTokenGenerator guestTokenGenerator,
      GuestSessionService guestSessionService,
      Clock clock) {
    this.appointmentRepository = appointmentRepository;
    this.appointmentMemberRepository = appointmentMemberRepository;
    this.appointmentAccessValidator = appointmentAccessValidator;
    this.inviteCodeGenerator = inviteCodeGenerator;
    this.guestTokenGenerator = guestTokenGenerator;
    this.guestSessionService = guestSessionService;
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
    return findByMember(appointmentId, appointmentMember);
  }

  @Transactional(readOnly = true)
  public AppointmentResponse findByMember(Long appointmentId, AppointmentMember appointmentMember) {
    Appointment appointment =
        appointmentRepository
            .findById(appointmentId)
            .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND));
    return AppointmentResponse.from(appointment, appointmentMember.getRole());
  }

  @Transactional
  public AppointmentResponse join(Long userId, AppointmentJoinRequest request) {
    Appointment appointment =
        appointmentRepository
            .findByInviteCode(request.inviteCode())
            .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_INVITE_CODE_NOT_FOUND));
    appointment.validateJoinable();

    return appointmentMemberRepository
        .findByAppointmentIdAndUserId(appointment.getId(), userId)
        .map(
            appointmentMember -> AppointmentResponse.from(appointment, appointmentMember.getRole()))
        .orElseGet(() -> joinAsMember(appointment, userId));
  }

  @Transactional(readOnly = true)
  public AppointmentInvitePreviewResponse findInvitePreview(String inviteCode) {
    Appointment appointment = getAppointmentByInviteCode(inviteCode);
    appointment.validateJoinable();
    return AppointmentInvitePreviewResponse.from(appointment);
  }

  @Transactional
  public GuestJoinResult joinAsGuest(GuestJoinRequest request) {
    Appointment appointment = getAppointmentByInviteCode(request.inviteCode());
    appointment.validateJoinable();
    if (appointmentMemberRepository.existsByAppointmentIdAndGuestName(
        appointment.getId(), request.guestName())) {
      throw new BusinessException(ErrorCode.GUEST_NAME_ALREADY_EXISTS);
    }

    // V2 schema still requires appointment_members.guest_token_hash for guest rows.
    String legacyGuestToken = guestTokenGenerator.generate();
    AppointmentMember appointmentMember =
        appointmentMemberRepository.save(
            AppointmentMember.createGuest(
                appointment.getId(),
                request.guestName(),
                MessageDigestSupport.sha256Hex(legacyGuestToken),
                Instant.now(clock)));
    GuestSessionIssue guestSessionIssue = guestSessionService.issue(appointmentMember);
    return new GuestJoinResult(
        GuestJoinResponse.from(appointment, appointmentMember), guestSessionIssue);
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
    appointment.validateDeletable();
    appointmentRepository.delete(appointment);
  }

  @Transactional
  public AppointmentResponse close(Long appointmentId, Long userId) {
    AppointmentMember appointmentMember =
        appointmentAccessValidator.validateHost(appointmentId, userId);
    Appointment appointment = getAppointment(appointmentId);
    appointment.close(Instant.now(clock));
    return AppointmentResponse.from(appointment, appointmentMember.getRole());
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

  private Appointment getAppointmentByInviteCode(String inviteCode) {
    return appointmentRepository
        .findByInviteCode(inviteCode)
        .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_INVITE_CODE_NOT_FOUND));
  }

  private AppointmentResponse joinAsMember(Appointment appointment, Long userId) {
    AppointmentMember appointmentMember =
        appointmentMemberRepository.save(
            AppointmentMember.createMember(appointment.getId(), userId, Instant.now(clock)));
    return AppointmentResponse.from(appointment, appointmentMember.getRole());
  }
}
