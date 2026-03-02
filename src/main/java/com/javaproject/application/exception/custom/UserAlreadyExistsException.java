package com.javaproject.application.exception.custom;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends ProcessApiException {

    public UserAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
