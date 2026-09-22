package com.knowledgeops.knowledge.domain;

import java.util.List;

public record KnowledgeDocumentPage(List<KnowledgeDocument> content, long totalElements) {}
