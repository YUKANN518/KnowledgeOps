package com.knowledgeops.audit.api;

import com.knowledgeops.audit.application.AuditQueryService;
import com.knowledgeops.audit.domain.AuditLog;
import java.time.Instant;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit-logs")
@PreAuthorize("hasAuthority('audit.read')")
public class AuditController {
  private final AuditQueryService queryService;

  public AuditController(AuditQueryService queryService) {
    this.queryService = queryService;
  }

  @GetMapping
  public List<Response> latest(@RequestParam(defaultValue = "50") int limit) {
    return queryService.latest(limit).stream().map(Response::from).toList();
  }

  public record Response(
      UUID id,
      String actorType,
      UUID actorId,
      String action,
      String resourceType,
      String resourceId,
      Map<String, Object> metadata,
      String requestId,
      Instant createdAt) {
    static Response from(AuditLog x) {
      return new Response(
          x.getId(),
          x.getActorType(),
          x.getActorId(),
          x.getAction().name(),
          x.getResourceType(),
          x.getResourceId(),
          x.getMetadataJson(),
          x.getRequestId(),
          x.getCreatedAt());
    }
  }
}
