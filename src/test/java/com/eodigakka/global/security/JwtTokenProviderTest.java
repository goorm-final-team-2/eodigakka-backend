package com.eodigakka.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eodigakka.global.error.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

  private static final Instant NOW = Instant.parse("2026-06-21T00:00:00Z");

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final JwtProperties jwtProperties = new JwtProperties("test-secret-key", 1800, 1209600);

  @Test
  void createAccessTokenCanBeParsedToUserId() {
    JwtTokenProvider tokenProvider =
        new JwtTokenProvider(jwtProperties, objectMapper, Clock.fixed(NOW, ZoneOffset.UTC));

    String token = tokenProvider.createAccessToken(1L);

    assertThat(tokenProvider.parseUserId(token)).isEqualTo(1L);
  }

  @Test
  void expiredAccessTokenThrowsBusinessException() {
    JwtTokenProvider issuingProvider =
        new JwtTokenProvider(jwtProperties, objectMapper, Clock.fixed(NOW, ZoneOffset.UTC));
    String token = issuingProvider.createAccessToken(1L);
    JwtTokenProvider parsingProvider =
        new JwtTokenProvider(
            jwtProperties, objectMapper, Clock.fixed(NOW.plusSeconds(1800), ZoneOffset.UTC));

    assertThatThrownBy(() -> parsingProvider.parseUserId(token))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void tamperedAccessTokenThrowsBusinessException() {
    JwtTokenProvider tokenProvider =
        new JwtTokenProvider(jwtProperties, objectMapper, Clock.fixed(NOW, ZoneOffset.UTC));
    String token = tokenProvider.createAccessToken(1L);

    assertThatThrownBy(() -> tokenProvider.parseUserId(token + "tampered"))
        .isInstanceOf(BusinessException.class);
  }
}
