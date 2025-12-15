package com.back.b2st.domain.venue.seat.seat.controller;

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

import com.back.b2st.domain.venue.seat.seat.error.SeatErrorCode;
import com.back.b2st.domain.venue.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@ActiveProfiles("test")
class SeatControllerTest {

	@Autowired
	private MockMvc mvc;
	@Autowired
	private SeatRepository seatRepository;
	@Autowired
	private SectionRepository sectionRepository;
	@Autowired
	EntityManager em;

	private Section section1A;
	private Section section1B;
	private Section section2A;
	private Section section;
	private String createUrl = "/api/admin/venues/{venueId}/seats";

	@BeforeEach
	void setUp() {
		section1A = Section.builder()
			.venueId(1L)
			.sectionName("A")
			.build();

		section1B = Section.builder()
			.venueId(1L)
			.sectionName("B")
			.build();

		section2A = Section.builder()
			.venueId(2L)
			.sectionName("A")
			.build();

		section = sectionRepository.save(section1A);
		sectionRepository.save(section1B);
		sectionRepository.save(section2A);

		em.flush();
		em.clear();
	}

	@Test
	@DisplayName("좌석생성_성공")
	void createSeat_success() throws Exception {
		// given
		Long param = 1L;

		String rowLabel = "2";
		Long sectionId = section.getId();
		int seatNumber = 7;

		String requestBody = "{"
			+ "\"sectionId\": " + sectionId + ","
			+ "\"rowLabel\": \"" + rowLabel + "\","
			+ "\"seatNumber\": " + seatNumber
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
			.andExpect(jsonPath("$.data.sectionId").value(sectionId))
			.andExpect(jsonPath("$.data.sectionId").value(sectionId))
			.andExpect(jsonPath("$.data.sectionName").value(section.getSectionName()))
			.andExpect(jsonPath("$.data.seatNumber").value(seatNumber))
		;
	}

	@Test
	@DisplayName("좌석생성_실패_공연장")
	void createSeat_fail_venud() throws Exception {
		// todo : venue Repo
	}

	@Test
	@DisplayName("좌석생성_실패_구역")
	void createSeat_fail_section() throws Exception {
		// given
		Long param = 1L;

		String rowLabel = "2";
		Long sectionId = 999L;
		int seatNumber = 7;

		String requestBody = "{"
			+ "\"sectionId\": " + sectionId + ","
			+ "\"rowLabel\": \"" + rowLabel + "\","
			+ "\"seatNumber\": " + seatNumber
			+ "}";

		// when & then
		mvc.perform(
				post(createUrl, param)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody)
			)
			.andDo(print())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value(SeatErrorCode.SECTION_NOT_FOUND.getMessage()))
		;
	}

}