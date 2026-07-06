package com.eodigakka.domain.place;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

record KakaoLocalKeywordSearchResponse(Meta meta, List<Document> documents) {

  record Meta(
      @JsonProperty("total_count") int totalCount,
      @JsonProperty("pageable_count") int pageableCount,
      @JsonProperty("is_end") boolean isEnd) {}

  record Document(
      String id,
      @JsonProperty("place_name") String placeName,
      @JsonProperty("category_name") String categoryName,
      String phone,
      @JsonProperty("address_name") String addressName,
      @JsonProperty("road_address_name") String roadAddressName,
      String x,
      String y,
      @JsonProperty("place_url") String placeUrl,
      String distance) {}
}
