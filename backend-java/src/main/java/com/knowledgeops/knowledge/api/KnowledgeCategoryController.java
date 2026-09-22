package com.knowledgeops.knowledge.api;

import com.knowledgeops.auth.application.AuthenticatedUser;
import com.knowledgeops.knowledge.application.KnowledgeCategoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/knowledge/categories")
public class KnowledgeCategoryController {
  private final KnowledgeCategoryService service;

  public KnowledgeCategoryController(KnowledgeCategoryService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyAuthority('knowledge.write','knowledge.admin')")
  KnowledgeDtos.CategoryResponse create(
      @Valid @RequestBody KnowledgeDtos.CreateCategory input,
      @AuthenticationPrincipal AuthenticatedUser actor) {
    return KnowledgeDtos.CategoryResponse.from(
        service.create(input.name(), input.description(), actor));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('knowledge.read')")
  List<KnowledgeDtos.CategoryResponse> list() {
    return service.list().stream().map(KnowledgeDtos.CategoryResponse::from).toList();
  }
}
