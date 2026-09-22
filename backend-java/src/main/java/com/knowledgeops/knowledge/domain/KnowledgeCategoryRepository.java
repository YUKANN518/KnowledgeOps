package com.knowledgeops.knowledge.domain;

import java.util.*;

public interface KnowledgeCategoryRepository {
  KnowledgeCategory saveAndFlush(KnowledgeCategory category);

  Optional<KnowledgeCategory> findById(UUID id);

  List<KnowledgeCategory> findAll();
}
