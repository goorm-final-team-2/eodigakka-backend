package com.eodigakka.domain.location;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisMemberLocationStore {

  private static final String KEY_FORMAT = "appointment:%d:locations";
  private static final Duration LOCATION_TTL = Duration.ofMinutes(30);

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  public RedisMemberLocationStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
  }

  public void save(MemberLocationResponse response) {
    String key = key(response.appointmentId());
    redisTemplate
        .opsForHash()
        .put(key, response.memberId().toString(), serialize(RedisMemberLocation.from(response)));
    redisTemplate.expire(key, LOCATION_TTL);
  }

  public List<MemberLocationResponse> findAll(Long appointmentId) {
    return redisTemplate.opsForHash().values(key(appointmentId)).stream()
        .map(value -> deserialize((String) value))
        .map(RedisMemberLocation::toResponse)
        .sorted(
            Comparator.comparing(MemberLocationResponse::updatedAt)
                .reversed()
                .thenComparing(MemberLocationResponse::memberId))
        .toList();
  }

  private String key(Long appointmentId) {
    return KEY_FORMAT.formatted(appointmentId);
  }

  private String serialize(RedisMemberLocation location) {
    try {
      return objectMapper.writeValueAsString(location);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize member location", exception);
    }
  }

  private RedisMemberLocation deserialize(String value) {
    try {
      return objectMapper.readValue(value, RedisMemberLocation.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to deserialize member location", exception);
    }
  }
}
