package com.back.b2st.domain.prereservation.booking.aop;

import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.prereservation.booking.service.PrereservationHoldService;

@ExtendWith(MockitoExtension.class)
class PrereservationHoldAspectTest {

	@Mock
	private PrereservationHoldService prereservationHoldService;

	@InjectMocks
	private PrereservationHoldAspect prereservationHoldAspect;

	@Test
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

