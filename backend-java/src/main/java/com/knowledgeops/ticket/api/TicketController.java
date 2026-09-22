package com.knowledgeops.ticket.api;

import com.knowledgeops.auth.application.AuthenticatedUser;
import com.knowledgeops.ticket.application.TicketService;
import com.knowledgeops.ticket.domain.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {
  private final TicketService service;

  public TicketController(TicketService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('ticket.create')")
  TicketDtos.TicketResponse create(
      @Valid @RequestBody TicketDtos.Create input,
      @AuthenticationPrincipal AuthenticatedUser actor) {
    return TicketDtos.TicketResponse.from(
        service.create(input.title(), input.description(), input.priority(), actor));
  }

  @GetMapping
  TicketDtos.PageResponse list(
      @RequestParam(required = false) TicketStatus status,
      @RequestParam(required = false) TicketPriority priority,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @AuthenticationPrincipal AuthenticatedUser actor) {
    return TicketDtos.PageResponse.from(
        service.list(status, priority, page, size, actor), page, size);
  }

  @GetMapping("/{id}")
  TicketDtos.TicketResponse get(
      @PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser actor) {
    return TicketDtos.TicketResponse.from(service.get(id, actor));
  }

  @PostMapping("/{id}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('ticket.comment.public')")
  TicketDtos.CommentResponse comment(
      @PathVariable UUID id,
      @Valid @RequestBody TicketDtos.AddComment input,
      @AuthenticationPrincipal AuthenticatedUser actor) {
    return TicketDtos.CommentResponse.from(
        service.addComment(id, input.content(), input.expectedVersion(), actor));
  }

  @GetMapping("/{id}/comments")
  @PreAuthorize("hasAuthority('ticket.comment.public')")
  List<TicketDtos.CommentResponse> comments(
      @PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser actor) {
    return service.comments(id, actor).stream().map(TicketDtos.CommentResponse::from).toList();
  }

  @PostMapping("/{id}/assignments")
  @PreAuthorize("hasAnyAuthority('ticket.assign.department','ticket.assign.all')")
  TicketDtos.TicketResponse assign(
      @PathVariable UUID id,
      @Valid @RequestBody TicketDtos.Assign input,
      @AuthenticationPrincipal AuthenticatedUser actor) {
    return TicketDtos.TicketResponse.from(
        service.assign(id, input.assigneeId(), input.expectedVersion(), actor));
  }

  @PostMapping("/{id}/transitions")
  @PreAuthorize("hasAnyAuthority('ticket.transition.assigned','ticket.transition.all')")
  TicketDtos.TicketResponse transition(
      @PathVariable UUID id,
      @Valid @RequestBody TicketDtos.Transition input,
      @AuthenticationPrincipal AuthenticatedUser actor) {
    return TicketDtos.TicketResponse.from(
        service.transition(id, input.status(), input.expectedVersion(), actor));
  }
}
