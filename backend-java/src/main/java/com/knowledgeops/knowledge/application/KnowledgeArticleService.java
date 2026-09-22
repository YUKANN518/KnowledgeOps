package com.knowledgeops.knowledge.application;

import com.knowledgeops.audit.application.AuditService;
import com.knowledgeops.audit.domain.AuditAction;
import com.knowledgeops.auth.application.AuthenticatedUser;
import com.knowledgeops.knowledge.domain.*;
import com.knowledgeops.shared.domain.*;
import java.time.Clock;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeArticleService {
  private final KnowledgeArticleRepository articles;
  private final KnowledgeCategoryRepository categories;
  private final AuditService audit;
  private final Clock clock;

  public KnowledgeArticleService(
      KnowledgeArticleRepository articles,
      KnowledgeCategoryRepository categories,
      AuditService audit,
      Clock clock) {
    this.articles = articles;
    this.categories = categories;
    this.audit = audit;
    this.clock = clock;
  }

  @Transactional
  public KnowledgeArticle create(
      String title, String content, UUID categoryId, AuthenticatedUser actor) {
    requireCategory(categoryId);
    KnowledgeArticle article =
        articles.saveAndFlush(
            new KnowledgeArticle(
                UUID.randomUUID(),
                title.trim(),
                content.trim(),
                categoryId,
                actor.id(),
                clock.instant()));
    record(actor, AuditAction.KNOWLEDGE_ARTICLE_CREATED, article, Map.of("status", "DRAFT"));
    return article;
  }

  @Transactional(readOnly = true)
  public KnowledgeArticlePage list(
      UUID categoryId, ArticleStatus status, int page, int size, AuthenticatedUser actor) {
    validatePage(page, size);
    boolean publishedOnly = !canWrite(actor);
    return articles.findVisible(
        categoryId, publishedOnly ? ArticleStatus.PUBLISHED : status, publishedOnly, page, size);
  }

  @Transactional(readOnly = true)
  public KnowledgeArticle get(UUID id, AuthenticatedUser actor) {
    KnowledgeArticle article = find(id);
    if (!canWrite(actor) && article.getStatus() != ArticleStatus.PUBLISHED) throw denied();
    return article;
  }

  @Transactional
  public KnowledgeArticle update(
      UUID id,
      String title,
      String content,
      UUID categoryId,
      long expectedVersion,
      AuthenticatedUser actor) {
    KnowledgeArticle article = find(id);
    requireVersion(article, expectedVersion);
    requireCategory(categoryId);
    if (article.getStatus() == ArticleStatus.ARCHIVED)
      throw stateConflict("Archived articles cannot be edited");
    article.update(title.trim(), content.trim(), categoryId, clock.instant());
    KnowledgeArticle saved = articles.saveAndFlush(article);
    record(
        actor,
        AuditAction.KNOWLEDGE_ARTICLE_UPDATED,
        saved,
        Map.of("status", saved.getStatus().name()));
    return saved;
  }

  @Transactional
  public KnowledgeArticle publish(UUID id, long expectedVersion, AuthenticatedUser actor) {
    KnowledgeArticle article = find(id);
    requireVersion(article, expectedVersion);
    if (article.getStatus() == ArticleStatus.ARCHIVED)
      throw stateConflict("Archived articles cannot be published");
    article.publish(clock.instant());
    KnowledgeArticle saved = articles.saveAndFlush(article);
    record(actor, AuditAction.KNOWLEDGE_ARTICLE_PUBLISHED, saved, Map.of("status", "PUBLISHED"));
    return saved;
  }

  @Transactional
  public KnowledgeArticle archive(UUID id, long expectedVersion, AuthenticatedUser actor) {
    KnowledgeArticle article = find(id);
    requireVersion(article, expectedVersion);
    article.archive(clock.instant());
    KnowledgeArticle saved = articles.saveAndFlush(article);
    record(actor, AuditAction.KNOWLEDGE_ARTICLE_ARCHIVED, saved, Map.of("status", "ARCHIVED"));
    return saved;
  }

  private KnowledgeArticle find(UUID id) {
    return articles
        .findById(id)
        .orElseThrow(
            () ->
                new BusinessException(
                    ErrorCode.KNOWLEDGE_ARTICLE_NOT_FOUND,
                    HttpStatus.NOT_FOUND,
                    "Knowledge article not found"));
  }

  private void requireCategory(UUID id) {
    if (categories.findById(id).isEmpty())
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR,
          HttpStatus.UNPROCESSABLE_ENTITY,
          "Knowledge category does not exist");
  }

  private void requireVersion(KnowledgeArticle article, long expectedVersion) {
    if (article.getVersion() != expectedVersion)
      throw new BusinessException(
          ErrorCode.ARTICLE_VERSION_CONFLICT, HttpStatus.CONFLICT, "Article version changed");
  }

  private void record(
      AuthenticatedUser actor,
      AuditAction action,
      KnowledgeArticle article,
      Map<String, Object> metadata) {
    audit.record(
        "USER", actor.id(), action, "KNOWLEDGE_ARTICLE", article.getId().toString(), metadata);
  }

  private boolean canWrite(AuthenticatedUser actor) {
    return actor.permissions().contains("knowledge.write")
        || actor.permissions().contains("knowledge.admin");
  }

  private void validatePage(int page, int size) {
    if (page < 0 || page > 1000 || size < 1 || size > 100)
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR,
          HttpStatus.UNPROCESSABLE_ENTITY,
          "Page must be 0..1000 and size must be 1..100");
  }

  private BusinessException denied() {
    return new BusinessException(
        ErrorCode.KNOWLEDGE_ACCESS_DENIED, HttpStatus.FORBIDDEN, "Knowledge access is denied");
  }

  private BusinessException stateConflict(String message) {
    return new BusinessException(ErrorCode.STATE_CONFLICT, HttpStatus.CONFLICT, message);
  }
}
