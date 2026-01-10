package com.back.b2st.domain.reservation.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.reservation.dto.request.ConfirmAssignedSeatsReq;
import com.back.b2st.domain.reservation.dto.response.LotteryReservationCreatedRes;
import com.back.b2st.domain.reservation.service.LotteryReservationService;
import com.back.b2st.global.common.BaseResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/lottery/reservations")
@RequiredArgsConstructor
public class LotteryReservationController {

	private final LotteryReservationService lotteryReservationService;

	/** === 추첨 예매 확정 (결제 완료 기준) === */
	@PostMapping
	public BaseResponse<LotteryReservationCreatedRes> createCompletedReservation(
		@RequestParam Long memberId,
		@RequestParam Long scheduleId
	) {
		LotteryReservationCreatedRes reservation =
			lotteryReservationService.createCompletedReservation(memberId, scheduleId);

		return BaseResponse.created(reservation);
	}

	/** === 추첨 좌석 확정 (관리자/배치용) === */
	@PostMapping("/{reservationId}/seats/confirm")
	public BaseResponse<Void> confirmAssignedSeats(
		@PathVariable Long reservationId,
		@Valid @RequestBody ConfirmAssignedSeatsReq req
	) {
		lotteryReservationService.confirmAssignedSeats(
			reservationId, req.scheduleId(), req.scheduleSeatIds()
		);
		return BaseResponse.created(null);
	}

}
