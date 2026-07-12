package com.eodigakka.domain.appointment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

  boolean existsByInviteCode(String inviteCode);

  Optional<Appointment> findByInviteCode(String inviteCode);

  List<Appointment> findByIdIn(Collection<Long> ids);

  @Query(
      """
      SELECT a
      FROM Appointment a
      WHERE a.status = :status
        AND (
          a.appointmentDate < :cutoffDate
          OR (a.appointmentDate = :cutoffDate AND a.appointmentTime <= :cutoffTime)
        )
      """)
  List<Appointment> findAutoCloseTargets(
      @Param("status") AppointmentStatus status,
      @Param("cutoffDate") LocalDate cutoffDate,
      @Param("cutoffTime") LocalTime cutoffTime);
}
