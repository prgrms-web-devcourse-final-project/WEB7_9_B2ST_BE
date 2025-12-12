package com.back.b2st.domain.lottery.entry.controller;

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

import com.back.b2st.domain.lottery.constants.LotteryConstants;
import com.back.b2st.domain.lottery.entry.error.LotteryEntryErrorCode;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@ActiveProfiles("test")
class LotteryEntryControllerTest {

	@Autowired
	private MockMvc mvc;

	@Test
	@DisplayName("좌석정보조회_성공")
	void getSeatInfo_success() throws Exception {
		// given
		String url = "/api/performances/{performanceId}/lottery/section";
		Long param = 1L;

		// when & then
		mvc.perform(
				get(url, param)
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.seatId").value("1"))
			.andExpect(jsonPath("$.data.sectionName").value("A"))
			.andExpect(jsonPath("$.data.rowLabel").value("8"))
			.andExpect(jsonPath("$.data.seatNumber").value("7"))
		;
	}

	@Test
	@DisplayName("추첨응모_성공")
	void registerLotteryEntry_success() throws Exception {
		// given
		String url = "/api/performances/{performanceId}/lottery/entry";
		Long param = 1L;

		Long memberId = 1L;
		Long scheduleId = 2L;
		Long seatGradeId = 3L;
		int quantity = 4;

		String requestBody = "{"
			+ "\"memberId\": " + memberId + ","
			+ "\"scheduleId\": " + scheduleId + ","
			+ "\"seatGradeId\": " + seatGradeId + ","
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
			.andExpect(jsonPath("$.data.id").value("1"))
			.andExpect(jsonPath("$.data.memberId").value(memberId))
			.andExpect(jsonPath("$.data.performanceId").value(param))
			.andExpect(jsonPath("$.data.scheduleId").value(scheduleId))
			.andExpect(jsonPath("$.data.seatGradeId").value(seatGradeId))
			.andExpect(jsonPath("$.data.quantity").value(quantity))
			.andExpect(jsonPath("$.data.status").value("APPLIED"))
		;
	}

	@Test
	@DisplayName("추첨응모_실패_공연")
	void registerLotteryEntry_fail_performance() throws Exception {
		// TODO: repo 연결 후 테스트 INVALID_PERFORMANCE_INFO
	}

	@Test
	@DisplayName("추첨응모_실패_응모자")
	void registerLotteryEntry_fail_member() throws Exception {
		// given
		String url = "/api/performances/{performanceId}/lottery/entry";
		Long param = 1L;

		Long memberId = 99L;
		Long scheduleId = 2L;
		Long seatGradeId = 3L;
		int quantity = 4;

		String requestBody = "{"
			+ "\"memberId\": " + memberId + ","
			+ "\"scheduleId\": " + scheduleId + ","
			+ "\"seatGradeId\": " + seatGradeId + ","
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
			.andExpect(jsonPath("$.code").value(400))
			.andExpect(jsonPath("$.message").value(LotteryEntryErrorCode.INVALID_MEMBER_INFO.getMessage()))
		;
	}

	@Test
	@DisplayName("추첨응모_실패_회차")
	void registerLotteryEntry_fail_schedule() throws Exception {
		// TODO : Repo 연결 후 테스트
	}

	@Test
	@DisplayName("추첨응모_실패_좌석등급")
	void registerLotteryEntry_fail_seatGrade() throws Exception {
		// TODO : Repo 연결 후 테스트
	}

	@Test
	@DisplayName("추첨응모_실패_인원수0")
	void registerLotteryEntry_fail_quantityZero() throws Exception {
		// given
		String url = "/api/performances/{performanceId}/lottery/entry";
		Long param = 1L;

		Long memberId = 1L;
		Long scheduleId = 2L;
		Long seatGradeId = 3L;
		int quantity = 0;

		String requestBody = "{"
			+ "\"memberId\": " + memberId + ","
			+ "\"scheduleId\": " + scheduleId + ","
			+ "\"seatGradeId\": " + seatGradeId + ","
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
			.andExpect(jsonPath("$.code").value(400))
			.andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
		;
	}

	@Test
	@DisplayName("추첨응모_실패_신청인원초과")
	void registerLotteryEntry_fail_quantity() throws Exception {
		// given
		String url = "/api/performances/{performanceId}/lottery/entry";
		Long param = 1L;

		Long memberId = 1L;
		Long scheduleId = 2L;
		Long seatGradeId = 3L;
		int quantity = LotteryConstants.MAX_LOTTERY_ENTRY_COUNT + 1;

		String requestBody = "{"
			+ "\"memberId\": " + memberId + ","
			+ "\"scheduleId\": " + scheduleId + ","
			+ "\"seatGradeId\": " + seatGradeId + ","
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
			.andExpect(jsonPath("$.code").value(400))
			.andExpect(jsonPath("$.message").value(LotteryEntryErrorCode.EXCEEDS_MAX_ALLOCATION.getMessage()))
		;
	}

	@Test
	@DisplayName("추첨응모_실패_중복응모")
	void registerLotteryEntry_fail_duplicate() throws Exception {
		// given
		String url = "/api/performances/{performanceId}/lottery/entry";
		Long param = 1L;

		Long memberId = 1L;
		Long scheduleId = 2L;
		Long seatGradeId = 3L;
		int quantity = 6;

		String requestBody = "{"
			+ "\"memberId\": " + memberId + ","
			+ "\"scheduleId\": " + scheduleId + ","
			+ "\"seatGradeId\": " + seatGradeId + ","
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
			.andExpect(jsonPath("$.code").value(400))
			.andExpect(jsonPath("$.message").value(LotteryEntryErrorCode.DUPLICATE_ENTRY.getMessage()))
		;
	}

}