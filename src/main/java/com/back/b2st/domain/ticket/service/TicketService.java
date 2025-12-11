package com.back.b2st.domain.ticket.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.error.TicketErrorCode;
import com.back.b2st.domain.ticket.repository.TicketRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketService {

	private final TicketRepository ticketRepository;

	public Ticket createTicket(Long reservationId, Long memberId, Long seatId) {
		// TODO QR코드 로직, 중복 생성 방지
		Ticket ticket = Ticket.builder()
			.reservationId(reservationId)
			.memberId(memberId)
			.seatId(seatId)
			.build();
		return ticketRepository.save(ticket);
	}

	private Ticket checkTicket(Long reservationId, Long memberId, Long seatId) {
		return ticketRepository.findByReservationIdAndMemberIdAndSeatId(reservationId, memberId, seatId)
			.orElseThrow(() -> new BusinessException(TicketErrorCode.TICKET_NOT_FOUND));
	}

	@Transactional
	public Ticket cancelTicket(Long reservationId, Long memberId, Long seatId) {
		Ticket ticket = checkTicket(reservationId, memberId, seatId);
		ticket.cancel();

		return ticket;
	}

	@Transactional
	public Ticket useTicket(Long reservationId, Long memberId, Long seatId) {
		Ticket ticket = checkTicket(reservationId, memberId, seatId);
		ticket.use();

		return ticket;
	}

	@Transactional
	public Ticket exchangeTicket(Long reservationId, Long memberId, Long seatId) {
		Ticket ticket = checkTicket(reservationId, memberId, seatId);
		ticket.exchange();

		return ticket;
	}

	@Transactional
	public Ticket transferTicket(Long reservationId, Long memberId, Long seatId) {
		Ticket ticket = checkTicket(reservationId, memberId, seatId);
		ticket.transfer();

		return ticket;
	}

	@Transactional
	public Ticket expireTicket(Long reservationId, Long memberId, Long seatId) {
		Ticket ticket = checkTicket(reservationId, memberId, seatId);
		ticket.expire();

		return ticket;
	}
}