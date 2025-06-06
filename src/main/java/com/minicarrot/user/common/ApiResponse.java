package com.minicarrot.user.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Object errors;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("성공")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static ApiResponse<?> error(String message) {
        return ApiResponse.builder()
                .success(false)
                .message(message)
                .build();
    }

    public static ApiResponse<?> error(String message, Object errors) {
        return ApiResponse.builder()
                .success(false)
                .message(message)
                .errors(errors)
                .build();
    }
} 