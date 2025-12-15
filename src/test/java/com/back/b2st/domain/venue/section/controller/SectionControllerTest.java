package com.back.b2st.domain.venue.section.controller;

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

import com.back.b2st.domain.venue.section.error.SectionErrorCode;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@ActiveProfiles("test")
class SectionControllerTest {

	@Autowired
	private MockMvc mvc;

	private String createUrl = "/api/admin/venues/{venueId}/sections";

	@Test
	@DisplayName("구역생성_성공")
	void createSection_success() throws Exception {
		// given
		Long param = 1L;

		String sectionName = "A";

		String requestBody = "{"
			+ "\"sectionName\": \"" + sectionName + "\""
			+ "}";

		// when & then
		mvc.perform(
				post(createUrl, param)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody)
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.venueId").value(param))
			.andExpect(jsonPath("$.data.sectionName").value(sectionName))
		;
	}

	@Test
	@DisplayName("구역생성_실패_공연장")
	void createSection_fail_venue() throws Exception {
		// todo : test - 구역생성_실패_공연장
	}

	@Test
	@DisplayName("구역생성_성공")
	void createSection_fail_duplicate() throws Exception {
		// given
		Long param = 1L;

		String sectionName = "A";

		String requestBody = "{"
			+ "\"sectionName\": \"" + sectionName + "\""
			+ "}";

		// when & then
		mvc.perform(
				post(createUrl, param)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody)
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.venueId").value(param))
			.andExpect(jsonPath("$.data.sectionName").value(sectionName))
		;

		mvc.perform(
				post(createUrl, param)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody)
			)
			.andDo(print())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value(409))
			.andExpect(jsonPath("$.message").value(SectionErrorCode.DUPLICATE_SECTION.getMessage()))
		;
	}
}