package com.knowledgeops.ticket.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_comments")
public class TicketComment {
  @Id private UUID id;

  @Column(name = "ticket_id", nullable = false, updatable = false)
  private UUID ticketId;

  @Column(name = "author_id", nullable = false, updatable = false)
  private UUID authorId;

  @Column(nullable = false, columnDefinition = "text", updatable = false)
  private String content;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected TicketComment() {}

  public TicketComment(UUID id, UUID ticketId, UUID authorId, String content, Instant createdAt) {
    this.id = id;
    this.ticketId = ticketId;
    this.authorId = authorId;
    this.content = content;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getTicketId() {
    return ticketId;
  }

  public UUID getAuthorId() {
    return authorId;
  }

  public String getContent() {
    return content;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
