package com.back.b2st.domain.reservation.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.reservation.service.SeatSelectionService;
import com.back.b2st.global.common.BaseResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/schedules/{scheduleId}/seats")
public class SeatSelectionController {

	private final SeatSelectionService seatSelectionService;

	@PostMapping("/{seatId}/hold")
	public BaseResponse<Void> holdSeat(
		@PathVariable Long scheduleId,
		@PathVariable Long seatId
	) {
		seatSelectionService.holdSeat(scheduleId, seatId);
		return BaseResponse.success();
	}

}