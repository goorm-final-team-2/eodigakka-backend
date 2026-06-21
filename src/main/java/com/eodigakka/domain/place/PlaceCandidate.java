package com.eodigakka.domain.place;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "place_candidates")
public class PlaceCandidate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "appointment_id", nullable = false)
  private Long appointmentId;

  @Column(name = "kakao_place_id", nullable = false, length = 255)
  private String kakaoPlaceId;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(nullable = false, length = 500)
  private String address;

  @Column(name = "road_address", length = 500)
  private String roadAddress;

  @Column(length = 255)
  private String category;

  @Column(name = "place_url", length = 500)
  private String placeUrl;

  @Column(length = 50)
  private String phone;

  @Column(nullable = false)
  private double latitude;

  @Column(nullable = false)
  private double longitude;

  @Column(name = "added_by_member_id", nullable = false)
  private Long addedByMemberId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected PlaceCandidate() {}

  private PlaceCandidate(
      Long appointmentId,
      String kakaoPlaceId,
      String name,
      String address,
      String roadAddress,
      String category,
      String placeUrl,
      String phone,
      double latitude,
      double longitude,
      Long addedByMemberId,
      Instant createdAt) {
    this.appointmentId = appointmentId;
    this.kakaoPlaceId = kakaoPlaceId;
    this.name = name;
    this.address = address;
    this.roadAddress = roadAddress;
    this.category = category;
    this.placeUrl = placeUrl;
    this.phone = phone;
    this.latitude = latitude;
    this.longitude = longitude;
    this.addedByMemberId = addedByMemberId;
    this.createdAt = createdAt;
  }

  public static PlaceCandidate create(
      Long appointmentId,
      String kakaoPlaceId,
      String name,
      String address,
      String roadAddress,
      String category,
      String placeUrl,
      String phone,
      double latitude,
      double longitude,
      Long addedByMemberId,
      Instant createdAt) {
    return new PlaceCandidate(
        appointmentId,
        kakaoPlaceId,
        name,
        address,
        roadAddress,
        category,
        placeUrl,
        phone,
        latitude,
        longitude,
        addedByMemberId,
        createdAt);
  }

  public Long getId() {
    return id;
  }

  public Long getAppointmentId() {
    return appointmentId;
  }

  public String getKakaoPlaceId() {
    return kakaoPlaceId;
  }

  public String getName() {
    return name;
  }

  public String getAddress() {
    return address;
  }

  public String getRoadAddress() {
    return roadAddress;
  }

  public String getCategory() {
    return category;
  }

  public String getPlaceUrl() {
    return placeUrl;
  }

  public String getPhone() {
    return phone;
  }

  public double getLatitude() {
    return latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public Long getAddedByMemberId() {
    return addedByMemberId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
