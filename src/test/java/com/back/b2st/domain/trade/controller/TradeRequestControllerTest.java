package com.back.b2st.domain.trade.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

import com.back.b2st.security.UserPrincipal;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TradeRequestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private Authentication tradeOwnerAuth;
	private Authentication requesterAuth;

	@BeforeEach
	void setup() {
		UserPrincipal tradeOwner = UserPrincipal.builder()
			.id(100L)
			.email("owner@test.com")
			.role("ROLE_MEMBER")
			.build();

		UserPrincipal requester = UserPrincipal.builder()
			.id(200L)
			.email("requester@test.com")
			.role("ROLE_MEMBER")
			.build();

		tradeOwnerAuth = new UsernamePasswordAuthenticationToken(tradeOwner, null, null);
		requesterAuth = new UsernamePasswordAuthenticationToken(requester, null, null);
	}

	@Test
	@DisplayName("교환 신청 생성 성공")
	void createTradeRequest_success() throws Exception {
		// given - 먼저 Trade 생성
		String createTradeRequest = """
			{
				"ticketId": 100,
				"type": "EXCHANGE",
				"price": null,
				"totalCount": 1
			}
			""";

		String tradeResponse = mockMvc.perform(post("/api/trades")
				.with(authentication(tradeOwnerAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createTradeRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(tradeResponse).get("data").get("tradeId").asLong();

		// when & then - TradeRequest 생성
		String createRequestBody = """
			{
				"requesterTicketId": 101
			}
			""";

		mockMvc.perform(post("/api/trades/" + tradeId + "/requests")
				.with(authentication(requesterAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequestBody))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.tradeId").value(tradeId))
			.andExpect(jsonPath("$.data.requesterId").value(200))
			.andExpect(jsonPath("$.data.requesterTicketId").value(101))
			.andExpect(jsonPath("$.data.status").value("PENDING"));
	}

	@Test
	@DisplayName("교환 신청 생성 실패 - 자신의 게시글")
	void createTradeRequest_fail_ownTrade() throws Exception {
		// given - Trade 생성
		String createTradeRequest = """
			{
				"ticketId": 110,
				"type": "EXCHANGE",
				"price": null,
				"totalCount": 1
			}
			""";

		String tradeResponse = mockMvc.perform(post("/api/trades")
				.with(authentication(tradeOwnerAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createTradeRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(tradeResponse).get("data").get("tradeId").asLong();

		// when & then - 자신의 게시글에 신청
		String createRequestBody = """
			{
				"requesterTicketId": 111
			}
			""";

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
				"ticketId": 120,
				"type": "EXCHANGE",
				"price": null,
				"totalCount": 1
			}
			""";

		String tradeResponse = mockMvc.perform(post("/api/trades")
				.with(authentication(tradeOwnerAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createTradeRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(tradeResponse).get("data").get("tradeId").asLong();

		String createRequestBody = """
			{
				"requesterTicketId": 121
			}
			""";

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
				"ticketId": 130,
				"type": "EXCHANGE",
				"price": null,
				"totalCount": 1
			}
			""";

		String tradeResponse = mockMvc.perform(post("/api/trades")
				.with(authentication(tradeOwnerAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createTradeRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(tradeResponse).get("data").get("tradeId").asLong();

		String createRequestBody = """
			{
				"requesterTicketId": 131
			}
			""";

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
				"ticketId": 140,
				"type": "EXCHANGE",
				"price": null,
				"totalCount": 1
			}
			""";

		String tradeResponse = mockMvc.perform(post("/api/trades")
				.with(authentication(tradeOwnerAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createTradeRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(tradeResponse).get("data").get("tradeId").asLong();

		// 첫 번째 신청
		mockMvc.perform(post("/api/trades/" + tradeId + "/requests")
			.with(authentication(requesterAuth))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"requesterTicketId\": 141}"));

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
				"ticketId": 150,
				"type": "EXCHANGE",
				"price": null,
				"totalCount": 1
			}
			""";

		String tradeResponse = mockMvc.perform(post("/api/trades")
				.with(authentication(tradeOwnerAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createTradeRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(tradeResponse).get("data").get("tradeId").asLong();

		mockMvc.perform(post("/api/trades/" + tradeId + "/requests")
			.with(authentication(requesterAuth))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"requesterTicketId\": 151}"));

		// when & then - 신청자별 조회
		mockMvc.perform(get("/api/trade-requests?requesterId=200")
				.with(authentication(requesterAuth)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].requesterId").value(200));
	}

	@Test
	@DisplayName("교환 신청 수락 성공")
	void acceptTradeRequest_success() throws Exception {
		// given - Trade 생성 및 신청
		String createTradeRequest = """
			{
				"ticketId": 160,
				"type": "EXCHANGE",
				"price": null,
				"totalCount": 1
			}
			""";

		String tradeResponse = mockMvc.perform(post("/api/trades")
				.with(authentication(tradeOwnerAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createTradeRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(tradeResponse).get("data").get("tradeId").asLong();

		String requestResponse = mockMvc.perform(post("/api/trades/" + tradeId + "/requests")
				.with(authentication(requesterAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"requesterTicketId\": 161}"))
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
				"ticketId": 170,
				"type": "EXCHANGE",
				"price": null,
				"totalCount": 1
			}
			""";

		String tradeResponse = mockMvc.perform(post("/api/trades")
				.with(authentication(tradeOwnerAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createTradeRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(tradeResponse).get("data").get("tradeId").asLong();

		String requestResponse = mockMvc.perform(post("/api/trades/" + tradeId + "/requests")
				.with(authentication(requesterAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"requesterTicketId\": 171}"))
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
				"ticketId": 180,
				"type": "EXCHANGE",
				"price": null,
				"totalCount": 1
			}
			""";

		String tradeResponse = mockMvc.perform(post("/api/trades")
				.with(authentication(tradeOwnerAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createTradeRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(tradeResponse).get("data").get("tradeId").asLong();

		String requestResponse = mockMvc.perform(post("/api/trades/" + tradeId + "/requests")
				.with(authentication(requesterAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"requesterTicketId\": 181}"))
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
				"ticketId": 190,
				"type": "EXCHANGE",
				"price": null,
				"totalCount": 1
			}
			""";

		String tradeResponse = mockMvc.perform(post("/api/trades")
				.with(authentication(tradeOwnerAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createTradeRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(tradeResponse).get("data").get("tradeId").asLong();

		String requestResponse = mockMvc.perform(post("/api/trades/" + tradeId + "/requests")
				.with(authentication(requesterAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"requesterTicketId\": 191}"))
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
				"ticketId": 200,
				"type": "EXCHANGE",
				"price": null,
				"totalCount": 1
			}
			""";

		String tradeResponse = mockMvc.perform(post("/api/trades")
				.with(authentication(tradeOwnerAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(createTradeRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(tradeResponse).get("data").get("tradeId").asLong();

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
