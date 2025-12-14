package com.back.b2st.domain.venue.section.service;

import org.springframework.stereotype.Service;

import com.back.b2st.domain.venue.section.dto.request.CreateSectionReq;
import com.back.b2st.domain.venue.section.dto.response.CreateSectionRes;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SectionService {

	private final SectionRepository sectionRepository;

	public CreateSectionRes createSectionInfo(Long venueId, @Valid CreateSectionReq request) {
		validateVenueId(venueId);
		Section section = Section.builder()
			.venueId(venueId)
			.sectionName(request.sectionName())
			.build();

		return new CreateSectionRes(sectionRepository.save(section));
	}

	private void validateVenueId(Long venueId) {
		// todo: check venueId
	}
}
