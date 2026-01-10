package com.back.b2st.domain.ticket.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.b2st.domain.ticket.entity.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
	List<Ticket> findAllByReservationIdAndMemberId(Long reservationId, Long memberId);

	Optional<Ticket> findByReservationIdAndMemberIdAndSeatId(Long reservationId, Long memberId, Long seatId);

	boolean existsByReservationIdAndMemberId(Long reservationId, Long memberId);

	List<Ticket> findByMemberId(Long memberId);

	List<Ticket> findByReservationId(Long reservationId);
}
