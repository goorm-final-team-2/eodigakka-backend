package com.eodigakka.domain.appointment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "guest_sessions")
public class GuestSession {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "appointment_member_id", nullable = false)
  private AppointmentMember appointmentMember;

  @Column(name = "token_hash", nullable = false, unique = true, length = 255)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  protected GuestSession() {}

  private GuestSession(
      AppointmentMember appointmentMember, String tokenHash, Instant expiresAt, Instant now) {
    this.appointmentMember = appointmentMember;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
    this.createdAt = now;
  }

  public static GuestSession create(
      AppointmentMember appointmentMember, String tokenHash, Instant expiresAt, Instant now) {
    return new GuestSession(appointmentMember, tokenHash, expiresAt, now);
  }

  public boolean isValid(Instant now) {
    return revokedAt == null && expiresAt.isAfter(now);
  }

  public void recordUsedAt(Instant now) {
    this.lastUsedAt = now;
  }

  public void revoke(Instant now) {
    this.revokedAt = now;
  }

  public AppointmentMember getAppointmentMember() {
    return appointmentMember;
  }
}
