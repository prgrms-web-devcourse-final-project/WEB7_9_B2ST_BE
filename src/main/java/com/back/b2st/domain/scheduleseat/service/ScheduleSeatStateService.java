package com.back.b2st.domain.scheduleseat.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.error.ScheduleSeatErrorCode;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduleSeatStateService {

	private final ScheduleSeatLockService scheduleSeatLockService;
	private final SeatHoldTokenService seatHoldTokenService;

	private final ScheduleSeatRepository scheduleSeatRepository;

	/** === 상태 변경 AVAILABLE → HOLD === */
	@Transactional
	public void holdSeat(Long memberId, Long scheduleId, Long seatId) {

		// 1. 좌석 락 획득
		String lockValue = scheduleSeatLockService.tryLock(scheduleId, seatId, memberId);
		if (lockValue == null) {
			throw new BusinessException(ScheduleSeatErrorCode.SEAT_LOCK_FAILED);
		}

		try {
			// 2. AVAILABLE → HOLD (순수 도메인 상태 전이)
			changeToHold(scheduleId, seatId);

			// 3. HOLD 소유권 저장 (Redis TTL)
			seatHoldTokenService.save(scheduleId, seatId, memberId);

		} finally {
			// 4. HOLD 확정 후 즉시 락 해제
			scheduleSeatLockService.unlock(scheduleId, seatId, lockValue);
		}
	}

	/** === 만료된 HOLD 좌석을 AVAILABLE로 일괄 복구 === */
	@Transactional
	public int releaseExpiredHolds() {
		LocalDateTime now = LocalDateTime.now();
		return scheduleSeatRepository.releaseExpiredHolds(SeatStatus.HOLD, SeatStatus.AVAILABLE, now);
	}

	// === 상태 변경 AVAILABLE → HOLD === //
	@Transactional
	public void changeToHold(Long scheduleId, Long seatId) {
		ScheduleSeat seat = getScheduleSeat(scheduleId, seatId);

		if (seat.getStatus() == SeatStatus.SOLD) {
			throw new BusinessException(ScheduleSeatErrorCode.SEAT_ALREADY_SOLD);
		}
		if (seat.getStatus() == SeatStatus.HOLD) {
			throw new BusinessException(ScheduleSeatErrorCode.SEAT_ALREADY_HOLD);
		}

		LocalDateTime expiredAt = LocalDateTime.now().plus(SeatHoldTokenService.HOLD_TTL);

		seat.hold(expiredAt);
	}

	// === 상태 변경 HOLD → AVAILABLE === //
	@Transactional
	public void changeToAvailable(Long scheduleId, Long seatId) {
		ScheduleSeat seat = getScheduleSeat(scheduleId, seatId);

		if (seat.getStatus() == SeatStatus.AVAILABLE) {
			return;
		}

		if (seat.getStatus() != SeatStatus.HOLD) {
			return;
		}

		seat.release();
	}

	// === 상태 변경 HOLD → SOLD === //
	@Transactional
	public void changeToSold(Long scheduleId, Long seatId) {
		ScheduleSeat seat = getScheduleSeat(scheduleId, seatId);

		if (seat.getStatus() == SeatStatus.SOLD) {
			return;
		}

		if (seat.getStatus() != SeatStatus.HOLD) {
			throw new BusinessException(ScheduleSeatErrorCode.SEAT_NOT_HOLD);
		}

		seat.sold();
	}

	// === 좌석 조회 공통 로직 === //
	private ScheduleSeat getScheduleSeat(Long scheduleId, Long seatId) {
		return scheduleSeatRepository
			.findByScheduleIdAndSeatId(scheduleId, seatId)
			.orElseThrow(() -> new BusinessException(ScheduleSeatErrorCode.SEAT_NOT_FOUND));
	}
}
