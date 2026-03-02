package com.javaproject.application.enums;

import lombok.Getter;

@Getter
public  enum ApiTypeEnum {

    REGISTER("REGISTER"),
    LOGIN("LOGIN"),
    UPDATE_PROFILE("UPDATE_PROFILE"),
    CHANGE_PASSWORD("CHANGE_PASSWORD"),
    DELETE_ACCOUNT("DELETE_ACCOUNT"),
    RESET_PASSWORD("RESET_PASSWORD"),
    VERIFY_REGISTRATION_OTP("VERIFY_REGISTRATION_OTP"),
    RESEND_REGISTRATION_OTP("RESEND_REGISTRATION_OTP");

    private final String apiType;

    ApiTypeEnum(String apiType) {
        this.apiType = apiType;
    }
}
