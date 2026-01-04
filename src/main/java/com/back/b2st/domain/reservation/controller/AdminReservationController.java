package com.back.b2st.domain.reservation.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.reservation.dto.response.AdminReservationSummaryRes;
import com.back.b2st.domain.reservation.dto.response.ReservationDetailWithPaymentRes;
import com.back.b2st.domain.reservation.entity.ReservationStatus;
import com.back.b2st.domain.reservation.service.AdminReservationService;
import com.back.b2st.global.common.BaseResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reservations")
public class AdminReservationController {

	private final AdminReservationService adminReservationService;

	@GetMapping
	public BaseResponse<Page<AdminReservationSummaryRes>> getList(
		@RequestParam ReservationStatus status,
		@RequestParam(required = false) Long scheduleId,
		@RequestParam(required = false) Long memberId,
		@RequestParam(defaultValue = "0") int page
	) {
		Pageable pageable = PageRequest.of(page, 20, Sort.by("createdAt").descending());

		return BaseResponse.success(
			adminReservationService.getReservationsByStatus(status, scheduleId, memberId, pageable)
		);
	}

	@GetMapping("/{reservationId}")
	public BaseResponse<ReservationDetailWithPaymentRes> getDetail(@PathVariable Long reservationId) {
		return BaseResponse.success(
			adminReservationService.getReservationDetail(reservationId));
	}

	@PostMapping("/{reservationId}/cancel")
	public BaseResponse<Void> cancel(@PathVariable Long reservationId) {
		adminReservationService.forceCancel(reservationId);
		return BaseResponse.success();
	}
}
