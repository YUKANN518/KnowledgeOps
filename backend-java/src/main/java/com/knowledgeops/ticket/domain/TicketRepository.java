package com.knowledgeops.ticket.domain;

import java.util.*;

public interface TicketRepository {
  Optional<Ticket> findById(UUID id);

  Ticket saveAndFlush(Ticket ticket);

  TicketPage findVisible(
      UUID creatorId,
      UUID departmentId,
      TicketStatus status,
      TicketPriority priority,
      int page,
      int size);
}
