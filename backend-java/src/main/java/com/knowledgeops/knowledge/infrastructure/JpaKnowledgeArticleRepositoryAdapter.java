package com.knowledgeops.knowledge.infrastructure;

import com.knowledgeops.knowledge.domain.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

@Repository
class JpaKnowledgeArticleRepositoryAdapter implements KnowledgeArticleRepository {
  private final SpringDataKnowledgeArticleRepository delegate;

  JpaKnowledgeArticleRepositoryAdapter(SpringDataKnowledgeArticleRepository delegate) {
    this.delegate = delegate;
  }

  public KnowledgeArticle saveAndFlush(KnowledgeArticle article) {
    return delegate.saveAndFlush(article);
  }

  public Optional<KnowledgeArticle> findById(UUID id) {
    return delegate.findById(id);
  }

  public KnowledgeArticlePage findVisible(
      UUID categoryId, ArticleStatus status, boolean publishedOnly, int page, int size) {
    Page<KnowledgeArticle> result =
        delegate.findVisible(
            categoryId,
            status,
            publishedOnly,
            PageRequest.of(
                page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
    return new KnowledgeArticlePage(result.getContent(), result.getTotalElements());
  }
}
