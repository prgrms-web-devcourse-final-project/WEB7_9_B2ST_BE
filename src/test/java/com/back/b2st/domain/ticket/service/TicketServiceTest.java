package com.back.b2st.domain.ticket.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
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

	private Ticket ticket;

	@BeforeEach
	void setUp() {
		Long rId = 2L;
		Long mId = 2L;
		Long sId = 4L;

		ticket = ticketService.createTicket(rId, mId, sId);
	}

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
		Long rId = ticket.getReservationId();
		Long mId = ticket.getMemberId();
		Long sId = ticket.getSeatId();

		// when
		ticketService.cancelTicket(rId, mId, sId);

		// then
		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELED);

		// DB 검증
		Ticket findTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(findTicket.getStatus()).isEqualTo(TicketStatus.CANCELED);
	}

	@Test
	void 티켓_사용변경() {
		// when
		Long rId = ticket.getReservationId();
		Long mId = ticket.getMemberId();
		Long sId = ticket.getSeatId();

		// when
		ticketService.useTicket(rId, mId, sId);

		// then
		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);

		// DB 검증
		Ticket findTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(findTicket.getStatus()).isEqualTo(TicketStatus.USED);
	}

	@Test
	void 티켓_교환변경() {
		// when
		Long rId = ticket.getReservationId();
		Long mId = ticket.getMemberId();
		Long sId = ticket.getSeatId();

		// when
		ticketService.exchangeTicket(rId, mId, sId);

		// then
		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.EXCHANGED);

		// DB 검증
		Ticket findTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(findTicket.getStatus()).isEqualTo(TicketStatus.EXCHANGED);
	}

	@Test
	void 티켓_양도변경() {
		// when
		Long rId = ticket.getReservationId();
		Long mId = ticket.getMemberId();
		Long sId = ticket.getSeatId();

		// when
		ticketService.transferTicket(rId, mId, sId);

		// then
		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.TRANSFERRED);

		// DB 검증
		Ticket findTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(findTicket.getStatus()).isEqualTo(TicketStatus.TRANSFERRED);
	}

	@Test
	void 티켓_만료변경() {
		// when
		Long rId = ticket.getReservationId();
		Long mId = ticket.getMemberId();
		Long sId = ticket.getSeatId();

		// when
		ticketService.expireTicket(rId, mId, sId);

		// then
		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.EXPIRED);

		// DB 검증
		Ticket findTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(findTicket.getStatus()).isEqualTo(TicketStatus.EXPIRED);
	}

}