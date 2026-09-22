package com.knowledgeops.knowledge.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_documents")
public class KnowledgeDocument {
  @Id private UUID id;

  @Column(name = "original_filename", nullable = false, length = 255)
  private String originalFilename;

  @Column(name = "stored_filename", nullable = false, unique = true, length = 80)
  private String storedFilename;

  @Column(name = "storage_key", nullable = false, unique = true, length = 80)
  private String storageKey;

  @Column(name = "content_type", nullable = false, length = 120)
  private String contentType;

  @Column(name = "file_size", nullable = false)
  private long fileSize;

  @Column(name = "uploader_id", nullable = false, updatable = false)
  private UUID uploaderId;

  @Column(name = "category_id", nullable = false)
  private UUID categoryId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private DocumentStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected KnowledgeDocument() {}

  public KnowledgeDocument(
      UUID id,
      String originalFilename,
      String storedFilename,
      String storageKey,
      String contentType,
      long fileSize,
      UUID uploaderId,
      UUID categoryId,
      Instant now) {
    this.id = id;
    this.originalFilename = originalFilename;
    this.storedFilename = storedFilename;
    this.storageKey = storageKey;
    this.contentType = contentType;
    this.fileSize = fileSize;
    this.uploaderId = uploaderId;
    this.categoryId = categoryId;
    this.status = DocumentStatus.ACTIVE;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getOriginalFilename() {
    return originalFilename;
  }

  public String getStoredFilename() {
    return storedFilename;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public String getContentType() {
    return contentType;
  }

  public long getFileSize() {
    return fileSize;
  }

  public UUID getUploaderId() {
    return uploaderId;
  }

  public UUID getCategoryId() {
    return categoryId;
  }

  public DocumentStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void archive(Instant now) {
    status = DocumentStatus.ARCHIVED;
    updatedAt = now;
  }
}
