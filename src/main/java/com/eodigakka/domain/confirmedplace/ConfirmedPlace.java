package com.eodigakka.domain.confirmedplace;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "confirmed_places")
public class ConfirmedPlace {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "appointment_id", nullable = false)
  private Long appointmentId;

  @Column(name = "place_candidate_id", nullable = false)
  private Long placeCandidateId;

  @Column(name = "confirmed_by_user_id", nullable = false)
  private Long confirmedByUserId;

  @Column(name = "confirmed_at", nullable = false, updatable = false)
  private Instant confirmedAt;

  protected ConfirmedPlace() {}

  private ConfirmedPlace(
      Long appointmentId, Long placeCandidateId, Long confirmedByUserId, Instant confirmedAt) {
    this.appointmentId = appointmentId;
    this.placeCandidateId = placeCandidateId;
    this.confirmedByUserId = confirmedByUserId;
    this.confirmedAt = confirmedAt;
  }

  public static ConfirmedPlace create(
      Long appointmentId, Long placeCandidateId, Long confirmedByUserId, Instant confirmedAt) {
    return new ConfirmedPlace(appointmentId, placeCandidateId, confirmedByUserId, confirmedAt);
  }

  public Long getId() {
    return id;
  }

  public Long getAppointmentId() {
    return appointmentId;
  }

  public Long getPlaceCandidateId() {
    return placeCandidateId;
  }

  public Long getConfirmedByUserId() {
    return confirmedByUserId;
  }

  public Instant getConfirmedAt() {
    return confirmedAt;
  }
}
