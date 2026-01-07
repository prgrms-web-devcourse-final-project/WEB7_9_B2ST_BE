package com.back.b2st.domain.venue.section.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.venue.section.dto.response.SectionInfoRes;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.error.SectionErrorCode;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.domain.venue.venue.repository.VenueRepository;
import com.back.b2st.global.error.exception.BusinessException;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class SectionServiceTest {

	@Autowired
	private SectionService sectionService;
	@Autowired
	private SectionRepository sectionRepository;
	@Autowired
	private VenueRepository venueRepository;

	private Venue venue1;
	private Venue venue2;
	private Section section1A;
	private Section section1B;
	private Section section2A;

	@BeforeEach
	void setUp() {
		venue1 = venueRepository.save(new Venue("공연장1"));
		venue2 = venueRepository.save(new Venue("공연장2"));

		section1A = Section.builder()
			.venueId(venue1.getVenueId())
			.sectionName("A")
			.build();

		section1B = Section.builder()
			.venueId(venue1.getVenueId())
			.sectionName("B")
			.build();

		section2A = Section.builder()
			.venueId(venue2.getVenueId())
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

	@Test
	@DisplayName("구역조회-성공-공연장")
	void getSectionNameByVenue() throws Exception {
		// given
		Section section = section1A;
		List<String> list = List.of("A", "B");

		// when
		SectionInfoRes findSection = sectionService.getSectionByVenueId(section.getVenueId());

		// then
		assertThat(findSection.venueId()).isEqualTo(section.getVenueId());
		assertThat(findSection.sections().containsAll(list));
	}

	@Test
	@DisplayName("구역조회_실패_공연장")
	void getSectionNameBySection_fail_venue() throws Exception {
		// given
		Long venuId = 999L;

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> sectionService.getSectionByVenueId(venuId));

		// then
		assertThat(e.getErrorCode()).isEqualTo(SectionErrorCode.INVALID_VENUE_INFO);
	}
}