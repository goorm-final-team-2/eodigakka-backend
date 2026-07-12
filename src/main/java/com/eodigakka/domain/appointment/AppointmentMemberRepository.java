package com.eodigakka.domain.appointment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentMemberRepository extends JpaRepository<AppointmentMember, Long> {

  Optional<AppointmentMember> findByAppointmentIdAndUserId(Long appointmentId, Long userId);

  Optional<AppointmentMember> findByAppointmentIdAndUserIdAndLeftAtIsNull(
      Long appointmentId, Long userId);

  Optional<AppointmentMember> findByAppointmentIdAndGuestName(Long appointmentId, String guestName);

  List<AppointmentMember> findByUserId(Long userId);

  List<AppointmentMember> findByUserIdAndLeftAtIsNull(Long userId);

  List<AppointmentMember> findByAppointmentIdOrderByJoinedAtAscIdAsc(Long appointmentId);

  List<AppointmentMember> findByAppointmentIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc(
      Long appointmentId);

  boolean existsByAppointmentIdAndGuestName(Long appointmentId, String guestName);

  boolean existsByAppointmentIdAndGuestNameAndLeftAtIsNull(Long appointmentId, String guestName);
}
