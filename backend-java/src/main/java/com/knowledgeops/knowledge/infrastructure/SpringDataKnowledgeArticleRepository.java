package com.knowledgeops.knowledge.infrastructure;

import com.knowledgeops.knowledge.domain.*;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

interface SpringDataKnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, UUID> {
  @Query(
      """
      select a from KnowledgeArticle a
      where (:categoryId is null or a.categoryId = :categoryId)
        and (:status is null or a.status = :status)
        and (:publishedOnly = false or a.status = com.knowledgeops.knowledge.domain.ArticleStatus.PUBLISHED)
      """)
  Page<KnowledgeArticle> findVisible(
      @Param("categoryId") UUID categoryId,
      @Param("status") ArticleStatus status,
      @Param("publishedOnly") boolean publishedOnly,
      Pageable pageable);
}
