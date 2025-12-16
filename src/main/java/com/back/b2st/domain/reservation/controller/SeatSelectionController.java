package com.back.b2st.domain.reservation.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.reservation.service.SeatSelectionService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
public class SeatSelectionController {

	private final SeatSelectionService seatSelectionService;

	@PostMapping("/{scheduleId}/seats/{seatId}/hold")
	public BaseResponse<Void> holdSeat(
		@CurrentUser UserPrincipal user,
		@PathVariable Long scheduleId,
		@PathVariable Long seatId
	) {
		seatSelectionService.selectSeat(
			user.getId(),
			scheduleId,
			seatId
		);
		return BaseResponse.success(null);
	}

}