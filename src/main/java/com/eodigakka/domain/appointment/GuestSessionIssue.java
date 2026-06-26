package com.eodigakka.domain.appointment;

import java.time.Instant;

public record GuestSessionIssue(String token, Instant expiresAt) {}
