package com.back.b2st.domain.scheduleseat.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.performanceschedule.error.PerformanceScheduleErrorCode;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.scheduleseat.dto.response.ScheduleSeatViewRes;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
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

}
