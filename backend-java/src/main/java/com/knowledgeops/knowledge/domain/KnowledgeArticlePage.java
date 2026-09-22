package com.knowledgeops.knowledge.domain;

import java.util.List;

public record KnowledgeArticlePage(List<KnowledgeArticle> content, long totalElements) {}
