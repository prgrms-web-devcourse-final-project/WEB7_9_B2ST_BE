package com.back.b2st.domain.lottery.entry.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.auth.dto.request.LoginReq;
import com.back.b2st.domain.auth.error.AuthErrorCode;
import com.back.b2st.domain.lottery.entry.error.LotteryEntryErrorCode;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.entity.PerformanceStatus;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.seat.grade.entity.SeatGrade;
import com.back.b2st.domain.seat.grade.entity.SeatGradeType;
import com.back.b2st.domain.seat.grade.error.SeatGradeErrorCode;
import com.back.b2st.domain.seat.grade.repository.SeatGradeRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.domain.venue.venue.repository.VenueRepository;
import com.back.b2st.global.test.AbstractContainerBaseTest;

import jakarta.persistence.EntityManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class LotteryEntryControllerTest extends AbstractContainerBaseTest {

	@Autowired
	private MockMvc mvc;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private PerformanceRepository performanceRepository;
	@Autowired
	private VenueRepository venueRepository;
	@Autowired
	private SectionRepository sectionRepository;
	@Autowired
	private SeatRepository seatRepository;
	@Autowired
	private PerformanceScheduleRepository performanceScheduleRepository;
	@Autowired
	private SeatGradeRepository seatGradeRepository;

	@Autowired
	private EntityManager em;

	private String getMyUrl = "/api/mypage/lottery/entries";
	private String createUrl = "/api/performances/{performanceId}/lottery/entry";

	private Member member;
	private Performance performance;
	private Section section;
	private Seat seat;
	private PerformanceSchedule performanceSchedule;
	private SeatGrade seatGrade;
	private Venue venue;
	private String accessToken;

	@BeforeEach
	void setUp() throws Exception {
		String email = "lotteryEntry@tt.com";
		String password = "P@$$w0rd";

		member = memberRepository.save(Member.builder()
			.email(email)
			.password(passwordEncoder.encode(password))
			.name("추첨응모")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build());

		accessToken = getAccessToken(email, password);

		venue = venueRepository.save(Venue.builder()
			.name("잠실실내체육관")
			.build());

		performance = performanceRepository.save(Performance.builder()
			.venue(venue)
			.title("2024 아이유 콘서트 - HEREH WORLD TOUR")
			.category("콘서트")
			.posterUrl("")
			.description(null)
			.startDate(LocalDateTime.of(2024, 12, 20, 19, 0))
			.endDate(LocalDateTime.of(2024, 12, 22, 21, 0))
			.status(PerformanceStatus.ON_SALE)
			.build());

		// Section 생성 추가
		section = sectionRepository.save(Section.builder()
			.venueId(venue.getVenueId())
			.sectionName("A")
			.build());

		// Seat 생성 추가
		seat = seatRepository.save(Seat.builder()
			.venueId(venue.getVenueId())
			.sectionId(section.getId())
			.sectionName("A")
			.rowLabel("5")
			.seatNumber(7)
			.build());

		performanceSchedule = performanceScheduleRepository.save(
			PerformanceSchedule.builder()
				.performance(performance)
				.startAt(LocalDateTime.of(2024, 12, 20, 19, 0))
				.roundNo(1)
				.bookingType(BookingType.LOTTERY)
				.bookingOpenAt(LocalDateTime.of(2024, 12, 10, 12, 0))
				.bookingCloseAt(LocalDateTime.of(2024, 12, 15, 23, 59))
				.build());

		seatGrade = seatGradeRepository.save(
			SeatGrade.builder()
				.performanceId(performance.getPerformanceId())
				.seatId(seat.getId())
				.grade(SeatGradeType.ROYAL)
				.price(10000)
				.build());

		Seat seat2 = seatRepository.save(Seat.builder()
			.venueId(venue.getVenueId())
			.sectionId(section.getId())
			.sectionName("A")
			.rowLabel("8")
			.seatNumber(70)
			.build());

		seatGradeRepository.save(
			SeatGrade.builder()
				.performanceId(performance.getPerformanceId())
				.seatId(seat2.getId())
				.grade(SeatGradeType.ROYAL)
				.price(10000)
				.build());

		Seat seat3 = seatRepository.save(Seat.builder()
			.venueId(venue.getVenueId())
			.sectionId(section.getId())
			.sectionName("B")
			.rowLabel("1")
			.seatNumber(13)
			.build());

		seatGradeRepository.save(
			SeatGrade.builder()
				.performanceId(performance.getPerformanceId())
				.seatId(seat3.getId())
				.grade(SeatGradeType.VIP)
				.price(10000)
				.build());

		em.flush();
		em.clear();
	}

	private String getAccessToken(String email, String password) throws Exception {
		LoginReq loginRequest = new LoginReq(email, password);

		String loginResponse = mvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andReturn().getResponse().getContentAsString();

		JsonNode jsonNode = objectMapper.readTree(loginResponse);
		return jsonNode.path("data").path("accessToken").textValue();
	}

	@Test
	@DisplayName("좌석정보조회_성공")
	void getSeatInfo_success() throws Exception {
		// given
		String url = "/api/performances/{performanceId}/lottery/section";
		Long param = performance.getPerformanceId();

		// when & then
		mvc.perform(
				get(url, param)
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data[0].sectionName").value("A"))
			.andExpect(jsonPath("$.data[0].grades[0].grade").value(seatGrade.getGrade().toString()))
			.andExpect(jsonPath("$.data[0].grades[0].rows[0]").value(seat.getRowLabel()));
		;
	}

	@Test
	@DisplayName("추첨응모_성공")
	void registerLotteryEntry_success() throws Exception {
		// given
		Long param = performance.getPerformanceId();

		Long memberId = member.getId();
		Long scheduleId = performanceSchedule.getPerformanceScheduleId();
		String grade = seatGrade.getGrade().toString();
		int quantity = 4;

		String requestBody = "{"
			+ "\"scheduleId\": " + scheduleId + ","
			+ "\"grade\": \"" + grade + "\","
			+ "\"quantity\": " + quantity
			+ "}";

		System.out.println("requestBody = " + requestBody);

		// when & then
		mvc.perform(
				post(createUrl, param)
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody)
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value("1"))
			.andExpect(jsonPath("$.data.memberId").value(memberId))
			.andExpect(jsonPath("$.data.performanceId").value(param))
			.andExpect(jsonPath("$.data.scheduleId").value(scheduleId))
			.andExpect(jsonPath("$.data.grade").value(grade))
			.andExpect(jsonPath("$.data.quantity").value(quantity))
			.andExpect(jsonPath("$.data.status").value("APPLIED"))
		;
	}

	@Test
	@DisplayName("추첨응모_실패_공연")
	void registerLotteryEntry_fail_performance() throws Exception {
		// given
		String url = "/api/performances/{performanceId}/lottery/entry";
		Long param = 9999L;

		Long memberId = member.getId();
		Long scheduleId = performanceSchedule.getPerformanceScheduleId();
		String grade = seatGrade.getGrade().toString();
		int quantity = 4;

		String requestBody = "{"
			+ "\"scheduleId\": " + scheduleId + ","
			+ "\"grade\": \"" + grade + "\","
			+ "\"quantity\": " + quantity
			+ "}";

		// when & then
		mvc.perform(
				post(url, param)
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody)
			)
			.andDo(print())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value(LotteryEntryErrorCode.PERFORMANCE_NOT_FOUND.getMessage()));
	}

	@Test
	@DisplayName("추첨응모_실패_응모자")
	void registerLotteryEntry_fail_member() throws Exception {
		// given
		String url = "/api/performances/{performanceId}/lottery/entry";
		Long param = performance.getPerformanceId();

		Long scheduleId = 2L;
		String grade = seatGrade.getGrade().toString();
		int quantity = 4;

		String requestBody = "{"
			+ "\"scheduleId\": " + scheduleId + ","
			+ "\"grade\": \"" + grade + "\","
			+ "\"quantity\": " + quantity
			+ "}";

		accessToken = "";

		// when & then
		mvc.perform(
				post(url, param)
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody)
			)
			.andDo(print())
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value(AuthErrorCode.INVALID_TOKEN.getMessage()))
		;
	}

	@Test
	@DisplayName("추첨응모_실패_회차")
	void registerLotteryEntry_fail_schedule() throws Exception {
		// given
		String url = "/api/performances/{performanceId}/lottery/entry";
		Long param = performance.getPerformanceId();

		Long memberId = member.getId();
		Long scheduleId = 999L;
		String grade = seatGrade.getGrade().toString();
		int quantity = 4;

		String requestBody = "{"
			+ "\"scheduleId\": " + scheduleId + ","
			+ "\"grade\": \"" + grade + "\","
			+ "\"quantity\": " + quantity
			+ "}";

		// when & then
		mvc.perform(
				post(url, param)
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody)
			)
			.andDo(print())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value(LotteryEntryErrorCode.SCHEDULE_NOT_FOUND.getMessage()));
	}

	@Test
	@DisplayName("추첨응모_실패_좌석등급")
	void registerLotteryEntry_fail_seatGrade() throws Exception {
		// given
		String url = "/api/performances/{performanceId}/lottery/entry";
		Long param = performance.getPerformanceId();

		Long memberId = member.getId();
		Long scheduleId = performanceSchedule.getPerformanceScheduleId();
		String grade = "NO";
		int quantity = 4;

		String requestBody = "{"
			+ "\"scheduleId\": " + scheduleId + ","
			+ "\"grade\": \"" + grade + "\","
			+ "\"quantity\": " + quantity
			+ "}";

		// when & then
		mvc.perform(
				post(url, param)
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody)
			)
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value(SeatGradeErrorCode.INVALID_GRADE_TYPE.getMessage()));
	}

	@Test
	@DisplayName("추첨응모_실패_인원수0")
	void registerLotteryEntry_fail_quantityZero() throws Exception {
		// given
		String url = "/api/performances/{performanceId}/lottery/entry";
		Long param = performance.getPerformanceId();

		Long memberId = member.getId();
		Long scheduleId = 2L;
		String grade = seatGrade.getGrade().toString();
		int quantity = 0;

		String requestBody = "{"
			+ "\"scheduleId\": " + scheduleId + ","
			+ "\"grade\": \"" + grade + "\","
			+ "\"quantity\": " + quantity
			+ "}";

		// when & then
		mvc.perform(
				post(url, param)
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody)
			)
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("인원수는 1 이상이어야 합니다."))
		;
	}

	@Test
	@DisplayName("추첨응모_실패_신청인원초과")
	void registerLotteryEntry_fail_quantity() throws Exception {
		// given
		String url = "/api/performances/{performanceId}/lottery/entry";
		Long param = performance.getPerformanceId();

		Long memberId = member.getId();
		;
		Long scheduleId = 2L;
		String grade = seatGrade.getGrade().toString();
		int quantity = 4 + 1;

		String requestBody = "{"
			+ "\"scheduleId\": " + scheduleId + ","
			+ "\"grade\": \"" + grade + "\","
			+ "\"quantity\": " + quantity
			+ "}";

		// when & then
		mvc.perform(
				post(url, param)
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody)
			)
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value(LotteryEntryErrorCode.EXCEEDS_MAX_ALLOCATION.getMessage()))
		;
	}

	@Test
	@DisplayName("추첨응모_실패_중복응모")
	void registerLotteryEntry_fail_duplicate() throws Exception {
		// given
		String url = "/api/performances/{performanceId}/lottery/entry";
		Long param = performance.getPerformanceId();

		Long memberId = member.getId();
		Long scheduleId = performanceSchedule.getPerformanceScheduleId();
		String grade = seatGrade.getGrade().toString();
		int quantity = 4;

		String requestBody = "{"
			+ "\"scheduleId\": " + scheduleId + ","
			+ "\"grade\": \"" + grade + "\","
			+ "\"quantity\": " + quantity
			+ "}";

		// when & then
		mvc.perform(
				post(url, param)
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody)
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(201))
		;

		mvc.perform(
				post(url, param)
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody)
			)
			.andDo(print())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value(LotteryEntryErrorCode.DUPLICATE_ENTRY.getMessage()))
		;
	}

	@Test
	@DisplayName("내응모조회 - 성공")
	void getMyLotteryEntries_success() throws Exception {
		// given
		String url = getMyUrl;
		Long param = performance.getPerformanceId();

		Long memberId = member.getId();
		Long scheduleId = performanceSchedule.getPerformanceScheduleId();
		SeatGradeType grade = seatGrade.getGrade();

		// 테스트 데이터 3
		for (int quantity = 1; quantity < 3; quantity++) {
			scheduleId = performanceScheduleRepository.save(
				PerformanceSchedule.builder()
					.performance(performance)
					.startAt(LocalDateTime.of(2024, 12, 20, 19, 0))
					.roundNo(quantity)
					.bookingType(BookingType.LOTTERY)
					.bookingOpenAt(LocalDateTime.of(2024, 12, 10, 12, 0))
					.bookingCloseAt(LocalDateTime.of(2024, 12, 15, 23, 59))
					.build()).getPerformanceScheduleId();

			grade = switch (quantity) {
				case 1 -> SeatGradeType.STANDARD;
				case 2 -> SeatGradeType.VIP;
				case 3 -> SeatGradeType.A;
				default -> SeatGradeType.RESTRICTED_VIEW;
			};

			String requestBody = "{"
				+ "\"scheduleId\": " + scheduleId + ","
				+ "\"grade\": \"" + grade.toString() + "\","
				+ "\"quantity\": " + quantity
				+ "}";

			mvc.perform(
					post(createUrl, param)
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody)
				)
				// .andDo(print())
				.andExpect(status().isOk())
			;
		}

		// when
		mvc.perform(
				get(getMyUrl)
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andDo(print())
			.andExpect(status().isOk());
	}

}
