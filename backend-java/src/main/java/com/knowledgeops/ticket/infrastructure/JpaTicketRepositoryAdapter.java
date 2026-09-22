package com.knowledgeops.ticket.infrastructure;

import com.knowledgeops.ticket.domain.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

@Repository
class JpaTicketRepositoryAdapter implements TicketRepository {
  private final SpringDataTicketRepository delegate;

  JpaTicketRepositoryAdapter(SpringDataTicketRepository delegate) {
    this.delegate = delegate;
  }

  public Optional<Ticket> findById(UUID id) {
    return delegate.findById(id);
  }

  public Ticket saveAndFlush(Ticket ticket) {
    return delegate.saveAndFlush(ticket);
  }

  public TicketPage findVisible(
      UUID creatorId,
      UUID departmentId,
      TicketStatus status,
      TicketPriority priority,
      int page,
      int size) {
    Page<Ticket> result =
        delegate.findVisible(
            creatorId,
            departmentId,
            status,
            priority,
            PageRequest.of(
                page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
    return new TicketPage(result.getContent(), result.getTotalElements());
  }
}
