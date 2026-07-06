package com.eodigakka.domain.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eodigakka.global.error.BusinessException;
import org.junit.jupiter.api.Test;

class PlaceSearchRequestTest {

  @Test
  void appliesDefaultsWhenOptionalConditionsAreMissing() {
    PlaceSearchRequest request =
        PlaceSearchRequest.of(" 강남역 ", null, null, null, null, null, null, null);

    assertThat(request.query()).isEqualTo("강남역");
    assertThat(request.page()).isEqualTo(1);
    assertThat(request.size()).isEqualTo(15);
    assertThat(request.sort()).isEqualTo(PlaceSearchSort.ACCURACY);
  }

  @Test
  void rejectsDistanceSortWithoutCenterCoordinate() {
    assertThatThrownBy(
            () -> PlaceSearchRequest.of("강남역", null, null, null, null, null, "distance", null))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void rejectsRadiusWithoutCenterCoordinate() {
    assertThatThrownBy(() -> PlaceSearchRequest.of("강남역", null, null, 1000, null, null, null, null))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void rejectsInvalidPageAndSize() {
    assertThatThrownBy(() -> PlaceSearchRequest.of("강남역", null, null, null, 46, null, null, null))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> PlaceSearchRequest.of("강남역", null, null, null, null, 16, null, null))
        .isInstanceOf(BusinessException.class);
  }
}
