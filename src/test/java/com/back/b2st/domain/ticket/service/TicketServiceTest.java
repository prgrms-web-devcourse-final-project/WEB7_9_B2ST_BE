package com.back.b2st.domain.ticket.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.entity.TicketStatus;
import com.back.b2st.domain.ticket.repository.TicketRepository;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class TicketServiceTest {

	@Autowired
	private TicketRepository ticketRepository;
	@Autowired
	private TicketService ticketService;

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

	@Test
	void 티켓생성_중복요청() {
		// TODO 개발 필요
	}

	@Test
	void 티켓_취소변경() {
		// when
		Long rId = 2L;
		Long mId = 2L;
		Long sId = 4L;

		Ticket ticket = ticketService.createTicket(rId, mId, sId);

		// when
		ticket.cancel();

		// then
		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELED);

		// DB 검증
		Ticket findTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(findTicket.getStatus()).isEqualTo(TicketStatus.CANCELED);
	}

	@Test
	void 티켓_사용변경() {
		// when
		Long rId = 2L;
		Long mId = 2L;
		Long sId = 4L;

		Ticket ticket = ticketService.createTicket(rId, mId, sId);

		// when
		ticket.use();

		// then
		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);

		// DB 검증
		Ticket findTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(findTicket.getStatus()).isEqualTo(TicketStatus.USED);
	}

	@Test
	void 티켓_교환변경() {
		// when
		Long rId = 2L;
		Long mId = 2L;
		Long sId = 4L;

		Ticket ticket = ticketService.createTicket(rId, mId, sId);

		// when
		ticket.exchange();

		// then
		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.EXCHANGED);

		// DB 검증
		Ticket findTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(findTicket.getStatus()).isEqualTo(TicketStatus.EXCHANGED);
	}

	@Test
	void 티켓_양도변경() {
		// when
		Long rId = 2L;
		Long mId = 2L;
		Long sId = 4L;

		Ticket ticket = ticketService.createTicket(rId, mId, sId);

		// when
		ticket.transfer();

		// then
		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.TRANSFERRED);

		// DB 검증
		Ticket findTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(findTicket.getStatus()).isEqualTo(TicketStatus.TRANSFERRED);
	}

}