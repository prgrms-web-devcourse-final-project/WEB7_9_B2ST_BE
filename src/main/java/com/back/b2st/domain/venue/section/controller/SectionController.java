package com.back.b2st.domain.venue.section.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.venue.section.dto.request.CreateSectionReq;
import com.back.b2st.domain.venue.section.dto.response.CreateSectionRes;
import com.back.b2st.domain.venue.section.service.SectionService;
import com.back.b2st.global.common.BaseResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SectionController {

	private final SectionService sectionService;

	@PostMapping("/admin/venue/{venueId}")
	public BaseResponse<CreateSectionRes> createSection(
		@PathVariable("venueId") Long venueId,
		@Valid @RequestBody CreateSectionReq request
	) {
		return BaseResponse.created(sectionService.createSectionInfo(venueId, request));
	}
}
