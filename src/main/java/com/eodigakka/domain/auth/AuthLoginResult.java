package com.eodigakka.domain.auth;

import com.eodigakka.domain.user.User;

public record AuthLoginResult(
    String accessToken, RefreshTokenIssue refreshTokenIssue, User user, boolean isNewUser) {}
