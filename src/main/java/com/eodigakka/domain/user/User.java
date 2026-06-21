package com.eodigakka.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "users",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_users_social_account",
            columnNames = {"social_provider", "social_id"}))
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "social_provider", nullable = false, length = 20)
  private SocialProvider socialProvider;

  @Column(name = "social_id", nullable = false, length = 255)
  private String socialId;

  @Column(nullable = false, length = 50)
  private String nickname;

  @Column(name = "profile_image", length = 500)
  private String profileImage;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected User() {}

  private User(
      SocialProvider socialProvider, String socialId, String nickname, String profileImage) {
    this.socialProvider = socialProvider;
    this.socialId = socialId;
    this.nickname = nickname;
    this.profileImage = profileImage;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  public static User create(
      SocialProvider socialProvider, String socialId, String nickname, String profileImage) {
    return new User(socialProvider, socialId, nickname, profileImage);
  }

  public void updateProfile(String nickname, String profileImage) {
    this.nickname = nickname;
    this.profileImage = profileImage;
    this.updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public SocialProvider getSocialProvider() {
    return socialProvider;
  }

  public String getSocialId() {
    return socialId;
  }

  public String getNickname() {
    return nickname;
  }

  public String getProfileImage() {
    return profileImage;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
