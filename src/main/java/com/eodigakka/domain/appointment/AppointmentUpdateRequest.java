package com.eodigakka.domain.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentUpdateRequest(
    @NotBlank @Size(max = 100) String title,
    @NotNull LocalDate appointmentDate,
    @NotNull LocalTime appointmentTime,
    String description,
    @Size(max = 255) String preferredArea,
    String notice) {}
