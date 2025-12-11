package com.back.b2st.domain.ticket.service;

import org.springframework.stereotype.Service;

import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.repository.TicketRepository;

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
}