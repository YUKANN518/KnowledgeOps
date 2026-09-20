package com.knowledgeops.audit.application;

import com.knowledgeops.audit.domain.AuditLog;
import com.knowledgeops.audit.infrastructure.AuditLogRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditQueryService {
  private final AuditLogRepository repository;

  public AuditQueryService(AuditLogRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public List<AuditLog> latest(int requestedLimit) {
    int limit = Math.max(1, Math.min(100, requestedLimit));
    return repository
        .findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
        .getContent();
  }
}
