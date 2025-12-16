package com.back.b2st.domain.reservation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.reservation.error.ReservationErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class SeatSelectionServiceV2Test {

	@Mock
	private SeatLockService seatLockService;

	@Mock
	private SeatHoldService seatHoldService;

	@Mock
	private SeatHoldTokenService seatHoldTokenService;

	@InjectMocks
	private SeatSelectionService seatSelectionService;

	@Test
	void 좌석_선점_성공() {
		// given
		Long memberId = 1L;
		Long scheduleId = 1001L;
		Long seatId = 55L;

		given(seatLockService.tryLock(scheduleId, seatId, memberId))
			.willReturn("lock-value");

		// when
		seatSelectionService.selectSeat(memberId, scheduleId, seatId);

		// then
		then(seatHoldService).should().holdSeat(scheduleId, seatId);
		then(seatHoldTokenService).should().save(scheduleId, seatId, memberId);
		then(seatLockService).should().unlock(eq(scheduleId), eq(seatId), any());
	}

	@Test
	void 락_획득_실패_시_예외() {
		// given
		given(seatLockService.tryLock(any(), any(), any()))
			.willReturn(null);

		// when & then
		assertThatThrownBy(() ->
			seatSelectionService.selectSeat(1L, 1001L, 55L)
		)
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(ReservationErrorCode.SEAT_LOCK_FAILED.getMessage());
	}
}
