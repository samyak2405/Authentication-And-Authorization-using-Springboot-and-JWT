package com.javaproject.application.exception.custom;

import org.springframework.http.HttpStatus;

public class DBException extends ProcessApiException{
    public DBException(String message) {
        super(message);
    }

    public DBException(String message, HttpStatus status) {
        super(message, status);
    }

    public DBException(String message, Throwable cause) {
        super(message, cause);
    }

    public DBException(String message, HttpStatus status, Throwable cause) {
        super(message, status, cause);
    }
}
