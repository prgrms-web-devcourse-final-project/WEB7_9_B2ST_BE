package com.back.b2st.domain.scheduleseat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;

public interface ScheduleSeatRepository extends JpaRepository<ScheduleSeat, Long> {

	/* 특정 회차 모든 좌석 조회 */
	List<ScheduleSeat> findByScheduleId(Long scheduleId);

	/* scheduleId + seatId 로 특정 좌석 조회 */
	Optional<ScheduleSeat> findByScheduleIdAndSeatId(Long scheduleId, Long seatId);

	/* 특정 회차에서 특정 상태의 좌석 조회 (예: AVAILABLE 좌석만) */
	List<ScheduleSeat> findByScheduleIdAndStatus(Long scheduleId, SeatStatus status);
}
