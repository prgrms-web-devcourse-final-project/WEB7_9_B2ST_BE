package com.back.b2st.domain.ticket.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.repository.TicketRepository;

@SpringBootTest
@Transactional
class TicketServiceTest {

	@Autowired
	private TicketRepository ticketRepository;

	@Test
	void 티켓생성() {
		// given
		Ticket ticket = Ticket.builder()
			.reservationId(99L)
			.memberId(99L)
			.seatId(99L)
			.build();

		// when
		Ticket saveTicket = ticketRepository.save(ticket);

		// then
		assertThat(saveTicket.getId()).isNotNull();

		// DB 검증
		Ticket findTicket = ticketRepository.findById(saveTicket.getId()).orElseThrow();
		assertThat(findTicket).isEqualTo(saveTicket);
	}

}