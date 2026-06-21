package com.eodigakka.domain.confirmedplace;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfirmedPlaceRepository extends JpaRepository<ConfirmedPlace, Long> {

  Optional<ConfirmedPlace> findByAppointmentId(Long appointmentId);
}
