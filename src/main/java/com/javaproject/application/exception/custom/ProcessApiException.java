package com.javaproject.application.exception.custom;

import org.springframework.http.HttpStatus;

public class ProcessApiException extends RuntimeException {

    private final HttpStatus status;

    public ProcessApiException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public ProcessApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public ProcessApiException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public ProcessApiException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
