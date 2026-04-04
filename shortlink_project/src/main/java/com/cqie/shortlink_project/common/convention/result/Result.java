package com.cqie.shortlink_project.common.convention.result;


import com.cqie.shortlink_project.common.convention.errorcode.BaseErrorCode;
import com.cqie.shortlink_project.common.convention.errorcode.IErrorCode;
import com.cqie.shortlink_project.common.convention.exception.AbstractException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Result<T> {

    private String code;

    private String message;

    private T data;


    public static <T> Result<T> success(T data) {
        return new Result<>("0000", "成功", data);
    }

    public static <T> Result<T> success() {
        return new Result<>("0000", "成功", null);
    }

    public static <T> Result<T> error(String code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> error(IErrorCode errorCode) {
        return new Result<>(errorCode.code(), errorCode.message(), null);
    }

    public static <T> Result<T> error(AbstractException abstractException) {
        String errorCode = Optional.ofNullable(abstractException.getErrorCode())
                .orElse(BaseErrorCode.SERVICE_ERROR.code());
        String errorMessage = Optional.ofNullable(abstractException.getErrorMessage())
                .orElse(BaseErrorCode.SERVICE_ERROR.message());
        return new Result<>(errorCode, errorMessage, null);
    }

    public static <T> Result<T> error() {
        return new Result<>(BaseErrorCode.SERVICE_ERROR.code(), BaseErrorCode.SERVICE_ERROR.message(), null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(BaseErrorCode.SERVICE_ERROR.code(), message, null);
    }
}
