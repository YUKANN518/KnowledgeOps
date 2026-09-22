package com.knowledgeops.ticket.domain;

import java.util.Set;

public final class TicketStatusPolicy {
  private TicketStatusPolicy() {}

  public static boolean allows(TicketStatus from, TicketStatus to) {
    return switch (from) {
      case OPEN -> to == TicketStatus.IN_PROGRESS;
      case IN_PROGRESS -> Set.of(TicketStatus.OPEN, TicketStatus.RESOLVED).contains(to);
      case RESOLVED -> to == TicketStatus.CLOSED;
      case CLOSED -> false;
    };
  }
}
