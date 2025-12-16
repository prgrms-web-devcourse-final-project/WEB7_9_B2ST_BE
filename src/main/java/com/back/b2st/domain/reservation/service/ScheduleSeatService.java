package com.back.b2st.domain.reservation.service;

import static com.back.b2st.domain.reservation.error.ReservationErrorCode.*;

import org.springframework.stereotype.Service;

import com.back.b2st.domain.reservation.entity.ScheduleSeat;
import com.back.b2st.domain.reservation.repository.ScheduleSeatRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduleSeatService {

	private final ScheduleSeatRepository scheduleSeatRepository;

	// === HOLD 상태인지 확인 ===
	public void getHoldSeatOrThrow(Long scheduleId, Long seatId) {
		ScheduleSeat seat = scheduleSeatRepository
			.findByScheduleIdAndSeatId(scheduleId, seatId)
			.orElseThrow(() -> new BusinessException(SEAT_NOT_FOUND));

		if (!seat.isHold()) {
			throw new BusinessException(SEAT_NOT_HOLD);
		}
	}
}