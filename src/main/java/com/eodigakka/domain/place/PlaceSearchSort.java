package com.eodigakka.domain.place;

import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;

/** Sort options supported by Kakao Local keyword search. */
public enum PlaceSearchSort {
  ACCURACY("accuracy"),
  DISTANCE("distance");

  private final String kakaoValue;

  PlaceSearchSort(String kakaoValue) {
    this.kakaoValue = kakaoValue;
  }

  public static PlaceSearchSort from(String value) {
    if (value == null || value.isBlank()) {
      return ACCURACY;
    }
    for (PlaceSearchSort sort : values()) {
      if (sort.kakaoValue.equalsIgnoreCase(value.trim())) {
        return sort;
      }
    }
    throw new BusinessException(ErrorCode.INVALID_REQUEST);
  }

  public String kakaoValue() {
    return kakaoValue;
  }
}
