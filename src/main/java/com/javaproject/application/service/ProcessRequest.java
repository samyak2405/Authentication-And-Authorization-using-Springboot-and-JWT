package com.javaproject.application.service;

import com.javaproject.application.dto.request.BaseRequest;
import com.javaproject.application.dto.response.ApiResponse;

public interface ProcessRequest {

    ApiResponse<?> processApiRequest(BaseRequest baseRequest);
}
