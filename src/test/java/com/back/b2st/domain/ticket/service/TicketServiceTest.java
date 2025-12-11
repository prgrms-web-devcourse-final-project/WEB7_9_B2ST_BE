package com.back.b2st.domain.ticket.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.entity.TicketStatus;
import com.back.b2st.domain.ticket.error.TicketErrorCode;
import com.back.b2st.domain.ticket.repository.TicketRepository;
import com.back.b2st.global.error.exception.BusinessException;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class TicketServiceTest {

	@Autowired
	private TicketRepository ticketRepository;
	@Autowired
	private TicketService ticketService;

	private Ticket ticket;
	private Long rId, mId, sId;

	@BeforeEach
	void setUp() {
		rId = 2L;
		mId = 2L;
		sId = 4L;

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
	void 티켓취소_이미취소된티켓() {
		// given
		ticketService.cancelTicket(rId, mId, sId);

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> ticketService.cancelTicket(rId, mId, sId));

		// then
		assertThat(e.getErrorCode()).isEqualTo(TicketErrorCode.ALREADY_CANCEL_TICKET);
	}

	@Test
	void 티켓취소_존재하지않는티켓() {
		// given
		rId = 99L;
		mId = 99L;
		sId = 99L;

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> ticketService.cancelTicket(rId, mId, sId));

		// then
		assertThat(e.getErrorCode()).isEqualTo(TicketErrorCode.TICKET_NOT_FOUND);
	}

	@Test
	void 티켓취소_사용티켓취소불가() {
		// given
		ticketService.useTicket(rId, mId, sId);

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> ticketService.cancelTicket(rId, mId, sId));

		// then
		assertThat(e.getErrorCode()).isEqualTo(TicketErrorCode.TICKET_NOT_CANCELABLE);
	}

	@Test
	void 티켓취소_교환티켓취소불가() {
		// given
		ticketService.exchangeTicket(rId, mId, sId);

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> ticketService.cancelTicket(rId, mId, sId));

		// then
		assertThat(e.getErrorCode()).isEqualTo(TicketErrorCode.TICKET_NOT_CANCELABLE);
	}

	@Test
	void 티켓취소_양도티켓취소불가() {
		// given
		ticketService.transferTicket(rId, mId, sId);
		
		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> ticketService.cancelTicket(rId, mId, sId));

		// then
		assertThat(e.getErrorCode()).isEqualTo(TicketErrorCode.TICKET_NOT_CANCELABLE);
	}

	@Test
	void 티켓취소_만료티켓취소불가() {
		// given
		ticketService.expireTicket(rId, mId, sId);

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> ticketService.cancelTicket(rId, mId, sId));

		// then
		assertThat(e.getErrorCode()).isEqualTo(TicketErrorCode.TICKET_NOT_CANCELABLE);
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