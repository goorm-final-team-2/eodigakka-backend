package com.eodigakka.domain.place;

import com.eodigakka.domain.appointment.AppointmentMemberResolver;
import com.eodigakka.domain.appointment.AppointmentRepository;
import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import com.eodigakka.global.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for appointment-scoped place search. */
@Service
public class PlaceSearchService {

  private final AppointmentRepository appointmentRepository;
  private final AppointmentMemberResolver appointmentMemberResolver;
  private final KakaoLocalClient kakaoLocalClient;

  public PlaceSearchService(
      AppointmentRepository appointmentRepository,
      AppointmentMemberResolver appointmentMemberResolver,
      KakaoLocalClient kakaoLocalClient) {
    this.appointmentRepository = appointmentRepository;
    this.appointmentMemberResolver = appointmentMemberResolver;
    this.kakaoLocalClient = kakaoLocalClient;
  }

  @Transactional(readOnly = true)
  public PlaceSearchResponse search(
      Long appointmentId, AuthUser authUser, String guestSessionToken, PlaceSearchRequest request) {
    appointmentMemberResolver.resolve(appointmentId, authUser, guestSessionToken);
    if (!appointmentRepository.existsById(appointmentId)) {
      throw new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND);
    }
    return kakaoLocalClient.searchKeyword(request);
  }
}
