package com.back.b2st.domain.ticket.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.b2st.domain.ticket.entity.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
}
