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
}
