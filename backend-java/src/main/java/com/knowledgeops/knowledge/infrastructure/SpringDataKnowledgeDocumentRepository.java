package com.knowledgeops.knowledge.infrastructure;

import com.knowledgeops.knowledge.domain.*;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

interface SpringDataKnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {
  @Query(
      """
      select d from KnowledgeDocument d
      where (:categoryId is null or d.categoryId = :categoryId)
        and (:status is null or d.status = :status)
        and (:activeOnly = false or d.status = com.knowledgeops.knowledge.domain.DocumentStatus.ACTIVE)
      """)
  Page<KnowledgeDocument> findVisible(
      @Param("categoryId") UUID categoryId,
      @Param("status") DocumentStatus status,
      @Param("activeOnly") boolean activeOnly,
      Pageable pageable);
}
