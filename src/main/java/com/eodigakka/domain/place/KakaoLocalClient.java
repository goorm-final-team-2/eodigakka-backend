package com.eodigakka.domain.place;

/** Client boundary for Kakao Local APIs used by the place domain. */
public interface KakaoLocalClient {

  PlaceSearchResponse searchKeyword(PlaceSearchRequest request);
}
