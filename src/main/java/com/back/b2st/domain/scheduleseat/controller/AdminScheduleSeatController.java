package com.back.b2st.domain.scheduleseat.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.scheduleseat.dto.response.ScheduleSeatViewRes;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.service.AdminScheduleSeatService;
import com.back.b2st.global.common.BaseResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/schedules/{scheduleId}/seats")
@RequiredArgsConstructor
public class AdminScheduleSeatController implements AdminScheduleSeatApi {

	private final AdminScheduleSeatService adminScheduleSeatService;

	@GetMapping
	public BaseResponse<List<ScheduleSeatViewRes>> getScheduleSeats(
		@PathVariable Long scheduleId,
		@RequestParam(required = false) SeatStatus status
	) {
		if (status == null) {
			return BaseResponse.success(adminScheduleSeatService.getSeats(scheduleId));
		}
		return BaseResponse.success(
			adminScheduleSeatService.getSeatsByStatus(scheduleId, status)
		);
	}

	@PostMapping("/{seatId}/release-hold")
	public BaseResponse<Void> releaseHold(
		@PathVariable Long scheduleId,
		@PathVariable Long seatId
	) {
		adminScheduleSeatService.releaseHold(scheduleId, seatId);
		return BaseResponse.created(null);
	}
}
