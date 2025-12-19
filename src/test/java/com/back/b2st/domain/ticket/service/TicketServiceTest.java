package com.back.b2st.domain.ticket.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.ticket.dto.response.TicketRes;
import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.entity.TicketStatus;
import com.back.b2st.domain.ticket.error.TicketErrorCode;
import com.back.b2st.domain.ticket.repository.TicketRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.global.error.exception.BusinessException;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class TicketServiceTest {

	@Autowired
	private TicketRepository ticketRepository;
	@Autowired
	private TicketService ticketService;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private SectionRepository sectionRepository;
	@Autowired
	private SeatRepository seatRepository;
	@Autowired
	private ReservationRepository reservationRepository;
	@Autowired
	private EntityManager em;

	private Ticket ticket;
	private Long rId, mId, sId;

	@BeforeEach
	void setUp() {
		// Create test member
		Member testMember = Member.builder()
			.email("ticket@test.com")
			.password("password")
			.name("Test User")
			.birth(LocalDate.of(1990, 1, 1))
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build();
		Member savedMember = memberRepository.save(testMember);

		// Create test section
		Section section = Section.builder()
			.venueId(1L)
			.sectionName("A")
			.build();
		Section savedSection = sectionRepository.save(section);

		// Create test seat
		Seat seat = Seat.builder()
			.venueId(1L)
			.sectionId(savedSection.getId())
			.sectionName(savedSection.getSectionName())
			.rowLabel("1")
			.seatNumber(1)
			.build();
		Seat savedSeat = seatRepository.save(seat);

		// Create test reservation
		Reservation reservation = Reservation.builder()
			.scheduleId(1L)
			.memberId(savedMember.getId())
			.seatId(savedSeat.getId())
			.build();
		Reservation savedReservation = reservationRepository.save(reservation);

		em.flush();
		em.clear();

		rId = savedReservation.getId();
		mId = savedMember.getId();
		sId = savedSeat.getId();

		ticket = ticketService.createTicket(rId, mId, sId);
	}

	@Test
	@DisplayName("티켓생성_성공")
	void createTicket_success() {
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
	@DisplayName("티켓취소_성공")
	void cancelTicket_success() {
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
	@DisplayName("티켓취소_실패_취소티켓")
	void cancelTicket_fail_already() {
		// given
		ticketService.cancelTicket(rId, mId, sId);

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> ticketService.cancelTicket(rId, mId, sId));

		// then
		assertThat(e.getErrorCode()).isEqualTo(TicketErrorCode.ALREADY_CANCEL_TICKET);
	}

	@Test
	@DisplayName("티켓취소_실패_없는티켓")
	void cancelTicket_fail_noTicket() {
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
	@DisplayName("티켓취소_실패_사용티켓")
	void cancelTicket_fail_used() {
		// given
		ticketService.useTicket(rId, mId, sId);

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> ticketService.cancelTicket(rId, mId, sId));

		// then
		assertThat(e.getErrorCode()).isEqualTo(TicketErrorCode.TICKET_NOT_CANCELABLE);
	}

	@Test
	@DisplayName("티켓취소_실패_교환티켓")
	void cancelTicket_fail_exchanged() {
		// given
		ticketService.exchangeTicket(rId, mId, sId);

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> ticketService.cancelTicket(rId, mId, sId));

		// then
		assertThat(e.getErrorCode()).isEqualTo(TicketErrorCode.TICKET_NOT_CANCELABLE);
	}

	@Test
	@DisplayName("티켓취소_실패_양도티켓")
	void cancelTicket_fail_transferred() {
		// given
		ticketService.transferTicket(rId, mId, sId);

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> ticketService.cancelTicket(rId, mId, sId));

		// then
		assertThat(e.getErrorCode()).isEqualTo(TicketErrorCode.TICKET_NOT_CANCELABLE);
	}

	@Test
	@DisplayName("티켓취소_실패_만료티켓")
	void cancelTicket_fail_expired() {
		// given
		ticketService.expireTicket(rId, mId, sId);

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> ticketService.cancelTicket(rId, mId, sId));

		// then
		assertThat(e.getErrorCode()).isEqualTo(TicketErrorCode.TICKET_NOT_CANCELABLE);
	}

	@Test
	@DisplayName("티켓사용_성공")
	void changeTicket_success_used() {
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
	@DisplayName("티켓교환_성공")
	void changeTicket_success_exchanged() {
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
	@DisplayName("티켓양도_성공")
	void changeTicket_success_transferred() {
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
	@DisplayName("티켓만료_성공")
	void changeTicket_success_expired() {
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

	@Test
	@DisplayName("내티켓조회_성공")
	void getMyTicket_success() {
		// given - using the member created in setUp
		Long memberId = mId;

		// when
		List<TicketRes> tickets = ticketService.getMyTickets(memberId);

		// then
		assertThat(tickets).isNotNull();
		assertThat(tickets.size()).isGreaterThanOrEqualTo(1);

		// 티켓 정보 검증
		TicketRes firstTicket = tickets.get(0);
		assertThat(firstTicket.getTicketId()).isNotNull();
		assertThat(firstTicket.getReservationId()).isNotNull();
		assertThat(firstTicket.getSeatId()).isNotNull();
		assertThat(firstTicket.getStatus()).isNotNull();
		assertThat(firstTicket.getSectionName()).isNotNull();
		assertThat(firstTicket.getRowLabel()).isNotNull();
		assertThat(firstTicket.getSeatNumber()).isNotNull();
		assertThat(firstTicket.getPerformanceId()).isNotNull();
	}

	@Test
	@DisplayName("내티켓조회_성공_빈목록")
	void getMyTicket_success_empty() {
		// given
		Long memberId = 9999L; // 존재하지 않는 회원 ID

		// when
		List<TicketRes> tickets = ticketService.getMyTickets(memberId);

		// then
		assertThat(tickets).isNotNull();
		assertThat(tickets).isEmpty();
	}
}