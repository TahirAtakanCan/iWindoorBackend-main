package com.atablood.iWindoor_api.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {
    public BadRequestException(String code, String message) {
        super(HttpStatus.BAD_REQUEST, code, message);
    }

    public BadRequestException(String code, String message, Object details) {
        super(HttpStatus.BAD_REQUEST, code, message, details);
    }
}
