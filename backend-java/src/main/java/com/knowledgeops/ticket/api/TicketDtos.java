package com.knowledgeops.ticket.api;

import com.knowledgeops.ticket.domain.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public final class TicketDtos {
  private TicketDtos() {}

  public record Create(
      @NotBlank @Size(max = 200) String title,
      @NotBlank @Size(max = 10000) String description,
      @NotNull TicketPriority priority) {}

  public record Assign(@NotNull UUID assigneeId, @PositiveOrZero long expectedVersion) {}

  public record Transition(@NotNull TicketStatus status, @PositiveOrZero long expectedVersion) {}

  public record AddComment(
      @NotBlank @Size(max = 4000) String content, @PositiveOrZero long expectedVersion) {}

  public record TicketResponse(
      UUID id,
      String title,
      String description,
      TicketPriority priority,
      TicketStatus status,
      UUID creatorId,
      UUID assigneeId,
      long version,
      Instant createdAt,
      Instant updatedAt) {
    static TicketResponse from(Ticket ticket) {
      return new TicketResponse(
          ticket.getId(),
          ticket.getTitle(),
          ticket.getDescription(),
          ticket.getPriority(),
          ticket.getStatus(),
          ticket.getCreatorId(),
          ticket.getAssigneeId(),
          ticket.getVersion(),
          ticket.getCreatedAt(),
          ticket.getUpdatedAt());
    }
  }

  public record TicketSummary(
      UUID id,
      String title,
      TicketPriority priority,
      TicketStatus status,
      UUID creatorId,
      UUID assigneeId,
      long version,
      Instant createdAt,
      Instant updatedAt) {
    static TicketSummary from(Ticket ticket) {
      return new TicketSummary(
          ticket.getId(),
          ticket.getTitle(),
          ticket.getPriority(),
          ticket.getStatus(),
          ticket.getCreatorId(),
          ticket.getAssigneeId(),
          ticket.getVersion(),
          ticket.getCreatedAt(),
          ticket.getUpdatedAt());
    }
  }

  public record PageResponse(
      List<TicketSummary> content, int page, int size, long totalElements, int totalPages) {
    static PageResponse from(TicketPage tickets, int page, int size) {
      int pages =
          tickets.totalElements() == 0 ? 0 : (int) ((tickets.totalElements() + size - 1) / size);
      return new PageResponse(
          tickets.content().stream().map(TicketSummary::from).toList(),
          page,
          size,
          tickets.totalElements(),
          pages);
    }
  }

  public record CommentResponse(
      UUID id, UUID ticketId, UUID authorId, String content, Instant createdAt) {
    static CommentResponse from(TicketComment comment) {
      return new CommentResponse(
          comment.getId(),
          comment.getTicketId(),
          comment.getAuthorId(),
          comment.getContent(),
          comment.getCreatedAt());
    }
  }
}
