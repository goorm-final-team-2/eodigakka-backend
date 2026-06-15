package com.eodigakka.global.error;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
    ErrorCode errorCode = exception.getErrorCode();
    ErrorResponse response = ErrorResponse.of(errorCode, exception.getMessage());

    return ResponseEntity.status(errorCode.getStatus()).body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException exception) {
    Map<String, String> errors = new LinkedHashMap<>();
    for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
      errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
    }

    ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
    ErrorResponse response = ErrorResponse.of(errorCode, errorCode.getMessage(), errors);

    return ResponseEntity.status(errorCode.getStatus()).body(response);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolationException(
      ConstraintViolationException exception) {
    Map<String, String> errors = new LinkedHashMap<>();
    exception
        .getConstraintViolations()
        .forEach(
            violation ->
                errors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));

    ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
    ErrorResponse response = ErrorResponse.of(errorCode, errorCode.getMessage(), errors);

    return ResponseEntity.status(errorCode.getStatus()).body(response);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class,
    HandlerMethodValidationException.class
  })
  public ResponseEntity<ErrorResponse> handleInvalidRequestException(Exception exception) {
    log.debug("Invalid request", exception);

    ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
    ErrorResponse response = ErrorResponse.of(errorCode, errorCode.getMessage());

    return ResponseEntity.status(errorCode.getStatus()).body(response);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
      NoResourceFoundException exception) {
    ErrorCode errorCode = ErrorCode.RESOURCE_NOT_FOUND;
    ErrorResponse response = ErrorResponse.of(errorCode, errorCode.getMessage());

    return ResponseEntity.status(errorCode.getStatus()).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
    log.error("Unhandled exception", exception);

    ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
    ErrorResponse response = ErrorResponse.of(errorCode, errorCode.getMessage());

    return ResponseEntity.status(errorCode.getStatus()).body(response);
  }
}
