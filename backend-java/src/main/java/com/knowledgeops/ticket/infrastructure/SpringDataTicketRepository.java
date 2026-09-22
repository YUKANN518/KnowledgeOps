package com.knowledgeops.ticket.infrastructure;

import com.knowledgeops.ticket.domain.*;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

interface SpringDataTicketRepository extends JpaRepository<Ticket, UUID> {
  @Query(
      """
      select t from Ticket t
      where (:creatorId is null or t.creatorId = :creatorId)
        and (:departmentId is null or t.departmentId = :departmentId)
        and (:status is null or t.status = :status)
        and (:priority is null or t.priority = :priority)
      """)
  Page<Ticket> findVisible(
      @Param("creatorId") UUID creatorId,
      @Param("departmentId") UUID departmentId,
      @Param("status") TicketStatus status,
      @Param("priority") TicketPriority priority,
      Pageable pageable);
}
