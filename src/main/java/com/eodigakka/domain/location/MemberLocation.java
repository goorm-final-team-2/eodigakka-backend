package com.eodigakka.domain.location;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "member_locations")
public class MemberLocation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "appointment_id", nullable = false)
  private Long appointmentId;

  @Column(name = "member_id", nullable = false)
  private Long memberId;

  @Column(nullable = false)
  private double latitude;

  @Column(nullable = false)
  private double longitude;

  @Column(nullable = false)
  private double accuracy;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected MemberLocation() {}

  private MemberLocation(
      Long appointmentId,
      Long memberId,
      double latitude,
      double longitude,
      double accuracy,
      Instant updatedAt) {
    this.appointmentId = appointmentId;
    this.memberId = memberId;
    this.latitude = latitude;
    this.longitude = longitude;
    this.accuracy = accuracy;
    this.updatedAt = updatedAt;
  }

  public static MemberLocation create(
      Long appointmentId,
      Long memberId,
      double latitude,
      double longitude,
      double accuracy,
      Instant updatedAt) {
    return new MemberLocation(appointmentId, memberId, latitude, longitude, accuracy, updatedAt);
  }

  public void update(double latitude, double longitude, double accuracy, Instant updatedAt) {
    this.latitude = latitude;
    this.longitude = longitude;
    this.accuracy = accuracy;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public Long getAppointmentId() {
    return appointmentId;
  }

  public Long getMemberId() {
    return memberId;
  }

  public double getLatitude() {
    return latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public double getAccuracy() {
    return accuracy;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
