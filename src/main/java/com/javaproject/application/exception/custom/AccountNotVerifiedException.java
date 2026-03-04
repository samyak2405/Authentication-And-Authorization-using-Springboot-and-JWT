package com.javaproject.application.exception.custom;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AccountNotVerifiedException extends ProcessApiException {

    private final String email;
    private final String mobile;

    public AccountNotVerifiedException(String message, String email, String mobile) {
        super(message, HttpStatus.FORBIDDEN);
        this.email = email;
        this.mobile = mobile;
    }
}
