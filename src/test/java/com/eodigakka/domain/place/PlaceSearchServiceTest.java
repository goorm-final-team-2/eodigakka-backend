package com.eodigakka.domain.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.eodigakka.domain.appointment.AppointmentMember;
import com.eodigakka.domain.appointment.AppointmentMemberResolver;
import com.eodigakka.domain.appointment.AppointmentRepository;
import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import com.eodigakka.global.security.AuthUser;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlaceSearchServiceTest {

  private static final Long APPOINTMENT_ID = 10L;
  private static final Long USER_ID = 1L;
  private static final Long MEMBER_ID = 100L;
  private static final String GUEST_TOKEN = "guest-token";
  private static final Instant NOW = Instant.parse("2026-06-21T00:00:00Z");

  @Mock private AppointmentRepository appointmentRepository;
  @Mock private AppointmentMemberResolver appointmentMemberResolver;
  @Mock private KakaoLocalClient kakaoLocalClient;

  @Test
  void searchReturnsPlacesWhenUserIsAppointmentMember() {
    PlaceSearchService placeSearchService =
        new PlaceSearchService(appointmentRepository, appointmentMemberResolver, kakaoLocalClient);
    AuthUser authUser = new AuthUser(USER_ID);
    PlaceSearchRequest request =
        PlaceSearchRequest.of("강남역", null, null, null, null, null, null, null);
    PlaceSearchResponse kakaoResponse = response();
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willReturn(userMember());
    given(appointmentRepository.existsById(APPOINTMENT_ID)).willReturn(true);
    given(kakaoLocalClient.searchKeyword(request)).willReturn(kakaoResponse);

    PlaceSearchResponse response =
        placeSearchService.search(APPOINTMENT_ID, authUser, null, request);

    assertThat(response).isSameAs(kakaoResponse);
    verify(kakaoLocalClient).searchKeyword(request);
  }

  @Test
  void searchReturnsPlacesWhenGuestIsAppointmentMember() {
    PlaceSearchService placeSearchService =
        new PlaceSearchService(appointmentRepository, appointmentMemberResolver, kakaoLocalClient);
    PlaceSearchRequest request =
        PlaceSearchRequest.of("강남역", null, null, null, null, null, null, null);
    PlaceSearchResponse kakaoResponse = response();
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, null, GUEST_TOKEN))
        .willReturn(guestMember());
    given(appointmentRepository.existsById(APPOINTMENT_ID)).willReturn(true);
    given(kakaoLocalClient.searchKeyword(request)).willReturn(kakaoResponse);

    PlaceSearchResponse response =
        placeSearchService.search(APPOINTMENT_ID, null, GUEST_TOKEN, request);

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().kakaoPlaceId()).isEqualTo("12345");
  }

  @Test
  void searchDoesNotCallKakaoWhenRequesterIsNotAppointmentMember() {
    PlaceSearchService placeSearchService =
        new PlaceSearchService(appointmentRepository, appointmentMemberResolver, kakaoLocalClient);
    AuthUser authUser = new AuthUser(USER_ID);
    PlaceSearchRequest request =
        PlaceSearchRequest.of("강남역", null, null, null, null, null, null, null);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willThrow(new BusinessException(ErrorCode.APPOINTMENT_MEMBER_NOT_FOUND));

    assertThatThrownBy(() -> placeSearchService.search(APPOINTMENT_ID, authUser, null, request))
        .isInstanceOf(BusinessException.class);
    verify(kakaoLocalClient, never()).searchKeyword(request);
  }

  @Test
  void searchDoesNotCallKakaoWhenAppointmentDoesNotExist() {
    PlaceSearchService placeSearchService =
        new PlaceSearchService(appointmentRepository, appointmentMemberResolver, kakaoLocalClient);
    AuthUser authUser = new AuthUser(USER_ID);
    PlaceSearchRequest request =
        PlaceSearchRequest.of("강남역", null, null, null, null, null, null, null);
    given(appointmentMemberResolver.resolve(APPOINTMENT_ID, authUser, null))
        .willReturn(userMember());
    given(appointmentRepository.existsById(APPOINTMENT_ID)).willReturn(false);

    assertThatThrownBy(() -> placeSearchService.search(APPOINTMENT_ID, authUser, null, request))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.APPOINTMENT_NOT_FOUND);
    verify(kakaoLocalClient, never()).searchKeyword(request);
  }

  private PlaceSearchResponse response() {
    return new PlaceSearchResponse(
        List.of(
            new PlaceSearchItemResponse(
                "12345",
                "강남역",
                "서울 강남구 강남대로 396",
                "서울 강남구 강남대로 396",
                "지하철역",
                "https://place.map.kakao.com/12345",
                "02-123-4567",
                37.4979,
                127.0276,
                null)),
        1,
        15,
        1,
        1,
        true);
  }

  private AppointmentMember userMember() {
    AppointmentMember appointmentMember =
        AppointmentMember.createMember(APPOINTMENT_ID, USER_ID, NOW);
    ReflectionTestUtils.setField(appointmentMember, "id", MEMBER_ID);
    return appointmentMember;
  }

  private AppointmentMember guestMember() {
    AppointmentMember appointmentMember =
        AppointmentMember.createGuest(APPOINTMENT_ID, "guest", "guest-token-hash", NOW);
    ReflectionTestUtils.setField(appointmentMember, "id", MEMBER_ID);
    return appointmentMember;
  }
}
