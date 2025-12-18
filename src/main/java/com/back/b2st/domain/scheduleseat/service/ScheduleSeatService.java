package com.back.b2st.domain.scheduleseat.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.scheduleseat.dto.response.ScheduleSeatViewRes;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduleSeatService {

	private final ScheduleSeatRepository scheduleSeatRepository;

	/** === 특정 회차 전체 좌석 조회 === */
	@Transactional(readOnly = true)
	public List<ScheduleSeatViewRes> getSeats(Long scheduleId) {
		return scheduleSeatRepository.findSeats(scheduleId);
	}

	/** === 특정 회차에서 상태별 좌석 조회 === */
	@Transactional(readOnly = true)
	public List<ScheduleSeatViewRes> getSeatsByStatus(Long scheduleId, SeatStatus status) {
		return scheduleSeatRepository.findSeatsByStatus(scheduleId, status);
	}

}
