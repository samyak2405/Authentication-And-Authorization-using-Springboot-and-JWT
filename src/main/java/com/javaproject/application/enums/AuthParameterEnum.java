package com.javaproject.application.enums;

import lombok.Getter;

@Getter
public enum AuthParameterEnum {

    IS_MFA_ENABLED("IS_MFA_ENABLED"),
    MFA_TYPE("MFA_TYPE");

    private final String parameterName;

    AuthParameterEnum(String parameterName) {
        this.parameterName = parameterName;
    }
}
