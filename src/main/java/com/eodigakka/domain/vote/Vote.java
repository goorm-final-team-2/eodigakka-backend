package com.eodigakka.domain.vote;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "votes")
public class Vote {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "appointment_id", nullable = false)
  private Long appointmentId;

  @Column(name = "place_candidate_id", nullable = false)
  private Long placeCandidateId;

  @Column(name = "member_id", nullable = false)
  private Long memberId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Vote() {}

  private Vote(Long appointmentId, Long placeCandidateId, Long memberId, Instant createdAt) {
    this.appointmentId = appointmentId;
    this.placeCandidateId = placeCandidateId;
    this.memberId = memberId;
    this.createdAt = createdAt;
  }

  public static Vote create(
      Long appointmentId, Long placeCandidateId, Long memberId, Instant createdAt) {
    return new Vote(appointmentId, placeCandidateId, memberId, createdAt);
  }

  public void changePlaceCandidate(Long placeCandidateId) {
    this.placeCandidateId = placeCandidateId;
  }

  public boolean hasPlaceCandidate(Long placeCandidateId) {
    return this.placeCandidateId.equals(placeCandidateId);
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

  public Long getMemberId() {
    return memberId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
