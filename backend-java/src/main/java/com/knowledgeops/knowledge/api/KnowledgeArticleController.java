package com.knowledgeops.knowledge.api;

import com.knowledgeops.auth.application.AuthenticatedUser;
import com.knowledgeops.knowledge.application.KnowledgeArticleService;
import com.knowledgeops.knowledge.domain.ArticleStatus;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/knowledge/articles")
public class KnowledgeArticleController {
  private final KnowledgeArticleService service;

  public KnowledgeArticleController(KnowledgeArticleService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyAuthority('knowledge.write','knowledge.admin')")
  KnowledgeDtos.ArticleResponse create(
      @Valid @RequestBody KnowledgeDtos.CreateArticle input,
      @AuthenticationPrincipal AuthenticatedUser actor) {
    return KnowledgeDtos.ArticleResponse.from(
        service.create(input.title(), input.content(), input.categoryId(), actor));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('knowledge.read')")
  KnowledgeDtos.ArticlePage list(
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) ArticleStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @AuthenticationPrincipal AuthenticatedUser actor) {
    return KnowledgeDtos.ArticlePage.from(
        service.list(categoryId, status, page, size, actor), page, size);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('knowledge.read')")
  KnowledgeDtos.ArticleResponse get(
      @PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser actor) {
    return KnowledgeDtos.ArticleResponse.from(service.get(id, actor));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('knowledge.write','knowledge.admin')")
  KnowledgeDtos.ArticleResponse update(
      @PathVariable UUID id,
      @Valid @RequestBody KnowledgeDtos.UpdateArticle input,
      @AuthenticationPrincipal AuthenticatedUser actor) {
    return KnowledgeDtos.ArticleResponse.from(
        service.update(
            id,
            input.title(),
            input.content(),
            input.categoryId(),
            input.expectedVersion(),
            actor));
  }

  @PostMapping("/{id}/publish")
  @PreAuthorize("hasAnyAuthority('knowledge.write','knowledge.admin')")
  KnowledgeDtos.ArticleResponse publish(
      @PathVariable UUID id,
      @Valid @RequestBody KnowledgeDtos.VersionCommand input,
      @AuthenticationPrincipal AuthenticatedUser actor) {
    return KnowledgeDtos.ArticleResponse.from(service.publish(id, input.expectedVersion(), actor));
  }

  @PostMapping("/{id}/archive")
  @PreAuthorize("hasAnyAuthority('knowledge.write','knowledge.admin')")
  KnowledgeDtos.ArticleResponse archive(
      @PathVariable UUID id,
      @Valid @RequestBody KnowledgeDtos.VersionCommand input,
      @AuthenticationPrincipal AuthenticatedUser actor) {
    return KnowledgeDtos.ArticleResponse.from(service.archive(id, input.expectedVersion(), actor));
  }
}
