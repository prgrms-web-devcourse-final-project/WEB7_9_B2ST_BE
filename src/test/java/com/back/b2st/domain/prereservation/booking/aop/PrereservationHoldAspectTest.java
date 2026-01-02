package com.back.b2st.domain.prereservation.booking.aop;

import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.prereservation.booking.service.PrereservationHoldService;

/**
 * [DEPRECATED - AOP 제거됨]
 *
 * PrereservationHoldAspect가 비활성화되어 이 테스트는 더 이상 의미가 없습니다.
 * 신청예매 검증은 PrereservationBookingController에서 직접 수행됩니다.
 *
 * @see com.back.b2st.domain.prereservation.booking.controller.PrereservationBookingController#holdSeat
 */
@Deprecated
@ExtendWith(MockitoExtension.class)
class PrereservationHoldAspectTest {

	@Mock
	private PrereservationHoldService prereservationHoldService;

	@InjectMocks
	private PrereservationHoldAspect prereservationHoldAspect;

	@Test
	@Disabled("AOP 제거로 인해 비활성화됨. PrereservationBookingController에서 직접 검증 수행")
	@DisplayName("validatePrereservationHoldAllowed(): HOLD 검증을 서비스에 위임한다")
	void validatePrereservationHoldAllowed_delegates() {
		// given
		Long memberId = 1L;
		Long scheduleId = 2L;
		Long seatId = 3L;

		// when
		prereservationHoldAspect.validatePrereservationHoldAllowed(memberId, scheduleId, seatId);

		// then
		then(prereservationHoldService).should().validateSeatHoldAllowed(memberId, scheduleId, seatId);
	}
}

