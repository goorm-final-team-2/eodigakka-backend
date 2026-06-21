package com.eodigakka.domain.appointment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

  boolean existsByInviteCode(String inviteCode);

  Optional<Appointment> findByInviteCode(String inviteCode);

  List<Appointment> findByIdIn(Collection<Long> ids);
}
