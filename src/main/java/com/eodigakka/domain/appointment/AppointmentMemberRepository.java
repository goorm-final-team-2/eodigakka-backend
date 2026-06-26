package com.eodigakka.domain.appointment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentMemberRepository extends JpaRepository<AppointmentMember, Long> {

  Optional<AppointmentMember> findByAppointmentIdAndUserId(Long appointmentId, Long userId);

  List<AppointmentMember> findByUserId(Long userId);

  List<AppointmentMember> findByAppointmentIdOrderByJoinedAtAscIdAsc(Long appointmentId);

  boolean existsByAppointmentIdAndGuestName(Long appointmentId, String guestName);
}
