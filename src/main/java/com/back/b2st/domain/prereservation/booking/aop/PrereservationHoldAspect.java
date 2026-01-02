package com.back.b2st.domain.prereservation.booking.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.back.b2st.domain.prereservation.booking.service.PrereservationHoldService;

import lombok.RequiredArgsConstructor;

/**
 * [DEPRECATED - AOP 제거됨]
 *
 * 기존 문제점:
 * - 모든 BookingType(FIRST_COME, LOTTERY, PRERESERVE)에서 AOP가 실행됨
 * - 일반예매/추첨에서도 불필요한 DB 조회 및 타입 체크 발생
 * - 예매 타입별 책임 분리가 명확하지 않음
 *
 * 개선:
 * - PrereservationBookingController.holdSeat()에서 직접 검증 호출
 * - ScheduleSeatController.holdSeat()는 일반예매/추첨 전용 (AOP 영향 없음)
 * - 각 예매 타입별로 명확하게 분리
 *
 * @deprecated AOP 대신 PrereservationBookingController에서 직접 검증 호출
 */
@Deprecated
@Aspect
// @Component  // AOP 비활성화: 더 이상 빈으로 등록하지 않음
@Order(0)
@RequiredArgsConstructor
public class PrereservationHoldAspect {

	private final PrereservationHoldService prereservationHoldService;

	@Before(
		value = "execution(* com.back.b2st.domain.scheduleseat.service.ScheduleSeatStateService.holdSeat(..))"
			+ " && args(memberId, scheduleId, seatId)",
		argNames = "memberId,scheduleId,seatId"
	)
	public void validatePrereservationHoldAllowed(Long memberId, Long scheduleId, Long seatId) {
		prereservationHoldService.validateSeatHoldAllowed(memberId, scheduleId, seatId);
	}
}
