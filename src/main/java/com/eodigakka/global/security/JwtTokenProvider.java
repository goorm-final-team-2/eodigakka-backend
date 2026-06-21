package com.eodigakka.global.security;

import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private static final String HMAC_SHA256 = "HmacSHA256";
  private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

  private final JwtProperties jwtProperties;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  @Autowired
  public JwtTokenProvider(JwtProperties jwtProperties, ObjectMapper objectMapper) {
    this(jwtProperties, objectMapper, Clock.systemUTC());
  }

  JwtTokenProvider(JwtProperties jwtProperties, ObjectMapper objectMapper, Clock clock) {
    this.jwtProperties = jwtProperties;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  public String createAccessToken(Long userId) {
    Instant now = Instant.now(clock);
    Instant expiresAt = now.plus(jwtProperties.accessTokenExpiration());

    Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("sub", String.valueOf(userId));
    payload.put("iat", now.getEpochSecond());
    payload.put("exp", expiresAt.getEpochSecond());

    String unsignedToken = encodeJson(header) + "." + encodeJson(payload);
    return unsignedToken + "." + sign(unsignedToken);
  }

  public Long parseUserId(String token) {
    String[] parts = token.split("\\.");
    if (parts.length != 3) {
      throw invalidToken();
    }

    String unsignedToken = parts[0] + "." + parts[1];
    if (!MessageDigestSupport.constantTimeEquals(sign(unsignedToken), parts[2])) {
      throw invalidToken();
    }

    Map<String, Object> payload = decodePayload(parts[1]);
    long expiresAt = readLongClaim(payload, "exp");
    if (Instant.now(clock).getEpochSecond() >= expiresAt) {
      throw invalidToken();
    }

    try {
      return Long.valueOf(String.valueOf(payload.get("sub")));
    } catch (NumberFormatException exception) {
      throw invalidToken();
    }
  }

  public long accessTokenExpirationSeconds() {
    return jwtProperties.accessTokenExpirationSeconds();
  }

  private String encodeJson(Map<String, Object> value) {
    try {
      return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to create JWT.", exception);
    }
  }

  private Map<String, Object> decodePayload(String value) {
    try {
      byte[] decoded = BASE64_URL_DECODER.decode(value);
      return objectMapper.readValue(decoded, new TypeReference<>() {});
    } catch (IllegalArgumentException | IOException exception) {
      throw invalidToken();
    }
  }

  private long readLongClaim(Map<String, Object> payload, String claimName) {
    try {
      return Long.parseLong(String.valueOf(payload.get(claimName)));
    } catch (NumberFormatException exception) {
      throw invalidToken();
    }
  }

  private String sign(String unsignedToken) {
    try {
      Mac mac = Mac.getInstance(HMAC_SHA256);
      mac.init(
          new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
      return BASE64_URL_ENCODER.encodeToString(
          mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to sign JWT.", exception);
    }
  }

  private BusinessException invalidToken() {
    return new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
  }
}
