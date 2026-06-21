package com.eodigakka.domain.vote;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, Long> {

  Optional<Vote> findByAppointmentIdAndMemberId(Long appointmentId, Long memberId);
}
