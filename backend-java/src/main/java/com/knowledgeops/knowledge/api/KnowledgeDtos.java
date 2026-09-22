package com.knowledgeops.knowledge.api;

import com.knowledgeops.knowledge.domain.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public final class KnowledgeDtos {
  private KnowledgeDtos() {}

  public record CreateCategory(
      @NotBlank @Size(max = 100) String name, @Size(max = 1000) String description) {}

  public record CategoryResponse(
      UUID id, String name, String description, Instant createdAt, Instant updatedAt) {
    static CategoryResponse from(KnowledgeCategory category) {
      return new CategoryResponse(
          category.getId(),
          category.getName(),
          category.getDescription(),
          category.getCreatedAt(),
          category.getUpdatedAt());
    }
  }

  public record CreateArticle(
      @NotBlank @Size(max = 200) String title,
      @NotBlank @Size(max = 100000) String content,
      @NotNull UUID categoryId) {}

  public record UpdateArticle(
      @NotBlank @Size(max = 200) String title,
      @NotBlank @Size(max = 100000) String content,
      @NotNull UUID categoryId,
      @PositiveOrZero long expectedVersion) {}

  public record VersionCommand(@PositiveOrZero long expectedVersion) {}

  public record ArticleResponse(
      UUID id,
      String title,
      String content,
      UUID categoryId,
      ArticleStatus status,
      UUID authorId,
      long version,
      Instant createdAt,
      Instant updatedAt) {
    static ArticleResponse from(KnowledgeArticle article) {
      return new ArticleResponse(
          article.getId(),
          article.getTitle(),
          article.getContent(),
          article.getCategoryId(),
          article.getStatus(),
          article.getAuthorId(),
          article.getVersion(),
          article.getCreatedAt(),
          article.getUpdatedAt());
    }
  }

  public record ArticleSummary(
      UUID id,
      String title,
      UUID categoryId,
      ArticleStatus status,
      UUID authorId,
      long version,
      Instant createdAt,
      Instant updatedAt) {
    static ArticleSummary from(KnowledgeArticle article) {
      return new ArticleSummary(
          article.getId(),
          article.getTitle(),
          article.getCategoryId(),
          article.getStatus(),
          article.getAuthorId(),
          article.getVersion(),
          article.getCreatedAt(),
          article.getUpdatedAt());
    }
  }

  public record ArticlePage(
      List<ArticleSummary> content, int page, int size, long totalElements, int totalPages) {
    static ArticlePage from(KnowledgeArticlePage articles, int page, int size) {
      int pages =
          articles.totalElements() == 0 ? 0 : (int) ((articles.totalElements() + size - 1) / size);
      return new ArticlePage(
          articles.content().stream().map(ArticleSummary::from).toList(),
          page,
          size,
          articles.totalElements(),
          pages);
    }
  }

  public record DocumentResponse(
      UUID id,
      String originalFilename,
      String contentType,
      long fileSize,
      UUID uploaderId,
      UUID categoryId,
      DocumentStatus status,
      Instant createdAt,
      Instant updatedAt) {
    static DocumentResponse from(KnowledgeDocument document) {
      return new DocumentResponse(
          document.getId(),
          document.getOriginalFilename(),
          document.getContentType(),
          document.getFileSize(),
          document.getUploaderId(),
          document.getCategoryId(),
          document.getStatus(),
          document.getCreatedAt(),
          document.getUpdatedAt());
    }
  }

  public record DocumentPage(
      List<DocumentResponse> content, int page, int size, long totalElements, int totalPages) {
    static DocumentPage from(KnowledgeDocumentPage documents, int page, int size) {
      int pages =
          documents.totalElements() == 0
              ? 0
              : (int) ((documents.totalElements() + size - 1) / size);
      return new DocumentPage(
          documents.content().stream().map(DocumentResponse::from).toList(),
          page,
          size,
          documents.totalElements(),
          pages);
    }
  }
}
