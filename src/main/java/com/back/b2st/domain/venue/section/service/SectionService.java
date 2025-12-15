package com.back.b2st.domain.venue.section.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.back.b2st.domain.venue.section.dto.request.CreateSectionReq;
import com.back.b2st.domain.venue.section.dto.response.CreateSectionRes;
import com.back.b2st.domain.venue.section.dto.response.SectionInfoRes;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.error.SectionErrorCode;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.domain.venue.venue.repository.VenueRepository;
import com.back.b2st.global.error.exception.BusinessException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SectionService {

	private final SectionRepository sectionRepository;
	private final VenueRepository venueRepository;

	// 구역 정보 생성
	public CreateSectionRes createSectionInfo(Long venueId, @Valid CreateSectionReq request) {
		validateVenueId(venueId);
		validateSectionNotDuplicated(venueId, request.sectionName());
		Section section = Section.builder()
			.venueId(venueId)
			.sectionName(request.sectionName())
			.build();

		return CreateSectionRes.form(sectionRepository.save(section));
	}

	// 기등록 데이터 검증
	private void validateSectionNotDuplicated(Long venueId, String sectionName) {
		if (sectionRepository.existsByVenueIdAndSectionName(venueId, sectionName)) {
			throw new BusinessException(SectionErrorCode.DUPLICATE_SECTION);
		}
	}

	// 공연장 검증
	private void validateVenueId(Long venueId) {
		if (!venueRepository.existsById(venueId)) {
			throw new BusinessException(SectionErrorCode.INVALID_VENUE_INFO);
		}
	}

	// 구역 정보 조회 - 구역 Id
	public Section getSection(Long sectionId) {
		return sectionRepository.findById(sectionId)
			.orElseThrow(() -> new BusinessException(SectionErrorCode.SECTION_NOT_FOUND));
	}

	// 구역 정보 조회 - 공연장 Id
	public SectionInfoRes getSectionByVenueId(Long venueId) {
		validateVenueId(venueId);
		List<Section> sections = sectionRepository.findByVenueId(venueId);

		return SectionInfoRes.from(venueId, sections);
	}
}
