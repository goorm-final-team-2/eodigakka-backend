package com.eodigakka.domain.place;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceCandidateRepository extends JpaRepository<PlaceCandidate, Long> {

  boolean existsByAppointmentIdAndKakaoPlaceId(Long appointmentId, String kakaoPlaceId);

  List<PlaceCandidate> findByAppointmentIdOrderByCreatedAtAscIdAsc(Long appointmentId);
}
