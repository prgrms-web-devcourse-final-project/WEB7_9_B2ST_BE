package com.back.b2st.domain.ticket.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.ticket.dto.response.TicketRes;
import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.error.TicketErrorCode;
import com.back.b2st.domain.ticket.repository.TicketRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

	private final TicketRepository ticketRepository;
	private final SeatRepository seatRepository;
	private final ReservationRepository reservationRepository;

	public Ticket createTicket(Long reservationId, Long memberId, Long seatId) {
		// TODO QR코드 로직, 중복 생성 방지
		Ticket ticket = Ticket.builder()
			.reservationId(reservationId)
			.memberId(memberId)
			.seatId(seatId)
			.build();
		return ticketRepository.save(ticket);
	}

	private Ticket getTicket(Long reservationId, Long memberId, Long seatId) {
		return ticketRepository.findByReservationIdAndMemberIdAndSeatId(reservationId, memberId, seatId)
			.orElseThrow(() -> new BusinessException(TicketErrorCode.TICKET_NOT_FOUND));
	}

	public Ticket getTicketById(Long ticketId) {
		return ticketRepository.findById(ticketId)
			.orElseThrow(() -> new BusinessException(TicketErrorCode.TICKET_NOT_FOUND));
	}

	@Transactional
	public Ticket cancelTicket(Long reservationId, Long memberId, Long seatId) {
		Ticket ticket = getTicket(reservationId, memberId, seatId);

		switch (ticket.getStatus()) {
			case ISSUED -> ticket.cancel();
			case CANCELED -> throw new BusinessException(TicketErrorCode.ALREADY_CANCEL_TICKET);
			default -> throw new BusinessException(TicketErrorCode.TICKET_NOT_CANCELABLE);
		}

		return ticket;
	}

	@Transactional
	public Ticket useTicket(Long reservationId, Long memberId, Long seatId) {
		Ticket ticket = getTicket(reservationId, memberId, seatId);
		ticket.use();

		return ticket;
	}

	@Transactional
	public Ticket exchangeTicket(Long reservationId, Long memberId, Long seatId) {
		Ticket ticket = getTicket(reservationId, memberId, seatId);
		ticket.exchange();

		return ticket;
	}

	@Transactional
	public Ticket transferTicket(Long reservationId, Long memberId, Long seatId) {
		Ticket ticket = getTicket(reservationId, memberId, seatId);
		ticket.transfer();

		return ticket;
	}

	@Transactional
	public Ticket expireTicket(Long reservationId, Long memberId, Long seatId) {
		Ticket ticket = getTicket(reservationId, memberId, seatId);
		ticket.expire();

		return ticket;
	}

	public List<TicketRes> getMyTickets(Long memberId) {
		List<Ticket> tickets = ticketRepository.findByMemberId(memberId);

		return tickets.stream()
			.map(ticket -> {
				Seat seat = seatRepository.findById(ticket.getSeatId())
					.orElseThrow(() -> new BusinessException(TicketErrorCode.TICKET_NOT_FOUND));
				Reservation reservation = reservationRepository.findById(ticket.getReservationId())
					.orElseThrow(() -> new BusinessException(TicketErrorCode.TICKET_NOT_FOUND));

				return TicketRes.builder()
					.ticketId(ticket.getId())
					.reservationId(ticket.getReservationId())
					.seatId(ticket.getSeatId())
					.status(ticket.getStatus())
					.sectionName(seat.getSectionName())
					.rowLabel(seat.getRowLabel())
					.seatNumber(seat.getSeatNumber())
					.performanceId(reservation.getPerformanceId())
					.build();
			})
			.collect(Collectors.toList());
	}
}
