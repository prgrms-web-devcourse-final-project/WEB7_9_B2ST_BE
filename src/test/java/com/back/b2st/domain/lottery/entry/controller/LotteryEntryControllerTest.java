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

import com.back.b2st.domain.lottery.constants.LotteryConstants;
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

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@ActiveProfiles("test")
class LotteryEntryControllerTest {

	@Autowired
	private MockMvc mvc;
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

	private Member tMember;
	private Performance performance;
	private Section section;
	private Seat seat;
	private PerformanceSchedule performanceSchedule;
	private SeatGrade seatGrade;
	private Venue venue;

	@BeforeEach
	void setUp() {
		Member user1 = Member.builder()
			.email("lotteryEntry@tt.com")
			.password(passwordEncoder.encode("1234"))
			.name("추첨응모")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isVerified(true)
			.build();

		tMember = memberRepository.save(user1);

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
	}

	record TestRequest(Long memberId, Long scheduleId, Long seatGradeId, Integer quantity) {
		String toJson() {
			return String.format("""
				{
				    "memberId": %d,
				    "scheduleId": %d,
				    "seatGradeId": %d,
				    "quantity": %d
				}
				""", memberId, scheduleId, seatGradeId, quantity);
		}
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
		String url = "/api/performances/{performanceId}/lottery/entry";
		Long param = performance.getPerformanceId();

		Long memberId = tMember.getId();
		Long scheduleId = performanceSchedule.getPerformanceScheduleId();
		String grade = seatGrade.getGrade().toString();
		int quantity = 4;

		String requestBody = "{"
			+ "\"memberId\": " + memberId + ","
			+ "\"scheduleId\": " + scheduleId + ","
			+ "\"grade\": \"" + grade + "\","
			+ "\"quantity\": " + quantity
			+ "}";

		System.out.println("requestBody = " + requestBody);

		// when & then
		mvc.perform(
				post(url, param)
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
		Long param = 999L;

		Long memberId = tMember.getId();
		Long scheduleId = performanceSchedule.getPerformanceScheduleId();
		String grade = seatGrade.getGrade().toString();
		int quantity = 4;

		String requestBody = "{"
			+ "\"memberId\": " + memberId + ","
			+ "\"scheduleId\": " + scheduleId + ","
			+ "\"grade\": \"" + grade + "\","
			+ "\"quantity\": " + quantity
			+ "}";

		// when & then
		mvc.perform(
				post(url, param)
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

		Long memberId = 99999999L;
		Long scheduleId = 2L;
		String grade = seatGrade.getGrade().toString();
		int quantity = 4;

		String requestBody = "{"
			+ "\"memberId\": " + memberId + ","
			+ "\"scheduleId\": " + scheduleId + ","
			+ "\"grade\": \"" + grade + "\","
			+ "\"quantity\": " + quantity
			+ "}";

		// when & then
		mvc.perform(
				post(url, param)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody)
			)
			.andDo(print())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value(LotteryEntryErrorCode.MEMBER_NOT_FOUND.getMessage()))
		;
	}

	@Test
	@DisplayName("추첨응모_실패_회차")
	void registerLotteryEntry_fail_schedule() throws Exception {
		// given
		String url = "/api/performances/{performanceId}/lottery/entry";
		Long param = performance.getPerformanceId();

		Long memberId = tMember.getId();
		Long scheduleId = 999L;
		String grade = seatGrade.getGrade().toString();
		int quantity = 4;

		String requestBody = "{"
			+ "\"memberId\": " + memberId + ","
			+ "\"scheduleId\": " + scheduleId + ","
			+ "\"grade\": \"" + grade + "\","
			+ "\"quantity\": " + quantity
			+ "}";

		// when & then
		mvc.perform(
				post(url, param)
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

		Long memberId = tMember.getId();
		Long scheduleId = performanceSchedule.getPerformanceScheduleId();
		String grade = "NO";
		int quantity = 4;

		String requestBody = "{"
			+ "\"memberId\": " + memberId + ","
			+ "\"scheduleId\": " + scheduleId + ","
			+ "\"grade\": \"" + grade + "\","
			+ "\"quantity\": " + quantity
			+ "}";

		// when & then
		mvc.perform(
				post(url, param)
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

		Long memberId = tMember.getId();
		Long scheduleId = 2L;
		String grade = seatGrade.getGrade().toString();
		int quantity = 0;

		String requestBody = "{"
			+ "\"memberId\": " + memberId + ","
			+ "\"scheduleId\": " + scheduleId + ","
			+ "\"grade\": \"" + grade + "\","
			+ "\"quantity\": " + quantity
			+ "}";

		// when & then
		mvc.perform(
				post(url, param)
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

		Long memberId = tMember.getId();
		;
		Long scheduleId = 2L;
		String grade = seatGrade.getGrade().toString();
		int quantity = LotteryConstants.MAX_LOTTERY_ENTRY_COUNT + 1;

		String requestBody = "{"
			+ "\"memberId\": " + memberId + ","
			+ "\"scheduleId\": " + scheduleId + ","
			+ "\"grade\": \"" + grade + "\","
			+ "\"quantity\": " + quantity
			+ "}";

		// when & then
		mvc.perform(
				post(url, param)
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

		Long memberId = tMember.getId();
		Long scheduleId = performanceSchedule.getPerformanceScheduleId();
		String grade = seatGrade.getGrade().toString();
		int quantity = 4;

		String requestBody = "{"
			+ "\"memberId\": " + memberId + ","
			+ "\"scheduleId\": " + scheduleId + ","
			+ "\"grade\": \"" + grade + "\","
			+ "\"quantity\": " + quantity
			+ "}";

		// when & then
		mvc.perform(
				post(url, param)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody)
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(201))
		;

		mvc.perform(
				post(url, param)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody)
			)
			.andDo(print())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value(LotteryEntryErrorCode.DUPLICATE_ENTRY.getMessage()))
		;
	}

}
