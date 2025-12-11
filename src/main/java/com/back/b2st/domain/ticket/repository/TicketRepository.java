package com.back.b2st.domain.ticket.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.b2st.domain.ticket.entity.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
	Optional<Ticket> findByReservationIdAndMemberIdAndSeatId(Long reservationId, Long memberId, Long seatId);
}
