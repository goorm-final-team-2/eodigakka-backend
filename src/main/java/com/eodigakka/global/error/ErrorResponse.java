package com.eodigakka.global.error;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        String code,
        String message,
        Map<String, String> errors,
        Instant timestamp
) {

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getCode(), message, null, Instant.now());
    }

    public static ErrorResponse of(
            ErrorCode errorCode,
            String message,
            Map<String, String> errors
    ) {
        return new ErrorResponse(errorCode.getCode(), message, errors, Instant.now());
    }
}
