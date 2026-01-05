package com.back.b2st.domain.prereservation.booking.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.prereservation.booking.dto.response.PrereservationBookingCreateRes;
import com.back.b2st.domain.prereservation.booking.service.PrereservationBookingService;
import com.back.b2st.domain.prereservation.booking.service.PrereservationHoldService;
import com.back.b2st.domain.scheduleseat.service.ScheduleSeatStateService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.global.error.code.CommonErrorCode;
import com.back.b2st.global.error.exception.BusinessException;
import com.back.b2st.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/prereservations/schedules/{scheduleId}/seats/{seatId}")
@Tag(name = "신청 예매", description = "신청 예매 전용 좌석 선택 및 예매 생성 API")
public class PrereservationBookingController {

	private final PrereservationBookingService prereservationBookingService;
	private final PrereservationHoldService prereservationHoldService;
	private final ScheduleSeatStateService scheduleSeatStateService;

	@PostMapping("/hold")
	@Operation(
		summary = "신청예매 좌석 선택 (HOLD)",
		description = """
			신청예매 전용 좌석 HOLD API입니다.
			- bookingType=PRERESERVE 회차만 가능
			- 신청한 구역인지 검증
			- 구역별 예매 시간대 검증
			- 예매 오픈 시간 검증
			- 검증 통과 후 좌석 HOLD
			"""
	)
	public BaseResponse<Void> holdSeat(
		@Parameter(hidden = true) @CurrentUser UserPrincipal user,
		@Parameter(description = "공연 회차 ID", example = "1")
		@PathVariable Long scheduleId,
		@Parameter(description = "좌석 ID", example = "101")
		@PathVariable Long seatId
	) {
		Long memberId = requireMemberId(user);
		// 1. 신청예매 검증 (구역, 시간대 등)
		prereservationHoldService.validateSeatHoldAllowed(memberId, scheduleId, seatId);

		// 2. 좌석 HOLD
		scheduleSeatStateService.holdSeat(memberId, scheduleId, seatId);

		return BaseResponse.created(null);
	}

	@PostMapping("/bookings")
	@Operation(
		summary = "신청예매 예매 생성(결제 시작)",
		description = """
			HOLD된 좌석으로 신청예매 전용 예매를 생성합니다.
			- bookingType=PRERESERVE 회차만 가능
			- 좌석 HOLD 소유권(memberId) 검증
			- 생성된 prereservationBookingId를 결제 domainId로 사용합니다(domainType=PRERESERVATION)
			"""
	)
	public BaseResponse<PrereservationBookingCreateRes> createBooking(
		@Parameter(hidden = true) @CurrentUser UserPrincipal user,
		@Parameter(description = "공연 회차 ID", example = "1")
		@PathVariable Long scheduleId,
		@Parameter(description = "좌석 ID", example = "101")
		@PathVariable Long seatId
	) {
		var booking = prereservationBookingService.createBooking(requireMemberId(user), scheduleId, seatId);
		return BaseResponse.created(new PrereservationBookingCreateRes(booking.getId(), booking.getExpiresAt()));
	}

	private Long requireMemberId(UserPrincipal user) {
		if (user == null) {
			throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
		}
		return user.getId();
	}
}
