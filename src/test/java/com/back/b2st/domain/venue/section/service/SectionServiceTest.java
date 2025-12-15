package com.back.b2st.domain.venue.section.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.error.SectionErrorCode;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.global.error.exception.BusinessException;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class SectionServiceTest {

	@Autowired
	private SectionService sectionService;
	@Autowired
	private SectionRepository sectionRepository;

	private Section section1A;
	private Section section1B;
	private Section section2A;

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

		sectionRepository.save(section1A);
		sectionRepository.save(section1B);
		sectionRepository.save(section2A);
	}

	@Test
	@DisplayName("구역id로구역조회")
	void getSectionNameBySection() throws Exception {
		// given
		Section section = section1A;

		// when
		Section findSection = sectionService.getSection(section.getId());

		// then
		assertThat(findSection.getId()).isEqualTo(section.getId());
		assertThat(findSection.getVenueId()).isEqualTo(section.getVenueId());
		assertThat(findSection.getSectionName()).isEqualTo(section.getSectionName());
	}

	@Test
	@DisplayName("구역조회_실패_id")
	void getSectionNameBySection_fail_sectionId() throws Exception {
		// given
		Long sectionId = 999L;

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> sectionService.getSection(sectionId));

		// then
		assertThat(e.getErrorCode()).isEqualTo(SectionErrorCode.SECTION_NOT_FOUND);
	}
}