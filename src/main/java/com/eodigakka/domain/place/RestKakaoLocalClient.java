package com.eodigakka.domain.place;

import com.eodigakka.domain.auth.kakao.KakaoProperties;
import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/** RestClient-backed adapter for Kakao Local keyword place search. */
@Component
public class RestKakaoLocalClient implements KakaoLocalClient {

  private static final Logger log = LoggerFactory.getLogger(RestKakaoLocalClient.class);

  private final KakaoProperties kakaoProperties;
  private final RestClient restClient;

  public RestKakaoLocalClient(
      KakaoProperties kakaoProperties, RestClient.Builder restClientBuilder) {
    this.kakaoProperties = kakaoProperties;
    this.restClient = restClientBuilder.build();
  }

  @Override
  public PlaceSearchResponse searchKeyword(PlaceSearchRequest request) {
    try {
      KakaoLocalKeywordSearchResponse response =
          restClient
              .get()
              .uri(buildUri(request))
              .headers(
                  headers ->
                      headers.set("Authorization", "KakaoAK " + kakaoProperties.restApiKey()))
              .retrieve()
              .body(KakaoLocalKeywordSearchResponse.class);
      if (response == null || response.meta() == null || response.documents() == null) {
        log.debug("Kakao Local search returned an empty or invalid response body.");
        throw new BusinessException(ErrorCode.KAKAO_LOCAL_SEARCH_FAILED);
      }
      return toResponse(request, response);
    } catch (BusinessException exception) {
      throw exception;
    } catch (RestClientResponseException exception) {
      log.debug(
          "Kakao Local search failed. status={}, responseBody={}",
          exception.getStatusCode(),
          exception.getResponseBodyAsString(StandardCharsets.UTF_8),
          exception);
      throw new BusinessException(ErrorCode.KAKAO_LOCAL_SEARCH_FAILED);
    } catch (RestClientException | IllegalArgumentException exception) {
      log.debug("Kakao Local search failed before a valid response was mapped.", exception);
      throw new BusinessException(ErrorCode.KAKAO_LOCAL_SEARCH_FAILED);
    }
  }

  private URI buildUri(PlaceSearchRequest request) {
    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromUri(kakaoProperties.localKeywordSearchUri())
            .queryParam("query", request.query())
            .queryParam("page", request.page())
            .queryParam("size", request.size())
            .queryParam("sort", request.sort().kakaoValue());
    if (request.categoryGroupCode() != null) {
      uriBuilder.queryParam("category_group_code", request.categoryGroupCode());
    }
    if (request.hasCenter()) {
      uriBuilder.queryParam("x", request.x()).queryParam("y", request.y());
    }
    if (request.radius() != null) {
      uriBuilder.queryParam("radius", request.radius());
    }
    return uriBuilder.build().toUri();
  }

  private PlaceSearchResponse toResponse(
      PlaceSearchRequest request, KakaoLocalKeywordSearchResponse response) {
    List<PlaceSearchItemResponse> items =
        response.documents().stream().map(this::toItemResponse).toList();
    return new PlaceSearchResponse(
        items,
        request.page(),
        request.size(),
        response.meta().totalCount(),
        response.meta().pageableCount(),
        response.meta().isEnd());
  }

  private PlaceSearchItemResponse toItemResponse(
      KakaoLocalKeywordSearchResponse.Document document) {
    return new PlaceSearchItemResponse(
        document.id(),
        document.placeName(),
        document.addressName(),
        document.roadAddressName(),
        document.categoryName(),
        document.placeUrl(),
        document.phone(),
        parseDouble(document.y()),
        parseDouble(document.x()),
        parseDistance(document.distance()));
  }

  private double parseDouble(String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(ErrorCode.KAKAO_LOCAL_SEARCH_FAILED);
    }
    return Double.parseDouble(value);
  }

  private Integer parseDistance(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return Integer.valueOf(value);
  }
}
