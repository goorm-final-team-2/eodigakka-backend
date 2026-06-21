package com.eodigakka.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청값이 올바르지 않습니다."),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
  FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "요청 권한이 없습니다."),
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
  AUTH_KAKAO_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_KAKAO_FAILED", "카카오 로그인에 실패했습니다."),
  AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_TOKEN", "인증 토큰이 올바르지 않습니다."),
  AUTH_REFRESH_TOKEN_INVALID(
      HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_TOKEN_INVALID", "Refresh Token이 올바르지 않습니다."),
  AUTH_REDIRECT_URI_NOT_ALLOWED(
      HttpStatus.BAD_REQUEST, "AUTH_REDIRECT_URI_NOT_ALLOWED", "허용되지 않은 redirectUri입니다."),
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
  APPOINTMENT_MEMBER_NOT_FOUND(
      HttpStatus.FORBIDDEN, "APPOINTMENT_MEMBER_NOT_FOUND", "약속방 참여자를 찾을 수 없습니다."),
  APPOINTMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "APPOINTMENT_ACCESS_DENIED", "약속방 접근 권한이 없습니다."),
  APPOINTMENT_HOST_REQUIRED(HttpStatus.FORBIDDEN, "APPOINTMENT_HOST_REQUIRED", "약속방 방장 권한이 필요합니다."),
  APPOINTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND", "약속방을 찾을 수 없습니다."),
  INTERNAL_SERVER_ERROR(
      HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;

  ErrorCode(HttpStatus status, String code, String message) {
    this.status = status;
    this.code = code;
    this.message = message;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
