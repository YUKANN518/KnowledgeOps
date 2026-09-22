package com.knowledgeops.ticket.domain;

import java.util.List;

public record TicketPage(List<Ticket> content, long totalElements) {}
