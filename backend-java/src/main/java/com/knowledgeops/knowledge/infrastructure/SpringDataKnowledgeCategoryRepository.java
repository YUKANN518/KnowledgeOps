package com.knowledgeops.knowledge.infrastructure;

import com.knowledgeops.knowledge.domain.KnowledgeCategory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataKnowledgeCategoryRepository extends JpaRepository<KnowledgeCategory, UUID> {}
