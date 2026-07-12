package com.eodigakka.domain.appointment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "appointment_members")
public class AppointmentMember {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "appointment_id", nullable = false)
  private Long appointmentId;

  @Column(name = "user_id")
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "member_type", nullable = false, length = 20)
  private AppointmentMemberType memberType;

  @Column(name = "guest_name", length = 50)
  private String guestName;

  @Column(name = "guest_token_hash", length = 255)
  private String guestTokenHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AppointmentMemberRole role;

  @Column(name = "joined_at", nullable = false)
  private Instant joinedAt;

  @Column(name = "left_at")
  private Instant leftAt;

  protected AppointmentMember() {}

  static AppointmentMember createUserMember(
      Long appointmentId, Long userId, AppointmentMemberRole role, Instant joinedAt) {
    AppointmentMember appointmentMember = new AppointmentMember();
    appointmentMember.appointmentId = appointmentId;
    appointmentMember.userId = userId;
    appointmentMember.memberType = AppointmentMemberType.USER;
    appointmentMember.role = role;
    appointmentMember.joinedAt = joinedAt;
    return appointmentMember;
  }

  public static AppointmentMember createHost(Long appointmentId, Long userId, Instant joinedAt) {
    return createUserMember(appointmentId, userId, AppointmentMemberRole.HOST, joinedAt);
  }

  public static AppointmentMember createMember(Long appointmentId, Long userId, Instant joinedAt) {
    return createUserMember(appointmentId, userId, AppointmentMemberRole.MEMBER, joinedAt);
  }

  public static AppointmentMember createGuest(
      Long appointmentId, String guestName, String guestTokenHash, Instant joinedAt) {
    AppointmentMember appointmentMember = new AppointmentMember();
    appointmentMember.appointmentId = appointmentId;
    appointmentMember.memberType = AppointmentMemberType.GUEST;
    appointmentMember.guestName = guestName;
    appointmentMember.guestTokenHash = guestTokenHash;
    appointmentMember.role = AppointmentMemberRole.MEMBER;
    appointmentMember.joinedAt = joinedAt;
    return appointmentMember;
  }

  public Long getId() {
    return id;
  }

  public Long getAppointmentId() {
    return appointmentId;
  }

  public Long getUserId() {
    return userId;
  }

  public AppointmentMemberType getMemberType() {
    return memberType;
  }

  public String getGuestName() {
    return guestName;
  }

  public String getGuestTokenHash() {
    return guestTokenHash;
  }

  public AppointmentMemberRole getRole() {
    return role;
  }

  public Instant getJoinedAt() {
    return joinedAt;
  }

  public Instant getLeftAt() {
    return leftAt;
  }

  public boolean isHost() {
    return role == AppointmentMemberRole.HOST;
  }

  public boolean isLeft() {
    return leftAt != null;
  }

  public void leave(Instant now) {
    if (isHost()) {
      throw new IllegalStateException("Host cannot leave appointment");
    }
    this.leftAt = now;
  }

  public void rejoin(Instant now) {
    this.leftAt = null;
    this.joinedAt = now;
  }
}
