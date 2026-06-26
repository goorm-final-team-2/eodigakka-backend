package com.eodigakka.domain.appointment;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestSessionRepository extends JpaRepository<GuestSession, Long> {

  Optional<GuestSession> findByTokenHash(String tokenHash);
}
