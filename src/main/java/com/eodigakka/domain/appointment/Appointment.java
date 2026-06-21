package com.eodigakka.domain.appointment;

import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "appointments")
public class Appointment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(name = "appointment_date", nullable = false)
  private LocalDate appointmentDate;

  @Column(name = "appointment_time", nullable = false)
  private LocalTime appointmentTime;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "preferred_area", length = 255)
  private String preferredArea;

  @Column(columnDefinition = "text")
  private String notice;

  @Column(name = "host_user_id", nullable = false)
  private Long hostUserId;

  @Column(name = "invite_code", nullable = false, unique = true, length = 255)
  private String inviteCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AppointmentStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Appointment() {}

  private Appointment(
      String title,
      LocalDate appointmentDate,
      LocalTime appointmentTime,
      String description,
      String preferredArea,
      String notice,
      Long hostUserId,
      String inviteCode,
      Instant now) {
    this.title = title;
    this.appointmentDate = appointmentDate;
    this.appointmentTime = appointmentTime;
    this.description = description;
    this.preferredArea = preferredArea;
    this.notice = notice;
    this.hostUserId = hostUserId;
    this.inviteCode = inviteCode;
    this.status = AppointmentStatus.PLANNING;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static Appointment create(
      String title,
      LocalDate appointmentDate,
      LocalTime appointmentTime,
      String description,
      String preferredArea,
      String notice,
      Long hostUserId,
      String inviteCode,
      Instant now) {
    return new Appointment(
        title,
        appointmentDate,
        appointmentTime,
        description,
        preferredArea,
        notice,
        hostUserId,
        inviteCode,
        now);
  }

  public void update(
      String title,
      LocalDate appointmentDate,
      LocalTime appointmentTime,
      String description,
      String preferredArea,
      String notice,
      Instant now) {
    validatePlanning();
    this.title = title;
    this.appointmentDate = appointmentDate;
    this.appointmentTime = appointmentTime;
    this.description = description;
    this.preferredArea = preferredArea;
    this.notice = notice;
    this.updatedAt = now;
  }

  public void confirm(Instant now) {
    validatePlanning();
    this.status = AppointmentStatus.CONFIRMED;
    this.updatedAt = now;
  }

  public void validatePlanning() {
    if (status != AppointmentStatus.PLANNING) {
      throw new BusinessException(ErrorCode.APPOINTMENT_STATUS_NOT_EDITABLE);
    }
  }

  public void validateJoinable() {
    if (status == AppointmentStatus.CLOSED) {
      throw new BusinessException(ErrorCode.APPOINTMENT_NOT_JOINABLE);
    }
  }

  public void validateConfirmed() {
    if (status != AppointmentStatus.CONFIRMED) {
      throw new BusinessException(ErrorCode.APPOINTMENT_STATUS_NOT_EDITABLE);
    }
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public LocalDate getAppointmentDate() {
    return appointmentDate;
  }

  public LocalTime getAppointmentTime() {
    return appointmentTime;
  }

  public String getDescription() {
    return description;
  }

  public String getPreferredArea() {
    return preferredArea;
  }

  public String getNotice() {
    return notice;
  }

  public Long getHostUserId() {
    return hostUserId;
  }

  public String getInviteCode() {
    return inviteCode;
  }

  public AppointmentStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
