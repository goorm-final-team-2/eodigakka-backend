package com.eodigakka.domain.auth;

import java.time.Instant;

public record RefreshTokenIssue(String token, Instant expiresAt) {}
