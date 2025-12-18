package com.back.b2st.domain.venue.section.controller;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.venue.section.error.SectionErrorCode;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.domain.venue.venue.repository.VenueRepository;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@ActiveProfiles("test")
class SectionControllerTest {

	@Autowired
	private MockMvc mvc;
	@Autowired
	private VenueRepository venueRepository;

	private String createUrl = "/api/admin/venues/{venueId}/sections";
	private Venue venue1;
	private Venue venue2;

	@BeforeEach
	void setUp() {
		venue1 = venueRepository.save(new Venue("공연장1"));
		venue2 = venueRepository.save(new Venue("공연장2"));
	}

	@Test
	@DisplayName("구역생성_성공")
	void createSection_success() throws Exception {
		// given
		Long param = venue1.getVenueId();

		String sectionName = "Z";

		String requestBody = "{" + "\"sectionName\": \"" + sectionName + "\"" + "}";

		// when & then
		mvc.perform(post(createUrl, param).contentType(MediaType.APPLICATION_JSON).content(requestBody))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.venueId").value(param))
			.andExpect(jsonPath("$.data.sectionName").value(sectionName));
	}

	@Test
	@DisplayName("구역생성_실패_공연장")
	void createSection_fail_venue() throws Exception {
		// given
		Long param = 999L;

		String sectionName = "Z";

		String requestBody = "{" + "\"sectionName\": \"" + sectionName + "\"" + "}";

		// when & then
		mvc.perform(post(createUrl, param).contentType(MediaType.APPLICATION_JSON).content(requestBody))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value(SectionErrorCode.INVALID_VENUE_INFO.getMessage()));
	}

	@Test
	@DisplayName("구역생성_실패_중복")
	void createSection_fail_duplicate() throws Exception {
		// given
		Long param = venue1.getVenueId();

		String sectionName = "Z";

		String requestBody = "{" + "\"sectionName\": \"" + sectionName + "\"" + "}";

		// when & then
		mvc.perform(post(createUrl, param).contentType(MediaType.APPLICATION_JSON).content(requestBody))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.venueId").value(param))
			.andExpect(jsonPath("$.data.sectionName").value(sectionName));

		mvc.perform(post(createUrl, param).contentType(MediaType.APPLICATION_JSON).content(requestBody))
			.andDo(print())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value(SectionErrorCode.DUPLICATE_SECTION.getMessage()));
	}
}