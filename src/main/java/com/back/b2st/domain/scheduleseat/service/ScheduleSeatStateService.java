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

	/** === 좌석 잡기 (HOLD) === */
	@Transactional
	public void holdSeat(Long memberId, Long scheduleId, Long seatId) {

		// prereservationService.validateSeatHoldAllowed(memberId, scheduleId, seatId);

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
	public int releaseExpiredHoldsBatch() {
		LocalDateTime now = LocalDateTime.now();

		var expiredKeys = scheduleSeatRepository.findExpiredHoldKeys(SeatStatus.HOLD, now);

		int updated = scheduleSeatRepository.releaseExpiredHolds(SeatStatus.HOLD, SeatStatus.AVAILABLE, now);

		for (Object[] row : expiredKeys) {
			Long scheduleId = (Long)row[0];
			Long seatId = (Long)row[1];
			seatHoldTokenService.remove(scheduleId, seatId);
		}

		return updated;
	}

	@Transactional
	public void releaseHold(Long scheduleId, Long seatId) {
		changeToAvailable(scheduleId, seatId);
		seatHoldTokenService.remove(scheduleId, seatId);
	}

	@Transactional
	public void confirmHold(Long scheduleId, Long seatId) {
		changeToSold(scheduleId, seatId);
		seatHoldTokenService.remove(scheduleId, seatId);
	}

	// === 상태 변경 AVAILABLE → HOLD === //
	@Transactional
	public void changeToHold(Long scheduleId, Long seatId) {
		ScheduleSeat seat = getScheduleSeatWithLock(scheduleId, seatId);

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
		ScheduleSeat seat = getScheduleSeatWithLock(scheduleId, seatId);

		if (seat.getStatus() == SeatStatus.AVAILABLE) {
			return;
		}

		if (seat.getStatus() != SeatStatus.HOLD) {
			return;
		}

		seat.release();
	}

	@Transactional
	public void forceToAvailable(Long scheduleId, Long seatId) {
		ScheduleSeat seat = getScheduleSeatWithLock(scheduleId, seatId);

		if (seat.getStatus() == SeatStatus.AVAILABLE) {
			seatHoldTokenService.remove(scheduleId, seatId);
			return;
		}

		// SOLD든 HOLD든 운영 복구 목적으로 AVAILABLE로 강제
		seat.release();

		seatHoldTokenService.remove(scheduleId, seatId);
	}

	// === 상태 변경 HOLD → SOLD === //
	@Transactional
	public void changeToSold(Long scheduleId, Long seatId) {
		ScheduleSeat seat = getScheduleSeatWithLock(scheduleId, seatId);

		if (seat.getStatus() == SeatStatus.SOLD) {
			return;
		}

		if (seat.getStatus() != SeatStatus.HOLD) {
			throw new BusinessException(ScheduleSeatErrorCode.SEAT_NOT_HOLD);
		}

		seat.sold();
	}

	// === 좌석 조회 공통 로직 (락) === //
	private ScheduleSeat getScheduleSeatWithLock(Long scheduleId, Long seatId) {
		return scheduleSeatRepository
			.findByScheduleIdAndSeatIdWithLock(scheduleId, seatId)
			.orElseThrow(() -> new BusinessException(ScheduleSeatErrorCode.SEAT_NOT_FOUND));
	}
}
