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
import com.back.b2st.domain.scheduleseat.service.ScheduleSeatService;
import com.back.b2st.domain.scheduleseat.service.ScheduleSeatStateService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
public class ScheduleSeatController implements ScheduleSeatApi {

	private final ScheduleSeatService scheduleSeatService;
	private final ScheduleSeatStateService scheduleSeatStateService;

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

	@PostMapping("/{scheduleId}/seats/{seatId}/hold")
	public BaseResponse<Void> holdSeat(
		@CurrentUser UserPrincipal user,
		@PathVariable Long scheduleId,
		@PathVariable Long seatId
	) {
		scheduleSeatStateService.holdSeat(
			user.getId(),
			scheduleId,
			seatId
		);
		return BaseResponse.created(null);
	}
}
