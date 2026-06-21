package com.eodigakka.domain.place;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceCandidateRepository extends JpaRepository<PlaceCandidate, Long> {

  boolean existsByAppointmentIdAndKakaoPlaceId(Long appointmentId, String kakaoPlaceId);

  Optional<PlaceCandidate> findByIdAndAppointmentId(Long id, Long appointmentId);

  List<PlaceCandidate> findByAppointmentIdOrderByCreatedAtAscIdAsc(Long appointmentId);
}
