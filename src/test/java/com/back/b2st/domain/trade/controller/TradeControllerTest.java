package com.back.b2st.domain.trade.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.auth.dto.request.LoginReq;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
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
import com.back.b2st.global.test.AbstractContainerBaseTest;

import jakarta.persistence.EntityManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TradeControllerTest extends AbstractContainerBaseTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

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
	private TradeRepository tradeRepository;

	@Autowired
	private TradeRequestRepository tradeRequestRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private EntityManager em;

	private String accessToken;
	private Long testMemberId;
	private List<Long> ticketIds = new ArrayList<>();

	@BeforeEach
	void setup() throws Exception {
		// Clean up existing data to ensure test isolation
		tradeRequestRepository.deleteAll();
		tradeRepository.deleteAll();
		ticketRepository.deleteAll();
		reservationRepository.deleteAll();
		seatRepository.deleteAll();
		sectionRepository.deleteAll();
		memberRepository.deleteAll();
		ticketIds.clear();

		String email = "trade@test.com";
		String password = "Password123!";

		// Create test member
		Member testMember = Member.builder()
			.email(email)
			.password(passwordEncoder.encode(password))
			.name("Test User")
			.birth(LocalDate.of(1990, 1, 1))
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isVerified(true)
			.build();
		Member savedMember = memberRepository.save(testMember);
		testMemberId = savedMember.getId();

		// Get real JWT token
		accessToken = getAccessToken(email, password);

		// Create test section
		Section section = Section.builder()
			.venueId(1L)
			.sectionName("A")
			.build();
		Section savedSection = sectionRepository.save(section);

		// Create 55 seats and tickets for the test member
		for (int i = 1; i <= 55; i++) {
			Seat seat = Seat.builder()
				.venueId(1L)
				.sectionId(savedSection.getId())
				.sectionName(savedSection.getSectionName())
				.rowLabel("1")
				.seatNumber(i)
				.build();
			Seat savedSeat = seatRepository.save(seat);

			Reservation reservation = Reservation.builder()
				.performanceId(1L)
				.memberId(testMemberId)
				.seatId(savedSeat.getId())
				.build();
			Reservation savedReservation = reservationRepository.save(reservation);

			Ticket ticket = Ticket.builder()
				.reservationId(savedReservation.getId())
				.memberId(testMemberId)
				.seatId(savedSeat.getId())
				.qrCode("QR-" + i)
				.build();
			Ticket savedTicket = ticketRepository.save(ticket);
			ticketIds.add(savedTicket.getId());
		}

		em.flush();
		em.clear();
	}

	@AfterEach
	void cleanup() {
		// Clean up after each test to ensure no state leakage
		try {
			tradeRequestRepository.deleteAll();
			tradeRepository.deleteAll();
			ticketRepository.deleteAll();
			reservationRepository.deleteAll();
			seatRepository.deleteAll();
			sectionRepository.deleteAll();
			memberRepository.deleteAll();
		} catch (Exception e) {
			// Ignore cleanup errors
		}
	}

	private String getAccessToken(String email, String password) throws Exception {
		LoginReq loginRequest = new LoginReq(email, password);

		String response = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andReturn().getResponse().getContentAsString();

		JsonNode jsonNode = objectMapper.readTree(response);
		if (!jsonNode.has("data") || !jsonNode.get("data").has("accessToken")) {
			throw new IllegalStateException("Login response missing accessToken: " + response);
		}
		return jsonNode.path("data").path("accessToken").asText();
	}

	@Test
	@Order(1)
	@DisplayName("교환 게시글 생성 성공")
	void createExchangeTrade_success() throws Exception {
		// given
		String requestBody = String.format("""
			{
				"ticketIds": [%d],
				"type": "EXCHANGE",
				"price": null
			}
			""", ticketIds.get(0));

		// when & then
		mockMvc.perform(post("/api/trades")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data[0].type").value("EXCHANGE"))
			.andExpect(jsonPath("$.data[0].totalCount").value(1))
			.andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
	}

	@Test
	@Order(2)
	@DisplayName("양도 게시글 생성 성공")
	void createTransferTrade_success() throws Exception {
		// given
		String requestBody = String.format("""
			{
				"ticketIds": [%d],
				"type": "TRANSFER",
				"price": 50000
			}
			""", ticketIds.get(1));

		// when & then
		mockMvc.perform(post("/api/trades")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data[0].type").value("TRANSFER"))
			.andExpect(jsonPath("$.data[0].price").value(50000))
			.andExpect(jsonPath("$.data[0].totalCount").value(1));
	}

	@Test
	@Order(3)
	@DisplayName("중복 티켓 등록 실패")
	void createTrade_fail_duplicateTicket() throws Exception {
		// given - 먼저 게시글 생성
		String firstRequest = String.format("""
			{
				"ticketIds": [%d],
				"type": "EXCHANGE",
				"price": null
			}
			""", ticketIds.get(2));

		mockMvc.perform(post("/api/trades")
			.header("Authorization", "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content(firstRequest));

		// when - 동일한 티켓으로 다시 생성 시도
		mockMvc.perform(post("/api/trades")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(firstRequest))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(400));
	}

	@Test
	@Order(4)
	@DisplayName("교환 - price 설정 시 실패")
	void createExchangeTrade_fail_withPrice() throws Exception {
		// given
		String requestBody = String.format("""
			{
				"ticketIds": [%d],
				"type": "EXCHANGE",
				"price": 10000
			}
			""", ticketIds.get(4));

		// when & then
		mockMvc.perform(post("/api/trades")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(400));
	}

	@Test
	@Order(5)
	@DisplayName("양도 - price 미설정 시 실패")
	void createTransferTrade_fail_noPrice() throws Exception {
		// given
		String requestBody = String.format("""
			{
				"ticketIds": [%d],
				"type": "TRANSFER",
				"price": null
			}
			""", ticketIds.get(5));

		// when & then
		mockMvc.perform(post("/api/trades")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(400));
	}

	@Test
	@Order(6)
	@DisplayName("필수 필드 누락 시 검증 실패")
	void createTrade_fail_missingFields() throws Exception {
		// given
		String requestBody = """
			{
				"type": "EXCHANGE"
			}
			""";

		// when & then
		mockMvc.perform(post("/api/trades")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andDo(print())
			.andExpect(status().isBadRequest());
	}

	@Test
	@Order(7)
	@DisplayName("Trade 상세 조회 성공")
	void getTrade_success() throws Exception {
		String createRequest = String.format("""
			{
				"ticketIds": [%d],
				"type": "EXCHANGE",
				"price": null
			}
			""", ticketIds.get(9));

		String response = mockMvc.perform(post("/api/trades")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(response).get("data").get(0).get("tradeId").asLong();

		mockMvc.perform(get("/api/trades/" + tradeId)
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.tradeId").value(tradeId))
			.andExpect(jsonPath("$.data.type").value("EXCHANGE"))
			.andExpect(jsonPath("$.data.status").value("ACTIVE"))
			.andExpect(jsonPath("$.data.memberId").exists())
			.andExpect(jsonPath("$.data.section").exists());
	}

	@Test
	@Order(8)
	@DisplayName("Trade 상세 조회 실패 - 존재하지 않는 ID")
	void getTrade_fail_notFound() throws Exception {
		mockMvc.perform(get("/api/trades/99999")
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isNotFound());
	}

	@Test
	@Order(9)
	@DisplayName("Trade 목록 조회 성공")
	void getTrades_success() throws Exception {
		mockMvc.perform(post("/api/trades")
			.header("Authorization", "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content(String.format("""
				{
					"ticketIds": [%d],
					"type": "EXCHANGE",
					"price": null
				}
				""", ticketIds.get(19))));

		mockMvc.perform(post("/api/trades")
			.header("Authorization", "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content(String.format("""
				{
					"ticketIds": [%d],
					"type": "TRANSFER",
					"price": 50000
				}
				""", ticketIds.get(20))));

		mockMvc.perform(get("/api/trades")
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.size").value(10))
			.andExpect(jsonPath("$.data.content.length()").value(2));
	}

	@Test
	@Order(10)
	@DisplayName("Trade 목록 조회 - type 필터")
	void getTrades_withTypeFilter() throws Exception {
		mockMvc.perform(post("/api/trades")
			.header("Authorization", "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content(String.format("""
				{
					"ticketIds": [%d],
					"type": "EXCHANGE",
					"price": null
				}
				""", ticketIds.get(29))));

		mockMvc.perform(post("/api/trades")
			.header("Authorization", "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content(String.format("""
				{
					"ticketIds": [%d],
					"type": "TRANSFER",
					"price": 50000
				}
				""", ticketIds.get(30))));

		mockMvc.perform(get("/api/trades?type=EXCHANGE")
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content[0].type").value("EXCHANGE"));
	}

	@Test
	@Order(11)
	@DisplayName("Trade 목록 조회 - status 필터")
	void getTrades_withStatusFilter() throws Exception {
		mockMvc.perform(post("/api/trades")
			.header("Authorization", "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content(String.format("""
				{
					"ticketIds": [%d],
					"type": "EXCHANGE",
					"price": null
				}
				""", ticketIds.get(39))));

		mockMvc.perform(get("/api/trades?status=ACTIVE")
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"));
	}

	@Test
	@Order(12)
	@DisplayName("Trade 목록 조회 - 페이징")
	void getTrades_withPaging() throws Exception {
		for (int i = 49; i < 54; i++) {
			mockMvc.perform(post("/api/trades")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(String.format("""
					{
						"ticketIds": [%d],
						"type": "EXCHANGE",
						"price": null
					}
					""", ticketIds.get(i))));
		}

		mockMvc.perform(get("/api/trades?size=3")
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(3))
			.andExpect(jsonPath("$.data.size").value(3))
			.andExpect(jsonPath("$.data.totalElements").exists());
	}
}