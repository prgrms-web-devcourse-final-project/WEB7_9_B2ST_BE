package com.back.b2st.domain.ticket.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.b2st.domain.ticket.entity.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
	interface MissingTicketKey {
		Long getReservationId();

		Long getMemberId();

		Long getSeatId();
	}

	List<Ticket> findAllByReservationIdAndMemberId(Long reservationId, Long memberId);

	Optional<Ticket> findByReservationIdAndMemberIdAndSeatId(Long reservationId, Long memberId, Long seatId);

	boolean existsByReservationIdAndMemberId(Long reservationId, Long memberId);

	List<Ticket> findByMemberId(Long memberId);

	List<Ticket> findByReservationId(Long reservationId);

	@Query("""
		select
			rs.reservationId as reservationId,
			r.memberId as memberId,
			ss.seatId as seatId
		from ReservationSeat rs
		join Reservation r on r.id = rs.reservationId
		join ScheduleSeat ss on ss.id = rs.scheduleSeatId
		left join Ticket t
			on t.reservationId = rs.reservationId
			and t.memberId = r.memberId
			and t.seatId = ss.seatId
		where r.memberId = :memberId
		  and r.status = com.back.b2st.domain.reservation.entity.ReservationStatus.COMPLETED
		  and t.id is null
		""")
	List<MissingTicketKey> findMissingTicketsForMember(@Param("memberId") Long memberId);

	void deleteAllByReservationIdIn(List<Long> reservationIds);
}
