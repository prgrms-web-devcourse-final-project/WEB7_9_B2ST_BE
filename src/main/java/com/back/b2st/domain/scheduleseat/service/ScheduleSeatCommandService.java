package com.back.b2st.domain.scheduleseat.service;

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
public class ScheduleSeatCommandService {

	private final ScheduleSeatRepository scheduleSeatRepository;

	/** === 좌석 HOLD (Redis 없이 DB 기반) === */
	@Transactional
	public void holdSeat(Long scheduleId, Long seatId) {

		// 1. 좌석 조회
		ScheduleSeat seat = scheduleSeatRepository
			.findByScheduleIdAndSeatId(scheduleId, seatId)
			.orElseThrow(() -> new BusinessException(ScheduleSeatErrorCode.SEAT_NOT_FOUND));

		// 2. SOLD 좌석 확인
		if (seat.getStatus() == SeatStatus.SOLD) {
			throw new BusinessException(ScheduleSeatErrorCode.SEAT_ALREADY_SOLD);
		}

		// 3. HOLD 좌석 확인
		if (seat.getStatus() == SeatStatus.HOLD) {
			throw new BusinessException(ScheduleSeatErrorCode.SEAT_ALREADY_HOLD);
		}

		// 4. AVAILABLE → HOLD 변경
		seat.hold();
	}
}
