package com.knowledgeops.audit.application;

import com.knowledgeops.audit.domain.*;
import com.knowledgeops.audit.infrastructure.AuditLogRepository;
import com.knowledgeops.shared.infrastructure.RequestIdFilter;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.*;

@Service
public class AuditService {
  private final AuditLogRepository repository;
  private final Clock clock;

  public AuditService(AuditLogRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Transactional
  public void record(
      String actorType,
      UUID actorId,
      AuditAction action,
      String resourceType,
      String resourceId,
      Map<String, Object> safeMetadata) {
    repository.save(
        new AuditLog(
            UUID.randomUUID(),
            actorType,
            actorId,
            action,
            resourceType,
            resourceId,
            safeMetadata,
            requestId(),
            clock.instant()));
  }

  private String requestId() {
    RequestAttributes a = RequestContextHolder.getRequestAttributes();
    if (a instanceof ServletRequestAttributes s) {
      Object id = s.getRequest().getAttribute(RequestIdFilter.ATTRIBUTE);
      if (id != null) return id.toString();
    }
    return "system";
  }
}
