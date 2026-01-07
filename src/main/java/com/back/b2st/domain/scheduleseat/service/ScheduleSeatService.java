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
	private final SeatHoldTokenService seatHoldTokenService;

	/** === 좌석 상태 유효한지 검사 === */
	@Transactional(readOnly = true)
	public ScheduleSeat validateAndGetAttachableSeat(
		Long scheduleId,
		Long seatId,
		Long memberId
	) {
		// 1. HOLD 소유권 (Redis)
		seatHoldTokenService.validateOwnership(scheduleId, seatId, memberId);

		// 2. ScheduleSeat 조회
		ScheduleSeat scheduleSeat =
			scheduleSeatRepository.findByScheduleIdAndSeatId(scheduleId, seatId)
				.orElseThrow(() -> new BusinessException(ScheduleSeatErrorCode.SEAT_NOT_FOUND));

		if (scheduleSeat.getStatus() != SeatStatus.HOLD) {
			throw new BusinessException(ScheduleSeatErrorCode.SEAT_NOT_HOLD);
		}

		// 3. 만료 시간 조회 및 null 체크
		LocalDateTime expiredAt = scheduleSeat.getHoldExpiredAt();
		if (expiredAt == null || expiredAt.isBefore(LocalDateTime.now())) {
			throw new BusinessException(ScheduleSeatErrorCode.SEAT_HOLD_EXPIRED);
		}

		return scheduleSeat;
	}

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

}
