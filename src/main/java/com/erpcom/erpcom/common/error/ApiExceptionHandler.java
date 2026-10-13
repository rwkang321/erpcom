package com.erpcom.erpcom.common.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private final ApiErrorWriter writer;
    public ApiExceptionHandler(ApiErrorWriter writer) { this.writer = writer; }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> api(ApiException e, HttpServletRequest request) { return writer.response(e, request); }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class, org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
        HandlerMethodValidationException.class, ConstraintViolationException.class})
    ResponseEntity<ApiError> validation(Exception e, HttpServletRequest request) {
        return writer.response(new ApiException("ERR_INVALID_INPUT"), request);
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflict(Exception e, HttpServletRequest request) {
        log.warn("Reference data constraint violation: {}", e.getClass().getSimpleName());
        return writer.response(new ApiException("ERR_CONFLICT"), request);
    }
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> missing(Exception e, HttpServletRequest request) {
        return writer.response(new ApiException("ERR_NOT_FOUND"), request);
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> method(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        var result = writer.response(new ApiException("ERR_METHOD_NOT_ALLOWED"), request);
        return ResponseEntity.status(result.getStatusCode()).headers(result.getHeaders())
                .headers(e.getHeaders()).body(result.getBody());
    }
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> media(Exception e, HttpServletRequest request) {
        return writer.response(new ApiException("ERR_UNSUPPORTED_MEDIA_TYPE"), request);
    }
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<ApiError> accept(Exception e, HttpServletRequest request) {
        return writer.response(new ApiException("ERR_NOT_ACCEPTABLE"), request);
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception e, HttpServletRequest request) {
        log.error("Request failed: {}", request.getRequestURI(), e);
        return writer.response(new ApiException("ERR_INTERNAL_SERVER"), request);
    }
}
