package com.cinemamemory.api.common;

import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> handleApiException(ApiException exception) {
        return ResponseEntity.status(exception.status()).body(error(exception.status(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Validation failed");
        return ResponseEntity.badRequest().body(error(HttpStatus.BAD_REQUEST, message));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(error(HttpStatus.PAYLOAD_TOO_LARGE, "Media file is too large"));
    }

    @ExceptionHandler(MultipartException.class)
    ResponseEntity<Map<String, Object>> handleMultipart(MultipartException exception) {
        return ResponseEntity.badRequest().body(error(HttpStatus.BAD_REQUEST, "Invalid media upload request"));
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<Map<String, Object>> handleNotFound(Exception exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(HttpStatus.NOT_FOUND, "Not found"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception) {
        log.error("Unhandled API exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"));
    }

    private Map<String, Object> error(HttpStatus status, String message) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        );
    }
}
