package com.knowledgeops.knowledge.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_articles")
public class KnowledgeArticle {
  @Id private UUID id;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false, columnDefinition = "text")
  private String content;

  @Column(name = "category_id", nullable = false)
  private UUID categoryId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private ArticleStatus status;

  @Column(name = "author_id", nullable = false, updatable = false)
  private UUID authorId;

  @Version private long version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected KnowledgeArticle() {}

  public KnowledgeArticle(
      UUID id, String title, String content, UUID categoryId, UUID authorId, Instant now) {
    this.id = id;
    this.title = title;
    this.content = content;
    this.categoryId = categoryId;
    this.status = ArticleStatus.DRAFT;
    this.authorId = authorId;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }

  public UUID getCategoryId() {
    return categoryId;
  }

  public ArticleStatus getStatus() {
    return status;
  }

  public UUID getAuthorId() {
    return authorId;
  }

  public long getVersion() {
    return version;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void update(String title, String content, UUID categoryId, Instant now) {
    this.title = title;
    this.content = content;
    this.categoryId = categoryId;
    this.updatedAt = now;
  }

  public void publish(Instant now) {
    this.status = ArticleStatus.PUBLISHED;
    this.updatedAt = now;
  }

  public void archive(Instant now) {
    this.status = ArticleStatus.ARCHIVED;
    this.updatedAt = now;
  }
}
