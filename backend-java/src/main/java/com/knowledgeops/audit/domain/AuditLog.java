package com.knowledgeops.audit.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
  @Id private UUID id;

  @Column(name = "actor_type", nullable = false, updatable = false)
  private String actorType;

  @Column(name = "actor_id", updatable = false)
  private UUID actorId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private AuditAction action;

  @Column(name = "resource_type", nullable = false, updatable = false)
  private String resourceType;

  @Column(name = "resource_id", updatable = false)
  private String resourceId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata_json", nullable = false, updatable = false, columnDefinition = "json")
  private Map<String, Object> metadataJson;

  @Column(name = "request_id", nullable = false, updatable = false)
  private String requestId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected AuditLog() {}

  public AuditLog(
      UUID id,
      String actorType,
      UUID actorId,
      AuditAction action,
      String resourceType,
      String resourceId,
      Map<String, Object> metadataJson,
      String requestId,
      Instant createdAt) {
    this.id = id;
    this.actorType = actorType;
    this.actorId = actorId;
    this.action = action;
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    this.metadataJson = Map.copyOf(metadataJson);
    this.requestId = requestId;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public String getActorType() {
    return actorType;
  }

  public UUID getActorId() {
    return actorId;
  }

  public AuditAction getAction() {
    return action;
  }

  public String getResourceType() {
    return resourceType;
  }

  public String getResourceId() {
    return resourceId;
  }

  public Map<String, Object> getMetadataJson() {
    return metadataJson;
  }

  public String getRequestId() {
    return requestId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
