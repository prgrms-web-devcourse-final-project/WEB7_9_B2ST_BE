package com.back.b2st.domain.trade.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class TradeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("교환 게시글 생성 성공")
	void createExchangeTrade_success() throws Exception {
		// given
		String requestBody = """
			{
				"ticketId": 1,
				"type": "EXCHANGE",
				"price": null,
				"totalCount": 1
			}
			""";

		// when & then
		mockMvc.perform(post("/api/trades")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.type").value("EXCHANGE"))
			.andExpect(jsonPath("$.data.totalCount").value(1))
			.andExpect(jsonPath("$.data.status").value("ACTIVE"));
	}

	@Test
	@DisplayName("양도 게시글 생성 성공")
	void createTransferTrade_success() throws Exception {
		// given
		String requestBody = """
			{
				"ticketId": 2,
				"type": "TRANSFER",
				"price": 50000,
				"totalCount": 2
			}
			""";

		// when & then
		mockMvc.perform(post("/api/trades")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.type").value("TRANSFER"))
			.andExpect(jsonPath("$.data.price").value(50000))
			.andExpect(jsonPath("$.data.totalCount").value(2));
	}

	@Test
	@DisplayName("중복 티켓 등록 실패")
	void createTrade_fail_duplicateTicket() throws Exception {
		// given - 먼저 게시글 생성
		String firstRequest = """
			{
				"ticketId": 3,
				"type": "EXCHANGE",
				"price": null,
				"totalCount": 1
			}
			""";

		mockMvc.perform(post("/api/trades")
			.contentType(MediaType.APPLICATION_JSON)
			.content(firstRequest));

		// when - 동일한 티켓으로 다시 생성 시도
		mockMvc.perform(post("/api/trades")
				.contentType(MediaType.APPLICATION_JSON)
				.content(firstRequest))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(400));
	}

	@Test
	@DisplayName("교환 - totalCount 검증 실패")
	void createExchangeTrade_fail_invalidCount() throws Exception {
		// given
		String requestBody = """
			{
				"ticketId": 4,
				"type": "EXCHANGE",
				"price": null,
				"totalCount": 2
			}
			""";

		// when & then
		mockMvc.perform(post("/api/trades")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(400));
	}

	@Test
	@DisplayName("교환 - price 설정 시 실패")
	void createExchangeTrade_fail_withPrice() throws Exception {
		// given
		String requestBody = """
			{
				"ticketId": 5,
				"type": "EXCHANGE",
				"price": 10000,
				"totalCount": 1
			}
			""";

		// when & then
		mockMvc.perform(post("/api/trades")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(400));
	}

	@Test
	@DisplayName("양도 - price 미설정 시 실패")
	void createTransferTrade_fail_noPrice() throws Exception {
		// given
		String requestBody = """
			{
				"ticketId": 6,
				"type": "TRANSFER",
				"price": null,
				"totalCount": 1
			}
			""";

		// when & then
		mockMvc.perform(post("/api/trades")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(400));
	}

	@Test
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
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andDo(print())
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Trade 상세 조회 성공")
	void getTrade_success() throws Exception {
		String createRequest = """
			{
				"ticketId": 10,
				"type": "EXCHANGE",
				"price": null,
				"totalCount": 1
			}
			""";

		String response = mockMvc.perform(post("/api/trades")
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequest))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long tradeId = objectMapper.readTree(response).get("data").get("tradeId").asLong();

		mockMvc.perform(get("/api/trades/" + tradeId))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.tradeId").value(tradeId))
			.andExpect(jsonPath("$.data.type").value("EXCHANGE"))
			.andExpect(jsonPath("$.data.status").value("ACTIVE"))
			.andExpect(jsonPath("$.data.memberId").exists())
			.andExpect(jsonPath("$.data.section").value("A"));
	}

	@Test
	@DisplayName("Trade 상세 조회 실패 - 존재하지 않는 ID")
	void getTrade_fail_notFound() throws Exception {
		mockMvc.perform(get("/api/trades/99999"))
			.andDo(print())
			.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Trade 목록 조회 성공")
	void getTrades_success() throws Exception {
		mockMvc.perform(post("/api/trades")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
					"ticketId": 20,
					"type": "EXCHANGE",
					"price": null,
					"totalCount": 1
				}
				"""));

		mockMvc.perform(post("/api/trades")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
					"ticketId": 21,
					"type": "TRANSFER",
					"price": 50000,
					"totalCount": 2
				}
				"""));

		mockMvc.perform(get("/api/trades"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.size").value(10))
			.andExpect(jsonPath("$.data.content.length()").value(2));
	}

	@Test
	@DisplayName("Trade 목록 조회 - type 필터")
	void getTrades_withTypeFilter() throws Exception {
		mockMvc.perform(post("/api/trades")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
					"ticketId": 30,
					"type": "EXCHANGE",
					"price": null,
					"totalCount": 1
				}
				"""));

		mockMvc.perform(post("/api/trades")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
					"ticketId": 31,
					"type": "TRANSFER",
					"price": 50000,
					"totalCount": 2
				}
				"""));

		mockMvc.perform(get("/api/trades?type=EXCHANGE"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content[0].type").value("EXCHANGE"));
	}

	@Test
	@DisplayName("Trade 목록 조회 - status 필터")
	void getTrades_withStatusFilter() throws Exception {
		mockMvc.perform(post("/api/trades")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
					"ticketId": 40,
					"type": "EXCHANGE",
					"price": null,
					"totalCount": 1
				}
				"""));

		mockMvc.perform(get("/api/trades?status=ACTIVE"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"));
	}

	@Test
	@DisplayName("Trade 목록 조회 - 페이징")
	void getTrades_withPaging() throws Exception {
		for (int i = 50; i < 55; i++) {
			mockMvc.perform(post("/api/trades")
				.contentType(MediaType.APPLICATION_JSON)
				.content(String.format("""
					{
						"ticketId": %d,
						"type": "EXCHANGE",
						"price": null,
						"totalCount": 1
					}
					""", i)));
		}

		mockMvc.perform(get("/api/trades?size=3"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(3))
			.andExpect(jsonPath("$.data.size").value(3))
			.andExpect(jsonPath("$.data.totalElements").exists());
	}
}
