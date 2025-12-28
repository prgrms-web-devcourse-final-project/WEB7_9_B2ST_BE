package com.back.b2st.domain.seatapplication.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.seatapplication.dto.request.SeatSectionApplicationCreateReq;
import com.back.b2st.domain.seatapplication.dto.response.SeatSectionApplicationRes;
import com.back.b2st.domain.seatapplication.service.SeatSectionApplicationService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules/{scheduleId}/applications")
public class SeatSectionApplicationController {

	private final SeatSectionApplicationService seatSectionApplicationService;

	@PostMapping
	public BaseResponse<Void> apply(
		@PathVariable Long scheduleId,
		@CurrentUser UserPrincipal user,
		@Valid @RequestBody SeatSectionApplicationCreateReq request
	) {
		seatSectionApplicationService.apply(scheduleId, user.getId(), request.sectionId());
		return BaseResponse.created(null);
	}

	@GetMapping("/me")
	public BaseResponse<SeatSectionApplicationRes> getMyApplications(
		@PathVariable Long scheduleId,
		@CurrentUser UserPrincipal user
	) {
		return BaseResponse.success(seatSectionApplicationService.getMyApplications(scheduleId, user.getId()));
	}
}
