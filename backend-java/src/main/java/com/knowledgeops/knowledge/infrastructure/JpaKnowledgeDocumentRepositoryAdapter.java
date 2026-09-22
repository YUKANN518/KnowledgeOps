package com.knowledgeops.knowledge.infrastructure;

import com.knowledgeops.knowledge.domain.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

@Repository
class JpaKnowledgeDocumentRepositoryAdapter implements KnowledgeDocumentRepository {
  private final SpringDataKnowledgeDocumentRepository delegate;

  JpaKnowledgeDocumentRepositoryAdapter(SpringDataKnowledgeDocumentRepository delegate) {
    this.delegate = delegate;
  }

  public KnowledgeDocument saveAndFlush(KnowledgeDocument document) {
    return delegate.saveAndFlush(document);
  }

  public Optional<KnowledgeDocument> findById(UUID id) {
    return delegate.findById(id);
  }

  public KnowledgeDocumentPage findVisible(
      UUID categoryId, DocumentStatus status, boolean activeOnly, int page, int size) {
    Page<KnowledgeDocument> result =
        delegate.findVisible(
            categoryId,
            status,
            activeOnly,
            PageRequest.of(
                page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
    return new KnowledgeDocumentPage(result.getContent(), result.getTotalElements());
  }
}
