package com.back.b2st.domain.venue.section.service;

import org.springframework.stereotype.Service;

import com.back.b2st.domain.venue.section.dto.request.CreateSectionReq;
import com.back.b2st.domain.venue.section.dto.response.CreateSectionRes;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.error.SectionErrorCode;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.global.error.exception.BusinessException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SectionService {

	private final SectionRepository sectionRepository;

	public CreateSectionRes createSectionInfo(Long venueId, @Valid CreateSectionReq request) {
		validateVenueId(venueId);
		validateSectionNotDuplicated(venueId, request.sectionName());
		Section section = Section.builder()
			.venueId(venueId)
			.sectionName(request.sectionName())
			.build();

		return new CreateSectionRes(sectionRepository.save(section));
	}

	private void validateSectionNotDuplicated(Long venueId, String sectionName) {
		if (sectionRepository.existsByVenueIdAndSectionName(venueId, sectionName)) {
			throw new BusinessException(SectionErrorCode.DUPLICATE_SECTION);
		}
		;
	}

	private void validateVenueId(Long venueId) {
		// todo: check venueId
	}
}
