package com.knowledgeops.ticket.infrastructure;

import com.knowledgeops.ticket.domain.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
class JpaTicketCommentRepositoryAdapter implements TicketCommentRepository {
  private final SpringDataTicketCommentRepository delegate;

  JpaTicketCommentRepositoryAdapter(SpringDataTicketCommentRepository delegate) {
    this.delegate = delegate;
  }

  public TicketComment save(TicketComment comment) {
    return delegate.save(comment);
  }

  public List<TicketComment> findByTicketId(UUID ticketId) {
    return delegate.findByTicketIdOrderByCreatedAtAscIdAsc(ticketId);
  }
}
