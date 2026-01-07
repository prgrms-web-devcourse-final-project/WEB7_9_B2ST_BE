package com.back.b2st.domain.scheduleseat.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.scheduleseat.service.ScheduleSeatStateService;
import com.back.b2st.global.common.BaseResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test/schedules")
public class ScheduleSeatTestController {

	private final ScheduleSeatStateService scheduleSeatStateService;

	/** === 만료된 HOLD 좌석 일괄 해제 (수동) === */
	@PostMapping("/expired/release")
	public BaseResponse<Integer> releaseExpiredHolds() {
		int updated = scheduleSeatStateService.releaseExpiredHoldsBatch();
		return BaseResponse.success(updated);
	}

	/** === 특정 좌석을 강제로 AVAILABLE로 복구 === */
	@PostMapping("/{scheduleId}/seats/{seatId}/release")
	public BaseResponse<Void> forceReleaseSeat(
		@PathVariable Long scheduleId,
		@PathVariable Long seatId
	) {
		scheduleSeatStateService.changeToAvailable(scheduleId, seatId);
		return BaseResponse.success();
	}

}