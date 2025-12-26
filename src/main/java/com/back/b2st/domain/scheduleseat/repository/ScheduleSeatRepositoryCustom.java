package com.back.b2st.domain.scheduleseat.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.back.b2st.domain.scheduleseat.dto.response.ScheduleSeatViewRes;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;

@Repository
public interface ScheduleSeatRepositoryCustom {

	/* 특정 회차 모든 좌석 조회 */
	List<ScheduleSeatViewRes> findSeats(Long scheduleId);

	/* 특정 회차 특정 상태 좌석 조회 */
	List<ScheduleSeatViewRes> findSeatsByStatus(Long scheduleId, SeatStatus status);
}
