package com.eodigakka.domain.auth;

import com.eodigakka.domain.user.User;
import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import com.eodigakka.global.security.JwtProperties;
import com.eodigakka.global.security.MessageDigestSupport;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

  private static final int REFRESH_TOKEN_BYTE_LENGTH = 32;

  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtProperties jwtProperties;
  private final SecureRandom secureRandom = new SecureRandom();
  private final Clock clock;

  @Autowired
  public RefreshTokenService(
      RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
    this(refreshTokenRepository, jwtProperties, Clock.systemUTC());
  }

  RefreshTokenService(
      RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties, Clock clock) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.jwtProperties = jwtProperties;
    this.clock = clock;
  }

  @Transactional
  public RefreshTokenIssue issue(User user) {
    Instant now = Instant.now(clock);
    Instant expiresAt = now.plus(jwtProperties.refreshTokenExpiration());
    String token = generateToken();
    refreshTokenRepository.save(
        RefreshToken.create(user, MessageDigestSupport.sha256Hex(token), expiresAt, now));
    return new RefreshTokenIssue(token, expiresAt);
  }

  @Transactional
  public RefreshTokenIssue rotate(String token) {
    RefreshToken refreshToken = getUsableRefreshTokenForUpdate(token);
    refreshToken.revoke(Instant.now(clock));
    return issue(refreshToken.getUser());
  }

  @Transactional
  public void revoke(String token) {
    refreshTokenRepository
        .findByTokenHash(MessageDigestSupport.sha256Hex(token))
        .filter(refreshToken -> refreshToken.isUsableAt(Instant.now(clock)))
        .ifPresent(refreshToken -> refreshToken.revoke(Instant.now(clock)));
  }

  @Transactional(readOnly = true)
  public User getUser(String token) {
    return getUsableRefreshToken(token).getUser();
  }

  private RefreshToken getUsableRefreshToken(String token) {
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByTokenHash(MessageDigestSupport.sha256Hex(token))
            .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
    validateUsable(refreshToken);
    return refreshToken;
  }

  private RefreshToken getUsableRefreshTokenForUpdate(String token) {
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByTokenHashForUpdate(MessageDigestSupport.sha256Hex(token))
            .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
    validateUsable(refreshToken);
    return refreshToken;
  }

  private void validateUsable(RefreshToken refreshToken) {
    if (!refreshToken.isUsableAt(Instant.now(clock))) {
      throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
    }
  }

  private String generateToken() {
    byte[] bytes = new byte[REFRESH_TOKEN_BYTE_LENGTH];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
