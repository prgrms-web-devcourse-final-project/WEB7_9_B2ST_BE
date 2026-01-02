package com.back.b2st.domain.prereservation.booking.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.back.b2st.domain.prereservation.booking.service.PrereservationHoldService;

import lombok.RequiredArgsConstructor;

@Deprecated
@Aspect
// @Component
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
