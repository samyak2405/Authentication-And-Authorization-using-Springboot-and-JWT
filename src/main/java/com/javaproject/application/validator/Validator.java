package com.javaproject.application.validator;

import com.javaproject.application.dto.request.BaseRequest;

public interface Validator {

    public void validateRequest(BaseRequest baseRequest);
}
