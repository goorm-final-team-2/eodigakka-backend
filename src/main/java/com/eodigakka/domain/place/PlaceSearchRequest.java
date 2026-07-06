package com.eodigakka.domain.place;

import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;

/**
 * Validated search condition for proxying Kakao Local keyword place search.
 *
 * @param query keyword to search
 * @param x center longitude used for distance-based search
 * @param y center latitude used for distance-based search
 * @param radius search radius in meters
 * @param page Kakao result page number
 * @param size number of results per page
 * @param sort Kakao Local sort mode
 * @param categoryGroupCode optional Kakao category group code
 */
public record PlaceSearchRequest(
    String query,
    Double x,
    Double y,
    Integer radius,
    Integer page,
    Integer size,
    PlaceSearchSort sort,
    String categoryGroupCode) {

  private static final int DEFAULT_PAGE = 1;
  private static final int DEFAULT_SIZE = 15;
  private static final int MIN_PAGE = 1;
  private static final int MAX_PAGE = 45;
  private static final int MIN_SIZE = 1;
  private static final int MAX_SIZE = 15;
  private static final int MIN_RADIUS = 0;
  private static final int MAX_RADIUS = 20_000;

  public static PlaceSearchRequest of(
      String query,
      Double x,
      Double y,
      Integer radius,
      Integer page,
      Integer size,
      String sort,
      String categoryGroupCode) {
    PlaceSearchRequest request =
        new PlaceSearchRequest(
            normalizeQuery(query),
            x,
            y,
            radius,
            page == null ? DEFAULT_PAGE : page,
            size == null ? DEFAULT_SIZE : size,
            PlaceSearchSort.from(sort),
            normalizeBlank(categoryGroupCode));
    request.validate();
    return request;
  }

  public boolean hasCenter() {
    return x != null && y != null;
  }

  private void validate() {
    validateLongitude(x);
    validateLatitude(y);
    validateRange(page, MIN_PAGE, MAX_PAGE);
    validateRange(size, MIN_SIZE, MAX_SIZE);
    if (radius != null) {
      validateRange(radius, MIN_RADIUS, MAX_RADIUS);
      requireCenter();
    }
    if (sort == PlaceSearchSort.DISTANCE) {
      requireCenter();
    }
  }

  private void requireCenter() {
    if (!hasCenter()) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST);
    }
  }

  private static String normalizeQuery(String query) {
    String normalized = normalizeBlank(query);
    if (normalized == null || normalized.length() > 100) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST);
    }
    return normalized;
  }

  private static String normalizeBlank(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static void validateLongitude(Double longitude) {
    if (longitude != null && (longitude < -180.0 || longitude > 180.0)) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST);
    }
  }

  private static void validateLatitude(Double latitude) {
    if (latitude != null && (latitude < -90.0 || latitude > 90.0)) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST);
    }
  }

  private static void validateRange(int value, int min, int max) {
    if (value < min || value > max) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST);
    }
  }
}
