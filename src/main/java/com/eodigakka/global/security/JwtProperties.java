package com.eodigakka.global.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.jwt")
public record JwtProperties(
    String secret, long accessTokenExpirationSeconds, long refreshTokenExpirationSeconds) {

  public Duration accessTokenExpiration() {
    return Duration.ofSeconds(accessTokenExpirationSeconds);
  }

  public Duration refreshTokenExpiration() {
    return Duration.ofSeconds(refreshTokenExpirationSeconds);
  }
}
