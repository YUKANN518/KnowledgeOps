package com.knowledgeops.shared.api;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
    String code, String message, List<Detail> details, String requestId, Instant timestamp) {
  public record Detail(String field, String reason) {}
}
