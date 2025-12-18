package com.back.b2st.domain.scheduleseat.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.scheduleseat.dto.response.ScheduleSeatViewRes;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.service.ScheduleSeatService;
import com.back.b2st.global.common.BaseResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
public class ScheduleSeatController {

	private final ScheduleSeatService scheduleSeatService;

	@GetMapping("/{scheduleId}/seats")
	public BaseResponse<List<ScheduleSeatViewRes>> getScheduleSeats(
		@PathVariable Long scheduleId,
		@RequestParam(required = false) SeatStatus status
	) {
		if (status == null) {
			return BaseResponse.success(scheduleSeatService.getSeats(scheduleId));
		}

		return BaseResponse.success(scheduleSeatService.getSeatsByStatus(scheduleId, status));
	}

	@GetMapping("/ticketing/schedules/{scheduleId}/seats")
	public BaseResponse<List<ScheduleSeatViewRes>> getAvailableSeatsForTicketing(
		@PathVariable Long scheduleId
	) {
		return BaseResponse.success(scheduleSeatService.getSeatsByStatus(
				scheduleId,
				SeatStatus.AVAILABLE
			)
		);
	}
}
