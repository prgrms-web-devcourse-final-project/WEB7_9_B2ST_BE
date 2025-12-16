package com.back.b2st.domain.reservation.service;

import org.springframework.stereotype.Service;

import com.back.b2st.domain.reservation.error.ReservationErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatSelectionService {

	private final SeatLockService seatLockService;
	private final SeatHoldService seatHoldService;
	private final SeatHoldTokenService seatHoldTokenService;

	/** === 락 + 좌석 HOLD 통합 로직 === */
	public void selectSeat(Long memberId, Long scheduleId, Long seatId) {

		// 1. 락 획득 시도
		String lockValue = seatLockService.tryLock(scheduleId, seatId, memberId);

		if (lockValue == null) {
			throw new BusinessException(ReservationErrorCode.SEAT_LOCK_FAILED);
		}

		try {
			// 2. 좌석 상태 변경 (AVAILABLE → HOLD)
			seatHoldService.holdSeat(scheduleId, seatId);

			// 3. Redis HOLD 소유권 저장
			seatHoldTokenService.save(scheduleId, seatId, memberId);

		} finally {
			// 4. 락 해제
			seatLockService.unlock(scheduleId, seatId, lockValue);
		}
	}
}
