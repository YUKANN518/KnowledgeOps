package com.knowledgeops.ticket.domain;

import java.util.*;

public interface TicketCommentRepository {
  TicketComment save(TicketComment comment);

  List<TicketComment> findByTicketId(UUID ticketId);
}
