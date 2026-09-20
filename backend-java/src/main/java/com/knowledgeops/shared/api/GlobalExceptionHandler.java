package com.knowledgeops.shared.api;

import com.knowledgeops.shared.domain.*;
import com.knowledgeops.shared.infrastructure.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.*;
import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BusinessException.class)
  ResponseEntity<ErrorResponse> business(BusinessException ex, HttpServletRequest req) {
    return response(ex.getStatus(), ex.getCode().name(), ex.getMessage(), List.of(), req);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErrorResponse> validation(
      MethodArgumentNotValidException ex, HttpServletRequest req) {
    List<ErrorResponse.Detail> details =
        ex.getBindingResult().getFieldErrors().stream().map(this::detail).toList();
    return response(
        HttpStatus.UNPROCESSABLE_ENTITY,
        ErrorCode.VALIDATION_ERROR.name(),
        "Request validation failed",
        details,
        req);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ErrorResponse> malformed(
      HttpMessageNotReadableException ex, HttpServletRequest req) {
    return response(
        HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST.name(), "Malformed request", List.of(), req);
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ErrorResponse> forbidden(AccessDeniedException ex, HttpServletRequest req) {
    return response(
        HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN.name(), "Access is denied", List.of(), req);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorResponse> unexpected(Exception ex, HttpServletRequest req) {
    log.error("unhandled_request_failure", ex);
    return response(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorCode.INTERNAL_ERROR.name(),
        "Internal server error",
        List.of(),
        req);
  }

  private ErrorResponse.Detail detail(FieldError e) {
    return new ErrorResponse.Detail(
        e.getField(), Objects.requireNonNullElse(e.getDefaultMessage(), "invalid"));
  }

  private ResponseEntity<ErrorResponse> response(
      HttpStatus status,
      String code,
      String message,
      List<ErrorResponse.Detail> details,
      HttpServletRequest req) {
    Object id = req.getAttribute(RequestIdFilter.ATTRIBUTE);
    return ResponseEntity.status(status)
        .body(new ErrorResponse(code, message, details, String.valueOf(id), Instant.now()));
  }
}
