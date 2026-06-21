package com.eodigakka.domain.location;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberLocationRepository extends JpaRepository<MemberLocation, Long> {

  Optional<MemberLocation> findByAppointmentIdAndMemberId(Long appointmentId, Long memberId);

  List<MemberLocation> findByAppointmentIdOrderByUpdatedAtDescIdAsc(Long appointmentId);
}
