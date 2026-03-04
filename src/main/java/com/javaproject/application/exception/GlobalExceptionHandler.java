package com.javaproject.application.exception;

import com.javaproject.application.dto.response.ApiResponse;
import com.javaproject.application.exception.custom.AccountNotVerifiedException;
import com.javaproject.application.exception.custom.ApiValidationException;
import com.javaproject.application.exception.custom.ProcessApiException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        return errorResponse(request, HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        Map<String, String> violations = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            violations.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
        return errorResponse(request, HttpStatus.BAD_REQUEST, "Constraint violation", violations);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, HttpStatus.BAD_REQUEST, "Malformed JSON request", null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, HttpStatus.BAD_REQUEST,
                "Missing request parameter: " + ex.getParameterName(), null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, HttpStatus.BAD_REQUEST,
                "Invalid value for parameter: " + ex.getName(), null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Object> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", null);
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Object> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, HttpStatus.UNAUTHORIZED, "Unauthorized", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Object> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, HttpStatus.FORBIDDEN, "Forbidden", null);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Object> handleEntityNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, HttpStatus.NOT_FOUND, "Resource not found", null);
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Object> handleNoSuchElement(
            NoSuchElementException ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, HttpStatus.NOT_FOUND, "Resource not found", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Object> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, HttpStatus.CONFLICT, "Data integrity violation", null);
    }

    @ExceptionHandler(ApiValidationException.class)
    public ApiResponse<Object> handleApiValidation(
            ApiValidationException ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, ex.getStatus(), ex.getMessage(), null);
    }

    @ExceptionHandler(AccountNotVerifiedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Object> handleAccountNotVerified(
            AccountNotVerifiedException ex,
            HttpServletRequest request
    ) {
        Map<String, String> data = new HashMap<>();
        data.put("code", "ACCOUNT_NOT_VERIFIED");
        if (ex.getEmail() != null) {
            data.put("email", ex.getEmail());
        }
        if (ex.getMobile() != null) {
            data.put("mobile", ex.getMobile());
        }
        return errorResponse(request, ex.getStatus(), ex.getMessage(), data);
    }

    @ExceptionHandler(ProcessApiException.class)
    public ApiResponse<Object> handleProcessApi(
            ProcessApiException ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, ex.getStatus(), ex.getMessage(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Object> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Object> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", null);
    }

    private <T> ApiResponse<T> errorResponse(
            HttpServletRequest request,
            HttpStatus status,
            String message,
            T data
    ) {
        String requestId = MDC.get("requestId");
        log.error("Request [{}] failed — status={}, message={}", requestId, status.value(), message);

        ApiResponse<T> response = new ApiResponse<>();
        response.setRequestId(requestId);
        response.setSuccess(false);
        response.setResponseCode(String.valueOf(status.value()));
        response.setResponseMessage(message);
        response.setTimestamp(OffsetDateTime.now());
        response.setData(data);
        return response;
    }
}
