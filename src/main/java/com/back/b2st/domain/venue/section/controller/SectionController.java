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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Section", description = "구역 API")
public class SectionController {

	private final SectionService sectionService;

	// 공연장 구역 정보 생성
	@Operation(summary = "공연장 구역 정보 생성", description = "공연장의 구역 정보를 등록")
	@PostMapping("/admin/venues/{venueId}/sections")
	public BaseResponse<CreateSectionRes> createSection(
		@PathVariable("venueId") Long venueId,
		@Valid @RequestBody CreateSectionReq request
	) {
		return BaseResponse.created(sectionService.createSectionInfo(venueId, request));
	}
}
