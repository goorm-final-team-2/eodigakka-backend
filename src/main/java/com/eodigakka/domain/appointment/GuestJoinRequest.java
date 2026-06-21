package com.eodigakka.domain.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuestJoinRequest(
    @NotBlank String inviteCode, @NotBlank @Size(max = 50) String guestName) {}
