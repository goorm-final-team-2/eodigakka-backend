package com.eodigakka.domain.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.eodigakka.domain.auth.kakao.KakaoProperties;
import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RestKakaoLocalClientTest {

  @Test
  void searchKeywordMapsKakaoResponseToPlaceSearchResponse() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    RestKakaoLocalClient client = new RestKakaoLocalClient(properties(), builder);
    server
        .expect(requestTo(containsString("/v2/local/search/keyword.json")))
        .andExpect(header("Authorization", "KakaoAK rest-api-key"))
        .andExpect(queryParam("query", "강남역"))
        .andExpect(queryParam("x", "127.0276"))
        .andExpect(queryParam("y", "37.4979"))
        .andExpect(queryParam("radius", "20000"))
        .andExpect(queryParam("page", "1"))
        .andExpect(queryParam("size", "15"))
        .andExpect(queryParam("sort", "distance"))
        .andExpect(queryParam("category_group_code", "SW8"))
        .andRespond(withSuccess(kakaoResponse(), MediaType.APPLICATION_JSON));
    PlaceSearchRequest request =
        PlaceSearchRequest.of("강남역", 127.0276, 37.4979, 20000, 1, 15, "distance", "SW8");

    PlaceSearchResponse response = client.searchKeyword(request);

    assertThat(response.totalCount()).isEqualTo(1);
    assertThat(response.pageableCount()).isEqualTo(1);
    assertThat(response.isEnd()).isTrue();
    assertThat(response.items()).hasSize(1);
    PlaceSearchItemResponse item = response.items().getFirst();
    assertThat(item.kakaoPlaceId()).isEqualTo("12345");
    assertThat(item.name()).isEqualTo("강남역");
    assertThat(item.address()).isEqualTo("서울 강남구 역삼동 858");
    assertThat(item.roadAddress()).isEqualTo("서울 강남구 강남대로 396");
    assertThat(item.category()).isEqualTo("교통,수송 > 지하철,전철");
    assertThat(item.placeUrl()).isEqualTo("https://place.map.kakao.com/12345");
    assertThat(item.phone()).isEqualTo("02-123-4567");
    assertThat(item.latitude()).isEqualTo(37.4979);
    assertThat(item.longitude()).isEqualTo(127.0276);
    assertThat(item.distance()).isEqualTo(120);
    server.verify();
  }

  @Test
  void searchKeywordThrowsBusinessExceptionWhenKakaoFails() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    RestKakaoLocalClient client = new RestKakaoLocalClient(properties(), builder);
    server
        .expect(requestTo(containsString("/v2/local/search/keyword.json")))
        .andRespond(withServerError());
    PlaceSearchRequest request =
        PlaceSearchRequest.of("강남역", null, null, null, null, null, null, null);

    assertThatThrownBy(() -> client.searchKeyword(request))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.KAKAO_LOCAL_SEARCH_FAILED);
    server.verify();
  }

  private KakaoProperties properties() {
    return new KakaoProperties(
        "rest-api-key",
        "",
        URI.create("https://kauth.kakao.com/oauth/token"),
        URI.create("https://kapi.kakao.com/v2/user/me"),
        URI.create("https://dapi.kakao.com/v2/local/search/keyword.json"),
        List.of("http://localhost:5173/oauth/kakao/callback"));
  }

  private String kakaoResponse() {
    return """
        {
          "meta": {
            "total_count": 1,
            "pageable_count": 1,
            "is_end": true
          },
          "documents": [
            {
              "id": "12345",
              "place_name": "강남역",
              "category_name": "교통,수송 > 지하철,전철",
              "phone": "02-123-4567",
              "address_name": "서울 강남구 역삼동 858",
              "road_address_name": "서울 강남구 강남대로 396",
              "x": "127.0276",
              "y": "37.4979",
              "place_url": "https://place.map.kakao.com/12345",
              "distance": "120"
            }
          ]
        }
        """;
  }
}
