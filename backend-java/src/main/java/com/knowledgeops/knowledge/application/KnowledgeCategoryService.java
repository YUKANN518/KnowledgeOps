package com.knowledgeops.knowledge.application;

import com.knowledgeops.auth.application.AuthenticatedUser;
import com.knowledgeops.knowledge.domain.*;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeCategoryService {
  private final KnowledgeCategoryRepository categories;
  private final Clock clock;

  public KnowledgeCategoryService(KnowledgeCategoryRepository categories, Clock clock) {
    this.categories = categories;
    this.clock = clock;
  }

  @Transactional
  public KnowledgeCategory create(String name, String description, AuthenticatedUser actor) {
    return categories.saveAndFlush(
        new KnowledgeCategory(
            UUID.randomUUID(), name.trim(), cleanNullable(description), clock.instant()));
  }

  @Transactional(readOnly = true)
  public List<KnowledgeCategory> list() {
    return categories.findAll();
  }

  private String cleanNullable(String value) {
    if (value == null || value.isBlank()) return null;
    return value.trim();
  }
}
