package com.gzhu.repair.repository;

import com.gzhu.repair.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    boolean existsByTicketNo(String ticketNo);

    long countByStatus(String status);
}
