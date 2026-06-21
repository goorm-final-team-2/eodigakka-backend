package com.eodigakka.domain.appointment;

import jakarta.validation.constraints.NotBlank;

public record AppointmentJoinRequest(@NotBlank String inviteCode) {}
