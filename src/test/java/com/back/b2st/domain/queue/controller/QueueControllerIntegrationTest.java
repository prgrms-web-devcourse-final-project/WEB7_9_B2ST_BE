package com.back.b2st.domain.queue.controller;

import com.back.b2st.domain.queue.entity.Queue;
import com.back.b2st.domain.queue.repository.QueueRepository;
import com.back.b2st.security.UserPrincipal;
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
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QueueController 전체 플로우 통합 테스트
 *
 * 실제 HTTP 요청/응답 검증
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("QueueController 통합 테스트")
class QueueControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private QueueRepository queueRepository;

	private Authentication memberAuth;
	private Long memberId;
	private Long queueId;
	private Long scheduleId;

	@BeforeEach
	void setup() {
		memberId = 100L;
		scheduleId = 1L;

		// 인증 설정
		UserPrincipal member = UserPrincipal.builder()
			.id(memberId)
			.email("test@test.com")
			.role("ROLE_MEMBER")
			.build();
		memberAuth = new UsernamePasswordAuthenticationToken(member, null, null);

		// 테스트 대기열 생성
		Queue queue = Queue.builder()
			.performanceId(1L)
			.maxActiveUsers(100)
			.entryTtlMinutes(10)
			.build();
		queue = queueRepository.save(queue);
		queueId = queue.getId();
	}

	@Test
	@DisplayName("예매 시작 API - 성공")
	void testStartBookingApi() throws Exception {
		mockMvc.perform(post("/api/queues/start-booking/{scheduleId}", scheduleId)
				.with(authentication(memberAuth))
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.code").value(201))
			.andExpect(jsonPath("$.data.queueId").exists())
			.andExpect(jsonPath("$.data.performanceId").value(1))
			.andExpect(jsonPath("$.data.scheduleId").value(scheduleId))
			.andExpect(jsonPath("$.data.entry.userId").value(memberId))
			.andExpect(jsonPath("$.data.entry.status").value("WAITING"));
	}

	@Test
	@DisplayName("예매 시작 API - 멱등성 (중복 호출)")
	void testStartBookingApiIdempotent() throws Exception {
		// 첫 번째 호출
		mockMvc.perform(post("/api/queues/start-booking/{scheduleId}", scheduleId)
				.with(authentication(memberAuth))
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isCreated());

		// 두 번째 호출 - 예외 발생하지 않고 현재 상태 반환
		mockMvc.perform(post("/api/queues/start-booking/{scheduleId}", scheduleId)
				.with(authentication(memberAuth))
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.code").value(201))
			.andExpect(jsonPath("$.data.entry.status").exists());
	}

	@Test
	@DisplayName("대기 순번 조회 API - WAITING 상태")
	void testGetPositionApi() throws Exception {
		// Given: 대기열 진입
		mockMvc.perform(post("/api/queues/start-booking/{scheduleId}", scheduleId)
				.with(authentication(memberAuth))
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isCreated());

		// When & Then: 순번 조회
		mockMvc.perform(get("/api/queues/{queueId}/position", queueId)
				.with(authentication(memberAuth))
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data.queueId").value(queueId))
			.andExpect(jsonPath("$.data.userId").value(memberId))
			.andExpect(jsonPath("$.data.status").value("WAITING"))
			.andExpect(jsonPath("$.data.myRank").isNumber())
			.andExpect(jsonPath("$.data.aheadCount").isNumber());
	}

	@Test
	@DisplayName("대기 순번 조회 API - 대기열에 없는 경우")
	void testGetPositionApiNotInQueue() throws Exception {
		mockMvc.perform(get("/api/queues/{queueId}/position", queueId)
				.with(authentication(memberAuth))
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data.status").value("NOT_IN_QUEUE"));
	}

	@Test
	@DisplayName("대기열 나가기 API - 성공")
	void testExitQueueApi() throws Exception {
		// Given: 대기열 진입
		mockMvc.perform(post("/api/queues/start-booking/{scheduleId}", scheduleId)
				.with(authentication(memberAuth))
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isCreated());

		// When & Then: 나가기
		mockMvc.perform(delete("/api/queues/{queueId}/exit", queueId)
				.with(authentication(memberAuth))
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200));

		// 나간 후 상태 확인
		mockMvc.perform(get("/api/queues/{queueId}/position", queueId)
				.with(authentication(memberAuth))
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("NOT_IN_QUEUE"));
	}

	@Test
	@DisplayName("존재하지 않는 대기열 조회 - 404")
	void testQueueNotFoundApi() throws Exception {
		Long invalidQueueId = 999L;

		mockMvc.perform(get("/api/queues/{queueId}/position", invalidQueueId)
				.with(authentication(memberAuth))
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(404))
			.andExpect(jsonPath("$.message").value("존재하지 않는 대기열입니다."));
	}

	@Test
	@DisplayName("인증 없이 접근 - 401")
	void testUnauthorizedAccessApi() throws Exception {
		mockMvc.perform(post("/api/queues/start-booking/{scheduleId}", scheduleId)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("전체 플로우 - 입장 → 순번조회 → 나가기")
	void testCompleteFlowApi() throws Exception {
		// 1. 예매 시작
		mockMvc.perform(post("/api/queues/start-booking/{scheduleId}", scheduleId)
				.with(authentication(memberAuth))
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.entry.status").value("WAITING"));

		// 2. 순번 조회
		mockMvc.perform(get("/api/queues/{queueId}/position", queueId)
				.with(authentication(memberAuth))
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("WAITING"));

		// 3. 나가기
		mockMvc.perform(delete("/api/queues/{queueId}/exit", queueId)
				.with(authentication(memberAuth))
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());

		// 4. 나간 후 확인
		mockMvc.perform(get("/api/queues/{queueId}/position", queueId)
				.with(authentication(memberAuth))
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("NOT_IN_QUEUE"));
	}
}