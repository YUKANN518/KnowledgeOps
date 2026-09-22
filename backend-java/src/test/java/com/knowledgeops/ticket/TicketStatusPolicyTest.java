package com.knowledgeops.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgeops.ticket.domain.*;
import org.junit.jupiter.api.Test;

class TicketStatusPolicyTest {
  @Test
  void acceptsOnlyThePhaseTwoTransitions() {
    assertThat(TicketStatusPolicy.allows(TicketStatus.OPEN, TicketStatus.IN_PROGRESS)).isTrue();
    assertThat(TicketStatusPolicy.allows(TicketStatus.IN_PROGRESS, TicketStatus.OPEN)).isTrue();
    assertThat(TicketStatusPolicy.allows(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED)).isTrue();
    assertThat(TicketStatusPolicy.allows(TicketStatus.RESOLVED, TicketStatus.CLOSED)).isTrue();
    assertThat(TicketStatusPolicy.allows(TicketStatus.OPEN, TicketStatus.CLOSED)).isFalse();
    assertThat(TicketStatusPolicy.allows(TicketStatus.CLOSED, TicketStatus.OPEN)).isFalse();
  }
}
