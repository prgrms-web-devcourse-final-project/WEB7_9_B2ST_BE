package com.back.b2st.domain.scheduleseat.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.scheduleseat.dto.response.ScheduleSeatViewRes;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminScheduleSeatService {

	private final ScheduleSeatService scheduleSeatService;
	private final ScheduleSeatStateService seatStateService;

	@Transactional(readOnly = true)
	public List<ScheduleSeatViewRes> getSeats(Long scheduleId) {
		return scheduleSeatService.getSeats(scheduleId);
	}

	@Transactional(readOnly = true)
	public List<ScheduleSeatViewRes> getSeatsByStatus(Long scheduleId, SeatStatus status) {
		return scheduleSeatService.getSeatsByStatus(scheduleId, status);
	}

	@Transactional
	public void releaseHold(Long scheduleId, Long seatId) {
		seatStateService.releaseHold(scheduleId, seatId);
	}
}
