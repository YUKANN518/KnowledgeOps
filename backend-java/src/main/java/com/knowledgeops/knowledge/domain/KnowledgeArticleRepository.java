package com.knowledgeops.knowledge.domain;

import java.util.*;

public interface KnowledgeArticleRepository {
  KnowledgeArticle saveAndFlush(KnowledgeArticle article);

  Optional<KnowledgeArticle> findById(UUID id);

  KnowledgeArticlePage findVisible(
      UUID categoryId, ArticleStatus status, boolean publishedOnly, int page, int size);
}
