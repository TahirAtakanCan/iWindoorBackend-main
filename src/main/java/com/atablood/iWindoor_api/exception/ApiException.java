package com.atablood.iWindoor_api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Uygulamanın bilinçli fırlattığı tüm hataların tabanı. Makinece okunur bir {@code code} taşır. */
@Getter
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final transient Object details;

    public ApiException(HttpStatus status, String code, String message, Object details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }
}
