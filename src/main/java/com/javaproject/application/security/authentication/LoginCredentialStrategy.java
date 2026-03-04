package com.javaproject.application.security.authentication;

import com.javaproject.application.dto.request.LoginUserRequest;
import com.javaproject.application.model.User;

public interface LoginCredentialStrategy {

    String getName();

    boolean supports(LoginUserRequest loginRequest);

    User authenticate(LoginUserRequest loginRequest);
}
