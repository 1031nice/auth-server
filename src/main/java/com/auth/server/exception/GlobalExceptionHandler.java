package com.auth.server.exception;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationExceptions(
      MethodArgumentNotValidException ex) {

    Map<String, Object> errors = new HashMap<>();
    Map<String, String> fieldErrors = new HashMap<>();

    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            (error) -> {
              String fieldName = ((FieldError) error).getField();
              String errorMessage = error.getDefaultMessage();
              fieldErrors.put(fieldName, errorMessage);
            });

    errors.put("status", HttpStatus.BAD_REQUEST.value());
    errors.put("message", "Validation failed");
    errors.put("errors", fieldErrors);

    return ResponseEntity.badRequest().body(errors);
  }

  @ExceptionHandler(TokenReuseException.class)
  public ResponseEntity<Map<String, Object>> handleTokenReuseException(TokenReuseException ex) {
    log.warn("Token reuse detected: {}", ex.getMessage());

    Map<String, Object> error = new HashMap<>();
    error.put("status", HttpStatus.UNAUTHORIZED.value());
    error.put("message", ex.getMessage());
    error.put("error", "TOKEN_REUSE_DETECTED");

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
  }

  @ExceptionHandler(InvalidTokenException.class)
  public ResponseEntity<Map<String, Object>> handleInvalidTokenException(InvalidTokenException ex) {
    log.warn("Invalid token: {}", ex.getMessage());

    Map<String, Object> error = new HashMap<>();
    error.put("status", HttpStatus.UNAUTHORIZED.value());
    error.put("message", ex.getMessage());
    error.put("error", "INVALID_TOKEN");

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
  }

  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<Map<String, Object>> handleRateLimitExceededException(
      RateLimitExceededException ex) {
    log.warn("Rate limit exceeded: {}", ex.getMessage());

    Map<String, Object> error = new HashMap<>();
    error.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
    error.put("message", ex.getMessage());
    error.put("error", "RATE_LIMIT_EXCEEDED");

    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header("Retry-After", "60")
        .body(error);
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
    log.error("RuntimeException: {}", ex.getMessage());

    Map<String, Object> error = new HashMap<>();
    error.put("status", HttpStatus.BAD_REQUEST.value());
    error.put("message", ex.getMessage());

    return ResponseEntity.badRequest().body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex) {
    log.error("Unexpected error: {}", ex.getMessage(), ex);

    Map<String, Object> error = new HashMap<>();
    error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
    error.put("message", "An unexpected error occurred");

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}
