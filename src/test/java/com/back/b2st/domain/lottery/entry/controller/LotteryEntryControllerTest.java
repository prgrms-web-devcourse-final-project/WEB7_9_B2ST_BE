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

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@ActiveProfiles("test")
class LotteryEntryControllerTest {

	@Autowired
	private MockMvc mvc;

	@Test
	@DisplayName("좌석정보조회")
	void getSeatInfo_success() throws Exception {
		// given
		String url = "/performances/{performanceId}/lottery/section";
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

}