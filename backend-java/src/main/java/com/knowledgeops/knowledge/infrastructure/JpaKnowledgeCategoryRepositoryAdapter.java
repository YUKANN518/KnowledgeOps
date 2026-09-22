package com.knowledgeops.knowledge.infrastructure;

import com.knowledgeops.knowledge.domain.*;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class JpaKnowledgeCategoryRepositoryAdapter implements KnowledgeCategoryRepository {
  private final SpringDataKnowledgeCategoryRepository delegate;

  JpaKnowledgeCategoryRepositoryAdapter(SpringDataKnowledgeCategoryRepository delegate) {
    this.delegate = delegate;
  }

  public KnowledgeCategory saveAndFlush(KnowledgeCategory category) {
    return delegate.saveAndFlush(category);
  }

  public Optional<KnowledgeCategory> findById(UUID id) {
    return delegate.findById(id);
  }

  public List<KnowledgeCategory> findAll() {
    return delegate.findAll(Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")));
  }
}
