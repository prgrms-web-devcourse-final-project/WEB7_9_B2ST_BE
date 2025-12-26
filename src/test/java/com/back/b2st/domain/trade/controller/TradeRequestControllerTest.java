package com.back.b2st.domain.trade.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.entity.PerformanceStatus;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.repository.TicketRepository;
import com.back.b2st.domain.trade.repository.TradeRepository;
import com.back.b2st.domain.trade.repository.TradeRequestRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.domain.venue.venue.repository.VenueRepository;
import com.back.b2st.security.UserPrincipal;

import jakarta.persistence.EntityManager;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TradeRequestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private VenueRepository venueRepository;

	@Autowired
	private PerformanceRepository performanceRepository;

	@Autowired
	private PerformanceScheduleRepository performanceScheduleRepository;

	@Autowired
	private SectionRepository sectionRepository;

	@Autowired
	private SeatRepository seatRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private TradeRepository tradeRepository;

	@Autowired
	private TradeRequestRepository tradeRequestRepository;

	@Autowired
	private EntityManager em;

	private Authentication tradeOwnerAuth;
	private Authentication requesterAuth;

	private Long tradeOwnerId;
	private Long requesterId;
	private Long scheduleId;

	private Long ticket100Id;  // tradeOwner 소유
	private Long ticket101Id;  // requester 소유
	private Long ticket110Id;  // tradeOwner 소유 - 자신의 게시글 테스트용
	private Long ticket111Id;  // tradeOwner 소유 - 자신의 게시글 테스트용
	private Long ticket120Id;  // tradeOwner 소유 - 중복 신청 테스트용
	private Long ticket121Id;  // requester 소유 - 중복 신청 테스트용
	private Long ticket130Id;  // tradeOwner 소유 - 조회 성공 테스트용
	private Long ticket131Id;  // requester 소유 - 조회 성공 테스트용
	private Long ticket140Id;  // tradeOwner 소유 - Trade별 조회 테스트용
	private Long ticket141Id;  // requester 소유 - Trade별 조회 테스트용
	private Long ticket150Id;  // tradeOwner 소유 - 신청자별 조회 테스트용
	private Long ticket151Id;  // requester 소유 - 신청자별 조회 테스트용
	private Long ticket160Id;  // tradeOwner 소유 - accept 테스트용
	private Long ticket161Id;  // requester 소유 - accept 테스트용
	private Long ticket170Id;  // tradeOwner 소유 - unauthorized 테스트용
	private Long ticket171Id;  // requester 소유 - unauthorized 테스트용
	private Long ticket180Id;  // tradeOwner 소유 - 거절 성공 테스트용
	private Long ticket181Id;  // requester 소유 - 거절 성공 테스트용
	private Long ticket190Id;  // tradeOwner 소유 - 거절 unauthorized 테스트용
	private Long ticket191Id;  // requester 소유 - 거절 unauthorized 테스트용
	private Long ticket200Id;  // tradeOwner 소유 - 필드 누락 테스트용

	@BeforeEach
	void setup() {
		// Clean up existing data to ensure test isolation
		tradeRequestRepository.deleteAll();
		tradeRepository.deleteAll();
		ticketRepository.deleteAll();
		reservationRepository.deleteAll();
		seatRepository.deleteAll();
		sectionRepository.deleteAll();
		performanceScheduleRepository.deleteAll();
		performanceRepository.deleteAll();
		venueRepository.deleteAll();
		memberRepository.deleteAll();

		// Create test members
		Member tradeOwnerMember = Member.builder()
			.email("owner@test.com")
			.password("password")
			.name("Trade Owner")
			.birth(LocalDate.of(1990, 1, 1))
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build();
		Member savedTradeOwner = memberRepository.save(tradeOwnerMember);
		tradeOwnerId = savedTradeOwner.getId();

		Member requesterMember = Member.builder()
			.email("requester@test.com")
			.password("password")
			.name("Requester")
			.birth(LocalDate.of(1990, 1, 1))
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build();
		Member savedRequester = memberRepository.save(requesterMember);
		requesterId = savedRequester.getId();

		// Create test venue
		Venue venue = Venue.builder()
			.name("Test Venue")
			.build();
		Venue savedVenue = venueRepository.save(venue);

		// Create test performance
		Performance performance = Performance.builder()
			.venue(savedVenue)
			.title("Test Performance")
			.category("Test")
			.startDate(java.time.LocalDateTime.now())
			.endDate(java.time.LocalDateTime.now().plusDays(1))
			.status(PerformanceStatus.ON_SALE)
			.build();
		Performance savedPerformance = performanceRepository.save(performance);

		// Create test performance schedule
		PerformanceSchedule schedule = PerformanceSchedule.builder()
			.performance(savedPerformance)
			.roundNo(1)
			.startAt(java.time.LocalDateTime.now())
			.bookingType(BookingType.FIRST_COME)
			.bookingOpenAt(java.time.LocalDateTime.now().minusDays(1))
			.bookingCloseAt(java.time.LocalDateTime.now().plusDays(1))
			.build();
		PerformanceSchedule savedSchedule = performanceScheduleRepository.save(schedule);
		scheduleId = savedSchedule.getPerformanceScheduleId();

		// Create test section
		Section section = Section.builder()
			.venueId(savedVenue.getVenueId())
			.sectionName("A")
			.build();
		Section savedSection = sectionRepository.save(section);

		// Create seats and reservations for tickets
		for (int i = 1; i <= 200; i++) {
			Seat seat = Seat.builder()
				.venueId(savedVenue.getVenueId())
				.sectionId(savedSection.getId())
				.sectionName(savedSection.getSectionName())
				.rowLabel("1")
				.seatNumber(i)
				.build();
			seatRepository.save(seat);
		}

		UserPrincipal tradeOwner = UserPrincipal.builder()
			.id(tradeOwnerId)
			.email("owner@test.com")
			.role("ROLE_MEMBER")
			.build();

		UserPrincipal requester = UserPrincipal.builder()
			.id(requesterId)
			.email("requester@test.com")
			.role("ROLE_MEMBER")
			.build();

		tradeOwnerAuth = new UsernamePasswordAuthenticationToken(tradeOwner, null, null);
		requesterAuth = new UsernamePasswordAuthenticationToken(requester, null, null);

		// 테스트에 필요한 티켓 생성
		createTestTickets();
	}

	private void createTestTickets() {
		ticket100Id = createTicket(1, tradeOwnerId);
		ticket101Id = createTicket(2, requesterId);
		ticket110Id = createTicket(10, tradeOwnerId);
		ticket111Id = createTicket(11, tradeOwnerId);
		ticket120Id = createTicket(12, tradeOwnerId);
		ticket121Id = createTicket(13, requesterId);
		ticket130Id = createTicket(14, tradeOwnerId);
		ticket131Id = createTicket(15, requesterId);
		ticket140Id = createTicket(16, tradeOwnerId);
		ticket141Id = createTicket(17, requesterId);
		ticket150Id = createTicket(18, tradeOwnerId);
		ticket151Id = createTicket(19, requesterId);
		ticket160Id = createTicket(160, tradeOwnerId);
		ticket161Id = createTicket(161, requesterId);
		ticket170Id = createTicket(170, tradeOwnerId);
		ticket171Id = createTicket(171, requesterId);
		ticket180Id = createTicket(180, tradeOwnerId);
		ticket181Id = createTicket(181, requesterId);
		ticket190Id = createTicket(190, tradeOwnerId);
		ticket191Id = createTicket(191, requesterId);
		ticket200Id = createTicket(200, tradeOwnerId);
	}

	private Long createTicket(int seatNumber, Long memberId) {
		Seat seat = seatRepository.findAll().stream()
			.filter(s -> s.getSeatNumber() == seatNumber)
			.findFirst()
			.orElseThrow();
		Reservation reservation = reservationRepository.save(Reservation.builder()
			.scheduleId(scheduleId)
			.memberId(memberId)
			.seatId(seat.getId())
			.expiresAt(LocalDateTime.now().plusMinutes(5))
			.build());
		Ticket ticket = ticketRepository.save(Ticket.builder()
			.reservationId(reservation.getId())
			.memberId(memberId)
			.seatId(seat.getId())
			.build());
		return ticket.getId();
	}

	@Test
	@DisplayName("교환 신청 생성 성공")
	void createTradeRequest_success() throws Exception {
		// given - 먼저 Trade 생성
		String createTradeRequest = """
			{
				"ticketIds": [%d],
				"type": "EXCHANGE",
				"price": null
			}
			""".formatted(ticket100Id);

		String tradeResponse = mockMvc.perform(post("/api/trades")
				.with(authentication(tradeOwnerAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createTradeRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(tradeResponse).get("data").get(0).get("tradeId").asLong();

		// when & then - TradeRequest 생성
		String createRequestBody = """
			{
				"requesterTicketId": %d
			}
			""".formatted(ticket101Id);

		mockMvc.perform(post("/api/trades/" + tradeId + "/requests")
				.with(authentication(requesterAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequestBody))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.tradeId").value(tradeId))
			.andExpect(jsonPath("$.data.requesterId").value(requesterId))
			.andExpect(jsonPath("$.data.requesterTicketId").value(ticket101Id))
			.andExpect(jsonPath("$.data.status").value("PENDING"));
	}

	@Test
	@DisplayName("교환 신청 생성 실패 - 자신의 게시글")
	void createTradeRequest_fail_ownTrade() throws Exception {
		// given - Trade 생성
		String createTradeRequest = """
			{
				"ticketIds": [%d],
				"type": "EXCHANGE",
				"price": null
			}
			""".formatted(ticket110Id);

		String tradeResponse = mockMvc.perform(post("/api/trades")
				.with(authentication(tradeOwnerAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createTradeRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(tradeResponse).get("data").get(0).get("tradeId").asLong();

		// when & then - 자신의 게시글에 신청
		String createRequestBody = """
			{
				"requesterTicketId": %d
			}
			""".formatted(ticket111Id);

		mockMvc.perform(post("/api/trades/" + tradeId + "/requests")
				.with(authentication(tradeOwnerAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequestBody))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(400));
	}

	@Test
	@DisplayName("교환 신청 생성 실패 - 중복 신청")
	void createTradeRequest_fail_duplicate() throws Exception {
		// given - Trade 생성 및 첫 신청
		String createTradeRequest = """
			{
				"ticketIds": [%d],
				"type": "EXCHANGE",
				"price": null
			}
			""".formatted(ticket120Id);

		String tradeResponse = mockMvc.perform(post("/api/trades")
				.with(authentication(tradeOwnerAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createTradeRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(tradeResponse).get("data").get(0).get("tradeId").asLong();

		String createRequestBody = """
			{
				"requesterTicketId": %d
			}
			""".formatted(ticket121Id);

		mockMvc.perform(post("/api/trades/" + tradeId + "/requests")
			.with(authentication(requesterAuth))
			.contentType(MediaType.APPLICATION_JSON)
			.content(createRequestBody));

		// when & then - 중복 신청
		mockMvc.perform(post("/api/trades/" + tradeId + "/requests")
				.with(authentication(requesterAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequestBody))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(400));
	}

	@Test
	@DisplayName("교환 신청 조회 성공")
	void getTradeRequest_success() throws Exception {
		// given - Trade 생성 및 신청
		String createTradeRequest = """
			{
				"ticketIds": [%d],
				"type": "EXCHANGE",
				"price": null
			}
			""".formatted(ticket130Id);

		String tradeResponse = mockMvc.perform(post("/api/trades")
				.with(authentication(tradeOwnerAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createTradeRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(tradeResponse).get("data").get(0).get("tradeId").asLong();

		String createRequestBody = """
			{
				"requesterTicketId": %d
			}
			""".formatted(ticket131Id);

		String requestResponse = mockMvc.perform(post("/api/trades/" + tradeId + "/requests")
				.with(authentication(requesterAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequestBody))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeRequestId = objectMapper.readTree(requestResponse).get("data").get("tradeRequestId").asLong();

		// when & then - 조회
		mockMvc.perform(get("/api/trade-requests/" + tradeRequestId)
				.with(authentication(requesterAuth)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.tradeRequestId").value(tradeRequestId))
			.andExpect(jsonPath("$.data.tradeId").value(tradeId))
			.andExpect(jsonPath("$.data.status").value("PENDING"));
	}

	@Test
	@DisplayName("Trade별 신청 목록 조회 성공")
	void getTradeRequestsByTrade_success() throws Exception {
		// given - Trade 생성 및 여러 신청
		String createTradeRequest = """
			{
				"ticketIds": [%d],
				"type": "EXCHANGE",
				"price": null
			}
			""".formatted(ticket140Id);

		String tradeResponse = mockMvc.perform(post("/api/trades")
				.with(authentication(tradeOwnerAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createTradeRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(tradeResponse).get("data").get(0).get("tradeId").asLong();

		// 첫 번째 신청
		mockMvc.perform(post("/api/trades/" + tradeId + "/requests")
			.with(authentication(requesterAuth))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"requesterTicketId\": " + ticket141Id + "}"));

		// when & then - Trade별 조회
		mockMvc.perform(get("/api/trade-requests?tradeId=" + tradeId)
				.with(authentication(tradeOwnerAuth)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].tradeId").value(tradeId));
	}

	@Test
	@DisplayName("신청자별 신청 목록 조회 성공")
	void getTradeRequestsByRequester_success() throws Exception {
		// given - Trade 생성 및 신청
		String createTradeRequest = """
			{
				"ticketIds": [%d],
				"type": "EXCHANGE",
				"price": null
			}
			""".formatted(ticket150Id);

		String tradeResponse = mockMvc.perform(post("/api/trades")
				.with(authentication(tradeOwnerAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createTradeRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(tradeResponse).get("data").get(0).get("tradeId").asLong();

		mockMvc.perform(post("/api/trades/" + tradeId + "/requests")
			.with(authentication(requesterAuth))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"requesterTicketId\": " + ticket151Id + "}"));

		// when & then - 신청자별 조회
		mockMvc.perform(get("/api/trade-requests?requesterId=" + requesterId)
				.with(authentication(requesterAuth)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].requesterId").value(requesterId));
	}

	@Test
	@DisplayName("교환 신청 수락 성공")
	void acceptTradeRequest_success() throws Exception {
		// given - Trade 생성 및 신청
		String createTradeRequest = """
			{
				"ticketIds": [%d],
				"type": "EXCHANGE",
				"price": null
			}
			""".formatted(ticket160Id);

		String tradeResponse = mockMvc.perform(post("/api/trades")
				.with(authentication(tradeOwnerAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createTradeRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(tradeResponse).get("data").get(0).get("tradeId").asLong();

		String requestResponse = mockMvc.perform(post("/api/trades/" + tradeId + "/requests")
				.with(authentication(requesterAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"requesterTicketId\": " + ticket161Id + "}"))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeRequestId = objectMapper.readTree(requestResponse).get("data").get("tradeRequestId").asLong();

		// when & then - 수락
		mockMvc.perform(patch("/api/trade-requests/" + tradeRequestId + "/accept")
				.with(authentication(tradeOwnerAuth)))
			.andDo(print())
			.andExpect(status().isOk());

		// 수락 후 상태 확인
		mockMvc.perform(get("/api/trade-requests/" + tradeRequestId)
				.with(authentication(tradeOwnerAuth)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("ACCEPTED"));
	}

	@Test
	@DisplayName("교환 신청 수락 실패 - 권한 없음")
	void acceptTradeRequest_fail_unauthorized() throws Exception {
		// given - Trade 생성 및 신청
		String createTradeRequest = """
			{
				"ticketIds": [%d],
				"type": "EXCHANGE",
				"price": null
			}
			""".formatted(ticket170Id);

		String tradeResponse = mockMvc.perform(post("/api/trades")
				.with(authentication(tradeOwnerAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createTradeRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(tradeResponse).get("data").get(0).get("tradeId").asLong();

		String requestResponse = mockMvc.perform(post("/api/trades/" + tradeId + "/requests")
				.with(authentication(requesterAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"requesterTicketId\": " + ticket171Id + "}"))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeRequestId = objectMapper.readTree(requestResponse).get("data").get("tradeRequestId").asLong();

		// when & then - 다른 사용자가 수락 시도
		mockMvc.perform(patch("/api/trade-requests/" + tradeRequestId + "/accept")
				.with(authentication(requesterAuth)))
			.andDo(print())
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("교환 신청 거절 성공")
	void rejectTradeRequest_success() throws Exception {
		// given - Trade 생성 및 신청
		String createTradeRequest = """
			{
				"ticketIds": [%d],
				"type": "EXCHANGE",
				"price": null
			}
			""".formatted(ticket180Id);

		String tradeResponse = mockMvc.perform(post("/api/trades")
				.with(authentication(tradeOwnerAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createTradeRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(tradeResponse).get("data").get(0).get("tradeId").asLong();

		String requestResponse = mockMvc.perform(post("/api/trades/" + tradeId + "/requests")
				.with(authentication(requesterAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"requesterTicketId\": " + ticket181Id + "}"))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeRequestId = objectMapper.readTree(requestResponse).get("data").get("tradeRequestId").asLong();

		// when & then - 거절
		mockMvc.perform(patch("/api/trade-requests/" + tradeRequestId + "/reject")
				.with(authentication(tradeOwnerAuth)))
			.andDo(print())
			.andExpect(status().isOk());

		// 거절 후 상태 확인
		mockMvc.perform(get("/api/trade-requests/" + tradeRequestId)
				.with(authentication(tradeOwnerAuth)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("REJECTED"));
	}

	@Test
	@DisplayName("교환 신청 거절 실패 - 권한 없음")
	void rejectTradeRequest_fail_unauthorized() throws Exception {
		// given - Trade 생성 및 신청
		String createTradeRequest = """
			{
				"ticketIds": [%d],
				"type": "EXCHANGE",
				"price": null
			}
			""".formatted(ticket190Id);

		String tradeResponse = mockMvc.perform(post("/api/trades")
				.with(authentication(tradeOwnerAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createTradeRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(tradeResponse).get("data").get(0).get("tradeId").asLong();

		String requestResponse = mockMvc.perform(post("/api/trades/" + tradeId + "/requests")
				.with(authentication(requesterAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"requesterTicketId\": " + ticket191Id + "}"))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeRequestId = objectMapper.readTree(requestResponse).get("data").get("tradeRequestId").asLong();

		// when & then - 다른 사용자가 거절 시도
		mockMvc.perform(patch("/api/trade-requests/" + tradeRequestId + "/reject")
				.with(authentication(requesterAuth)))
			.andDo(print())
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("필수 필드 누락 시 검증 실패")
	void createTradeRequest_fail_missingFields() throws Exception {
		// given
		String createTradeRequest = """
			{
				"ticketIds": [%d],
				"type": "EXCHANGE",
				"price": null
			}
			""".formatted(ticket200Id);

		String tradeResponse = mockMvc.perform(post("/api/trades")
				.with(authentication(tradeOwnerAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createTradeRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(tradeResponse).get("data").get(0).get("tradeId").asLong();

		// when & then - 필드 누락
		String requestBody = "{}";

		mockMvc.perform(post("/api/trades/" + tradeId + "/requests")
				.with(authentication(requesterAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andDo(print())
			.andExpect(status().isBadRequest());
	}
}
