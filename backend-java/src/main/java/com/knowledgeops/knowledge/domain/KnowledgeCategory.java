package com.knowledgeops.knowledge.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_categories")
public class KnowledgeCategory {
  @Id private UUID id;

  @Column(nullable = false, unique = true, length = 100)
  private String name;

  @Column(length = 1000)
  private String description;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected KnowledgeCategory() {}

  public KnowledgeCategory(UUID id, String name, String description, Instant now) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
