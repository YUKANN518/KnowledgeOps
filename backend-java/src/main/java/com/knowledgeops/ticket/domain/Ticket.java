package com.knowledgeops.ticket.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class Ticket {
  @Id private UUID id;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false, columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private TicketPriority priority;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private TicketStatus status;

  @Column(name = "creator_id", nullable = false, updatable = false)
  private UUID creatorId;

  @Column(name = "department_id", nullable = false, updatable = false)
  private UUID departmentId;

  @Column(name = "assignee_id")
  private UUID assigneeId;

  @Version private long version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Ticket() {}

  public Ticket(
      UUID id,
      String title,
      String description,
      TicketPriority priority,
      UUID creatorId,
      UUID departmentId,
      Instant now) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.priority = priority;
    this.status = TicketStatus.OPEN;
    this.creatorId = creatorId;
    this.departmentId = departmentId;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public TicketPriority getPriority() {
    return priority;
  }

  public TicketStatus getStatus() {
    return status;
  }

  public UUID getCreatorId() {
    return creatorId;
  }

  public UUID getDepartmentId() {
    return departmentId;
  }

  public UUID getAssigneeId() {
    return assigneeId;
  }

  public long getVersion() {
    return version;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void assign(UUID newAssigneeId, Instant now) {
    assigneeId = newAssigneeId;
    updatedAt = now;
  }

  public void transitionTo(TicketStatus newStatus, Instant now) {
    status = newStatus;
    updatedAt = now;
  }

  public void recordComment(Instant now) {
    updatedAt = now;
  }
}
