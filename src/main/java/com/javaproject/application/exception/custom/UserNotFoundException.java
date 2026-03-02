package com.javaproject.application.exception.custom;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ProcessApiException {

    public UserNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
