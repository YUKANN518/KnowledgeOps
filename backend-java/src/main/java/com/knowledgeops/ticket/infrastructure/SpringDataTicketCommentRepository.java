package com.knowledgeops.ticket.infrastructure;

import com.knowledgeops.ticket.domain.TicketComment;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataTicketCommentRepository extends JpaRepository<TicketComment, UUID> {
  List<TicketComment> findByTicketIdOrderByCreatedAtAscIdAsc(UUID ticketId);
}
