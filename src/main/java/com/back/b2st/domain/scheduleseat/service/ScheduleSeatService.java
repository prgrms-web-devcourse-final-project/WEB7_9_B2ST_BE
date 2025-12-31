package com.back.b2st.domain.scheduleseat.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.performanceschedule.error.PerformanceScheduleErrorCode;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.scheduleseat.dto.response.ScheduleSeatViewRes;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.error.ScheduleSeatErrorCode;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduleSeatService {

	private final ScheduleSeatRepository scheduleSeatRepository;
	private final PerformanceScheduleRepository performanceScheduleRepository;

	/** === 특정 회차 전체 좌석 조회 === */
	@Transactional(readOnly = true)
	public List<ScheduleSeatViewRes> getSeats(Long scheduleId) {

		if (!performanceScheduleRepository.existsById(scheduleId)) {
			throw new BusinessException(PerformanceScheduleErrorCode.SCHEDULE_NOT_FOUND);
		}

		return scheduleSeatRepository.findSeats(scheduleId);
	}

	/** === 특정 회차에서 상태별 좌석 조회 === */
	@Transactional(readOnly = true)
	public List<ScheduleSeatViewRes> getSeatsByStatus(Long scheduleId, SeatStatus status) {

		if (!performanceScheduleRepository.existsById(scheduleId)) {
			throw new BusinessException(PerformanceScheduleErrorCode.SCHEDULE_NOT_FOUND);
		}

		return scheduleSeatRepository.findSeatsByStatus(scheduleId, status);
	}

	/** === Reservation 생성 직전에 DB 좌석이 유효한 HOLD 상태인지 검증 === */
	@Transactional(readOnly = true)
	public void validateHoldState(Long scheduleId, Long seatId) {
		ScheduleSeat seat = getScheduleSeat(scheduleId, seatId);

		if (seat.getStatus() != SeatStatus.HOLD) {
			throw new BusinessException(ScheduleSeatErrorCode.SEAT_NOT_HOLD);
		}

		LocalDateTime expiredAt = seat.getHoldExpiredAt();
		LocalDateTime now = LocalDateTime.now();

		if (expiredAt != null && expiredAt.isBefore(now)) {
			throw new BusinessException(ScheduleSeatErrorCode.SEAT_HOLD_EXPIRED);
		}
	}

	/** === Reservation expiresAt 동기화를 위한 holdExpiredAt 반환 === */
	@Transactional(readOnly = true)
	public LocalDateTime getHoldExpiredAtOrThrow(Long scheduleId, Long seatId) {
		ScheduleSeat seat = getScheduleSeat(scheduleId, seatId);

		if (seat.getStatus() != SeatStatus.HOLD) {
			throw new BusinessException(ScheduleSeatErrorCode.SEAT_NOT_HOLD);
		}

		LocalDateTime expiredAt = seat.getHoldExpiredAt();
		if (expiredAt == null) {
			throw new BusinessException(ScheduleSeatErrorCode.SEAT_HOLD_EXPIRED);
		}

		return expiredAt;
	}

	// === 좌석 조회 공통 로직 === //
	private ScheduleSeat getScheduleSeat(Long scheduleId, Long seatId) {
		return scheduleSeatRepository
			.findByScheduleIdAndSeatId(scheduleId, seatId)
			.orElseThrow(() -> new BusinessException(ScheduleSeatErrorCode.SEAT_NOT_FOUND));
	}

}
