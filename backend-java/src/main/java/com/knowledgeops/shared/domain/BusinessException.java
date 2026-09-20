package com.knowledgeops.shared.domain;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
  private final ErrorCode code;
  private final HttpStatus status;

  public BusinessException(ErrorCode code, HttpStatus status, String message) {
    super(message);
    this.code = code;
    this.status = status;
  }

  public ErrorCode getCode() {
    return code;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public static BusinessException notFound() {
    return new BusinessException(
        ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Resource not found");
  }
}
