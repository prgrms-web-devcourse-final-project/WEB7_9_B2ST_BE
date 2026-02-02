package com.back.b2st.domain.prereservation.entry.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

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
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.entity.PerformanceStatus;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.prereservation.entry.entity.Prereservation;
import com.back.b2st.domain.prereservation.entry.repository.PrereservationRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.domain.venue.venue.repository.VenueRepository;
import com.back.b2st.global.test.AbstractContainerBaseTest;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PrereservationMyApplicationControllerTest extends AbstractContainerBaseTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private PrereservationRepository prereservationRepository;

	@Autowired
	private VenueRepository venueRepository;

	@Autowired
	private PerformanceRepository performanceRepository;

	@Autowired
	private PerformanceScheduleRepository performanceScheduleRepository;

	private String accessToken;
	private Long memberId;
	private Long scheduleId1;
	private Long scheduleId2;
	private Long sectionIdA;
	private Long sectionIdB;

	@BeforeEach
	void setUp() throws Exception {
		String email = "prereservation-test@tt.com";
		String password = "1234567a!";

		Member member = memberRepository.save(
			Member.builder()
				.email(email)
				.password(passwordEncoder.encode(password))
				.name("신청예매테스트")
				.role(Member.Role.MEMBER)
				.provider(Member.Provider.EMAIL)
				.isEmailVerified(true)
				.isIdentityVerified(true)
				.build()
		);

		accessToken = getAccessToken(email, password);
		memberId = member.getId();

		Venue venue = venueRepository.save(
			Venue.builder()
				.name("신청예매테스트공연장")
				.build()
		);

		Performance performance = performanceRepository.save(
			Performance.builder()
				.venue(venue)
				.title("신청예매테스트공연")
				.category("테스트")
				.description("신청예매 테스트용 공연입니다.")
				.startDate(LocalDateTime.of(2026, 1, 1, 0, 0))
				.endDate(LocalDateTime.of(2026, 12, 31, 23, 59))
				.status(PerformanceStatus.ACTIVE)
				.build()
		);

		PerformanceSchedule schedule1 = performanceScheduleRepository.save(
			PerformanceSchedule.builder()
				.performance(performance)
				.startAt(LocalDateTime.of(2026, 1, 6, 19, 0))
				.roundNo(1)
				.bookingType(BookingType.PRERESERVE)
				.bookingOpenAt(LocalDateTime.of(2026, 1, 5, 0, 0))
				.bookingCloseAt(LocalDateTime.of(2026, 2, 5, 0, 0))
				.build()
		);

		PerformanceSchedule schedule2 = performanceScheduleRepository.save(
			PerformanceSchedule.builder()
				.performance(performance)
				.startAt(LocalDateTime.of(2026, 1, 7, 19, 0))
				.roundNo(2)
				.bookingType(BookingType.PRERESERVE)
				.bookingOpenAt(LocalDateTime.of(2026, 1, 5, 0, 0))
				.bookingCloseAt(LocalDateTime.of(2026, 2, 5, 0, 0))
				.build()
		);

		scheduleId1 = schedule1.getPerformanceScheduleId();
		scheduleId2 = schedule2.getPerformanceScheduleId();
		sectionIdA = 10L;
		sectionIdB = 20L;

		// 사전 신청 데이터 생성
		prereservationRepository.saveAll(List.of(
			Prereservation.builder()
				.performanceScheduleId(scheduleId1)
				.memberId(memberId)
				.sectionId(sectionIdA)
				.build(),
			Prereservation.builder()
				.performanceScheduleId(scheduleId1)
				.memberId(memberId)
				.sectionId(sectionIdB)
				.build(),
			Prereservation.builder()
				.performanceScheduleId(scheduleId2)
				.memberId(memberId)
				.sectionId(sectionIdA)
				.build()
		));
	}

	@Test
	@DisplayName("내 사전 신청 전체 조회 - 성공")
	void getMyApplications_success() throws Exception {
		// when & then
		mockMvc.perform(get("/api/prereservations/applications/me")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data[0].scheduleId").value(scheduleId1))
			.andExpect(jsonPath("$.data[0].sectionIds.length()").value(2))
			.andExpect(jsonPath("$.data[0].sectionIds[0]").value(sectionIdA))
			.andExpect(jsonPath("$.data[0].sectionIds[1]").value(sectionIdB))
			.andExpect(jsonPath("$.data[1].scheduleId").value(scheduleId2))
			.andExpect(jsonPath("$.data[1].sectionIds.length()").value(1))
			.andExpect(jsonPath("$.data[1].sectionIds[0]").value(sectionIdA));
	}

	@Test
	@DisplayName("내 사전 신청 전체 조회 - 인증 없이 호출 시 401")
	void getMyApplications_unauthorized() throws Exception {
		// when & then
		mockMvc.perform(get("/api/prereservations/applications/me")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("내 사전 신청 전체 조회 - 신청 내역이 없으면 빈 배열 반환")
	void getMyApplications_empty() throws Exception {
		// given - 신청 내역 없는 다른 유저 생성
		String email = "prereservation-empty@tt.com";
		String password = "1234567a!";
		memberRepository.save(
			Member.builder()
				.email(email)
				.password(passwordEncoder.encode(password))
				.name("신청예매빈테스트")
				.role(Member.Role.MEMBER)
				.provider(Member.Provider.EMAIL)
				.isEmailVerified(true)
				.isIdentityVerified(true)
				.build()
		);

		String otherAccessToken = getAccessToken(email, password);

		// when & then
		mockMvc.perform(get("/api/prereservations/applications/me")
				.header("Authorization", "Bearer " + otherAccessToken)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(0));
	}

	private String getAccessToken(String email, String password) throws Exception {
		LoginReq loginReq = new LoginReq(email, password);
		String response = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginReq)))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();

		JsonNode jsonNode = objectMapper.readTree(response);
		if (!jsonNode.has("data") || !jsonNode.get("data").has("accessToken")) {
			throw new IllegalStateException("Login response missing accessToken: " + response);
		}
		return jsonNode.path("data").path("accessToken").asText();
	}
}
