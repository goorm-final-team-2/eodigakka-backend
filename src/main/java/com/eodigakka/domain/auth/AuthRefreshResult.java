package com.eodigakka.domain.auth;

public record AuthRefreshResult(String accessToken, RefreshTokenIssue refreshTokenIssue) {}
