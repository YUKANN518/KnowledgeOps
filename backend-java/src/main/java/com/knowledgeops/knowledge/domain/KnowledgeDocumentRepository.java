package com.knowledgeops.knowledge.domain;

import java.util.*;

public interface KnowledgeDocumentRepository {
  KnowledgeDocument saveAndFlush(KnowledgeDocument document);

  Optional<KnowledgeDocument> findById(UUID id);

  KnowledgeDocumentPage findVisible(
      UUID categoryId, DocumentStatus status, boolean activeOnly, int page, int size);
}
