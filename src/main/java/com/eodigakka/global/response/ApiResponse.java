package com.eodigakka.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        T data,
        String message
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, "success");
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(null, "success");
    }
}
