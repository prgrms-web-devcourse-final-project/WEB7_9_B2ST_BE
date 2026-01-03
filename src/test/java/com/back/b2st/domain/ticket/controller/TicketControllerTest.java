package com.back.b2st.domain.ticket.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.repository.TicketRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.security.UserPrincipal;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Disabled("TODO: Controller 통합 테스트 인증 문제 해결 필요 - MockMvc 인증 설정 수정 예정")
class TicketControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private SectionRepository sectionRepository;

	@Autowired
	private SeatRepository seatRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private EntityManager em;

	private Authentication mockAuth;

	@BeforeEach
	void setup() {
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

		// Create a few test tickets
		for (int i = 1; i <= 5; i++) {
			Seat seat = Seat.builder()
				.venueId(1L)
				.sectionId(savedSection.getId())
				.sectionName(savedSection.getSectionName())
				.rowLabel("1")
				.seatNumber(i)
				.build();
			Seat savedSeat = seatRepository.save(seat);

			Reservation reservation = Reservation.builder()
				.scheduleId(1L)
				.memberId(savedMember.getId())
				.build();
			Reservation savedReservation = reservationRepository.save(reservation);

			Ticket ticket = Ticket.builder()
				.reservationId(savedReservation.getId())
				.memberId(savedMember.getId())
				.seatId(savedSeat.getId())
				.qrCode("QR-" + i)
				.build();
			ticketRepository.save(ticket);
		}

		em.flush();
		em.clear();

		// Mock user for testing (use the saved member's ID)
		UserPrincipal mockUser = UserPrincipal.builder()
			.id(savedMember.getId())
			.email("ticket@test.com")
			.role("ROLE_MEMBER")
			.build();

		mockAuth = new UsernamePasswordAuthenticationToken(mockUser, null, null);
	}

	@Test
	@DisplayName("내 티켓 목록 조회 성공")
	void getMyTickets_success() throws Exception {
		// when & then
		mockMvc.perform(get("/api/tickets/my")
				.with(authentication(mockAuth)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray());
	}

	@Test
	@DisplayName("내 티켓 목록 조회 - 인증 실패")
	void getMyTickets_fail_unauthorized() throws Exception {
		// when & then
		mockMvc.perform(get("/api/tickets/my"))
			.andDo(print())
			.andExpect(status().isUnauthorized());
	}
}
