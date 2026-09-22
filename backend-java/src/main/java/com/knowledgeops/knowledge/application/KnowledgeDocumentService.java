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
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Service
public class KnowledgeDocumentService {
  private final KnowledgeDocumentRepository documents;
  private final KnowledgeCategoryRepository categories;
  private final FileStorageService storage;
  private final AuditService audit;
  private final Clock clock;

  public KnowledgeDocumentService(
      KnowledgeDocumentRepository documents,
      KnowledgeCategoryRepository categories,
      FileStorageService storage,
      AuditService audit,
      Clock clock) {
    this.documents = documents;
    this.categories = categories;
    this.storage = storage;
    this.audit = audit;
    this.clock = clock;
  }

  @Transactional
  public KnowledgeDocument upload(UUID categoryId, MultipartFile file, AuthenticatedUser actor) {
    requireCategory(categoryId);
    FileStorageService.StoredFile stored = storage.store(file);
    registerRollbackCleanup(stored.storageKey());
    KnowledgeDocument document =
        documents.saveAndFlush(
            new KnowledgeDocument(
                UUID.randomUUID(),
                stored.originalFilename(),
                stored.storedFilename(),
                stored.storageKey(),
                stored.contentType(),
                stored.fileSize(),
                actor.id(),
                categoryId,
                clock.instant()));
    audit.record(
        "USER",
        actor.id(),
        AuditAction.KNOWLEDGE_DOCUMENT_UPLOADED,
        "KNOWLEDGE_DOCUMENT",
        document.getId().toString(),
        Map.of("contentType", document.getContentType(), "fileSize", document.getFileSize()));
    return document;
  }

  @Transactional(readOnly = true)
  public KnowledgeDocumentPage list(
      UUID categoryId, DocumentStatus status, int page, int size, AuthenticatedUser actor) {
    validatePage(page, size);
    boolean activeOnly = !canWrite(actor);
    return documents.findVisible(
        categoryId, activeOnly ? DocumentStatus.ACTIVE : status, activeOnly, page, size);
  }

  @Transactional(readOnly = true)
  public KnowledgeDocument get(UUID id, AuthenticatedUser actor) {
    KnowledgeDocument document = find(id);
    requireRead(document, actor);
    return document;
  }

  @Transactional
  public DownloadedDocument download(UUID id, AuthenticatedUser actor) {
    KnowledgeDocument document = find(id);
    requireRead(document, actor);
    byte[] bytes = storage.read(document.getStorageKey());
    audit.record(
        "USER",
        actor.id(),
        AuditAction.KNOWLEDGE_DOCUMENT_DOWNLOADED,
        "KNOWLEDGE_DOCUMENT",
        id.toString(),
        Map.of("fileSize", document.getFileSize()));
    return new DownloadedDocument(document, bytes);
  }

  @Transactional
  public KnowledgeDocument archive(UUID id, AuthenticatedUser actor) {
    KnowledgeDocument document = find(id);
    document.archive(clock.instant());
    KnowledgeDocument saved = documents.saveAndFlush(document);
    audit.record(
        "USER",
        actor.id(),
        AuditAction.KNOWLEDGE_DOCUMENT_ARCHIVED,
        "KNOWLEDGE_DOCUMENT",
        id.toString(),
        Map.of("status", "ARCHIVED"));
    return saved;
  }

  private KnowledgeDocument find(UUID id) {
    return documents
        .findById(id)
        .orElseThrow(
            () ->
                new BusinessException(
                    ErrorCode.KNOWLEDGE_DOCUMENT_NOT_FOUND,
                    HttpStatus.NOT_FOUND,
                    "Knowledge document not found"));
  }

  private void requireRead(KnowledgeDocument document, AuthenticatedUser actor) {
    if (document.getStatus() == DocumentStatus.ACTIVE || canWrite(actor)) return;
    throw new BusinessException(
        ErrorCode.KNOWLEDGE_ACCESS_DENIED, HttpStatus.FORBIDDEN, "Knowledge access is denied");
  }

  private void requireCategory(UUID id) {
    if (categories.findById(id).isEmpty())
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR,
          HttpStatus.UNPROCESSABLE_ENTITY,
          "Knowledge category does not exist");
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

  private void registerRollbackCleanup(String storageKey) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) storage.delete(storageKey);
          }
        });
  }

  public record DownloadedDocument(KnowledgeDocument metadata, byte[] content) {}
}
