package com.knowledgeops.knowledge.api;

import com.knowledgeops.auth.application.AuthenticatedUser;
import com.knowledgeops.knowledge.application.KnowledgeDocumentService;
import com.knowledgeops.knowledge.domain.DocumentStatus;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/v1/documents")
public class KnowledgeDocumentController {
  private final KnowledgeDocumentService service;

  public KnowledgeDocumentController(KnowledgeDocumentService service) {
    this.service = service;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyAuthority('knowledge.write','knowledge.admin')")
  KnowledgeDtos.DocumentResponse upload(
      @RequestParam @NotNull UUID categoryId,
      @RequestPart("file") MultipartFile file,
      @AuthenticationPrincipal AuthenticatedUser actor) {
    return KnowledgeDtos.DocumentResponse.from(service.upload(categoryId, file, actor));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('knowledge.read')")
  KnowledgeDtos.DocumentPage list(
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) DocumentStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @AuthenticationPrincipal AuthenticatedUser actor) {
    return KnowledgeDtos.DocumentPage.from(
        service.list(categoryId, status, page, size, actor), page, size);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('knowledge.read')")
  KnowledgeDtos.DocumentResponse get(
      @PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser actor) {
    return KnowledgeDtos.DocumentResponse.from(service.get(id, actor));
  }

  @GetMapping("/{id}/content")
  @PreAuthorize("hasAuthority('knowledge.read')")
  ResponseEntity<byte[]> download(
      @PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser actor) {
    KnowledgeDocumentService.DownloadedDocument download = service.download(id, actor);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(download.metadata().getContentType()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(download.metadata().getOriginalFilename(), StandardCharsets.UTF_8)
                .build()
                .toString())
        .header("X-Content-Type-Options", "nosniff")
        .contentLength(download.content().length)
        .body(download.content());
  }

  @PostMapping("/{id}/archive")
  @PreAuthorize("hasAnyAuthority('knowledge.write','knowledge.admin')")
  KnowledgeDtos.DocumentResponse archive(
      @PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser actor) {
    return KnowledgeDtos.DocumentResponse.from(service.archive(id, actor));
  }
}
