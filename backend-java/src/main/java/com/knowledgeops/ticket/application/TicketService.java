package com.knowledgeops.ticket.application;

import com.knowledgeops.audit.application.AuditService;
import com.knowledgeops.audit.domain.AuditAction;
import com.knowledgeops.auth.application.AuthenticatedUser;
import com.knowledgeops.shared.domain.*;
import com.knowledgeops.ticket.domain.*;
import com.knowledgeops.user.domain.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {
  private final TicketRepository tickets;
  private final TicketCommentRepository comments;
  private final UserRepository users;
  private final AuditService audit;
  private final Clock clock;

  public TicketService(
      TicketRepository tickets,
      TicketCommentRepository comments,
      UserRepository users,
      AuditService audit,
      Clock clock) {
    this.tickets = tickets;
    this.comments = comments;
    this.users = users;
    this.audit = audit;
    this.clock = clock;
  }

  @Transactional
  public Ticket create(
      String title, String description, TicketPriority priority, AuthenticatedUser actor) {
    User creator = activeUser(actor.id());
    Instant now = clock.instant();
    Ticket ticket =
        tickets.saveAndFlush(
            new Ticket(
                UUID.randomUUID(),
                title.trim(),
                description.trim(),
                priority,
                actor.id(),
                creator.getDepartmentId(),
                now));
    audit.record(
        "USER",
        actor.id(),
        AuditAction.TICKET_CREATED,
        "TICKET",
        ticket.getId().toString(),
        Map.of("priority", priority.name(), "status", ticket.getStatus().name()));
    return ticket;
  }

  @Transactional(readOnly = true)
  public TicketPage list(
      TicketStatus status, TicketPriority priority, int page, int size, AuthenticatedUser actor) {
    validatePage(page, size);
    User user = activeUser(actor.id());
    if (has(actor, "ticket.read.all"))
      return tickets.findVisible(null, null, status, priority, page, size);
    if (has(actor, "ticket.read.department"))
      return tickets.findVisible(null, user.getDepartmentId(), status, priority, page, size);
    if (has(actor, "ticket.read.own"))
      return tickets.findVisible(actor.id(), null, status, priority, page, size);
    throw denied();
  }

  @Transactional(readOnly = true)
  public Ticket get(UUID id, AuthenticatedUser actor) {
    Ticket ticket = find(id);
    requireRead(ticket, actor);
    return ticket;
  }

  @Transactional
  public Ticket assign(UUID id, UUID assigneeId, long expectedVersion, AuthenticatedUser actor) {
    Ticket ticket = find(id);
    requireAssignmentAccess(ticket, actor);
    requireVersion(ticket, expectedVersion);
    User assignee = activeUserForAssignment(assigneeId);
    if (!ticket.getDepartmentId().equals(assignee.getDepartmentId()) || !isSupport(assignee))
      throw invalidAssignee();
    UUID previous = ticket.getAssigneeId();
    ticket.assign(assigneeId, clock.instant());
    Ticket saved = tickets.saveAndFlush(ticket);
    Map<String, Object> metadata = new LinkedHashMap<>();
    if (previous != null) metadata.put("previousAssigneeId", previous.toString());
    metadata.put("assigneeId", assigneeId.toString());
    audit.record(
        "USER", actor.id(), AuditAction.TICKET_ASSIGNED, "TICKET", id.toString(), metadata);
    return saved;
  }

  @Transactional
  public Ticket transition(
      UUID id, TicketStatus target, long expectedVersion, AuthenticatedUser actor) {
    Ticket ticket = find(id);
    requireTransitionAccess(ticket, actor);
    requireVersion(ticket, expectedVersion);
    TicketStatus previous = ticket.getStatus();
    if (!TicketStatusPolicy.allows(previous, target)
        || (target == TicketStatus.IN_PROGRESS && ticket.getAssigneeId() == null)) {
      throw new BusinessException(
          ErrorCode.INVALID_TICKET_STATUS,
          HttpStatus.CONFLICT,
          "Ticket status transition is not allowed");
    }
    ticket.transitionTo(target, clock.instant());
    Ticket saved = tickets.saveAndFlush(ticket);
    audit.record(
        "USER",
        actor.id(),
        AuditAction.TICKET_STATUS_CHANGED,
        "TICKET",
        id.toString(),
        Map.of("from", previous.name(), "to", target.name()));
    return saved;
  }

  @Transactional
  public TicketComment addComment(
      UUID id, String content, long expectedVersion, AuthenticatedUser actor) {
    Ticket ticket = find(id);
    requireCommentAccess(ticket, actor);
    requireVersion(ticket, expectedVersion);
    Instant now = clock.instant();
    TicketComment comment =
        comments.save(new TicketComment(UUID.randomUUID(), id, actor.id(), content.trim(), now));
    ticket.recordComment(now);
    tickets.saveAndFlush(ticket);
    audit.record(
        "USER",
        actor.id(),
        AuditAction.TICKET_COMMENT_ADDED,
        "TICKET",
        id.toString(),
        Map.of("commentId", comment.getId().toString()));
    return comment;
  }

  @Transactional(readOnly = true)
  public List<TicketComment> comments(UUID id, AuthenticatedUser actor) {
    Ticket ticket = find(id);
    requireCommentAccess(ticket, actor);
    return comments.findByTicketId(id);
  }

  private Ticket find(UUID id) {
    return tickets
        .findById(id)
        .orElseThrow(
            () ->
                new BusinessException(
                    ErrorCode.TICKET_NOT_FOUND, HttpStatus.NOT_FOUND, "Ticket not found"));
  }

  private User activeUser(UUID id) {
    User user = users.findById(id).orElseThrow(BusinessException::notFound);
    if (user.getStatus() != UserStatus.ACTIVE) throw denied();
    return user;
  }

  private User activeUserForAssignment(UUID id) {
    return users
        .findById(id)
        .filter(user -> user.getStatus() == UserStatus.ACTIVE)
        .orElseThrow(this::invalidAssignee);
  }

  private void requireRead(Ticket ticket, AuthenticatedUser actor) {
    if (has(actor, "ticket.read.all")) return;
    User user = activeUser(actor.id());
    if (has(actor, "ticket.read.department")
        && ticket.getDepartmentId().equals(user.getDepartmentId())) return;
    if (has(actor, "ticket.read.own") && ticket.getCreatorId().equals(actor.id())) return;
    throw denied();
  }

  private void requireCommentAccess(Ticket ticket, AuthenticatedUser actor) {
    if (!has(actor, "ticket.comment.public")) throw denied();
    requireRead(ticket, actor);
  }

  private void requireAssignmentAccess(Ticket ticket, AuthenticatedUser actor) {
    if (has(actor, "ticket.assign.all")) return;
    User user = activeUser(actor.id());
    if (has(actor, "ticket.assign.department")
        && ticket.getDepartmentId().equals(user.getDepartmentId())) return;
    throw denied();
  }

  private void requireTransitionAccess(Ticket ticket, AuthenticatedUser actor) {
    if (has(actor, "ticket.transition.all")) return;
    if (has(actor, "ticket.transition.assigned") && actor.id().equals(ticket.getAssigneeId()))
      return;
    throw denied();
  }

  private boolean isSupport(User user) {
    return user.getRoles().stream()
        .flatMap(role -> role.getPermissions().stream())
        .map(Permission::getCode)
        .anyMatch(
            code ->
                code.equals("ticket.transition.assigned") || code.equals("ticket.transition.all"));
  }

  private boolean has(AuthenticatedUser actor, String permission) {
    return actor.permissions().contains(permission);
  }

  private void requireVersion(Ticket ticket, long expectedVersion) {
    if (ticket.getVersion() != expectedVersion)
      throw new BusinessException(
          ErrorCode.TICKET_VERSION_CONFLICT, HttpStatus.CONFLICT, "Ticket version changed");
  }

  private void validatePage(int page, int size) {
    if (page < 0 || page > 1000 || size < 1 || size > 100)
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR,
          HttpStatus.UNPROCESSABLE_ENTITY,
          "Page must be 0..1000 and size must be 1..100");
  }

  private BusinessException denied() {
    return new BusinessException(
        ErrorCode.TICKET_ACCESS_DENIED, HttpStatus.FORBIDDEN, "Ticket access is denied");
  }

  private BusinessException invalidAssignee() {
    return new BusinessException(
        ErrorCode.INVALID_ASSIGNEE,
        HttpStatus.UNPROCESSABLE_ENTITY,
        "Assignee must be an active support user in the ticket department");
  }
}
