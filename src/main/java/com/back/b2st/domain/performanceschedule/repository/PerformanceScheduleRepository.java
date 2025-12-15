package com.back.b2st.domain.performanceschedule.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.b2st.domain.performanceSchedule.entity.PerformanceSchedule;

public interface PerformanceScheduleRepository extends JpaRepository<PerformanceSchedule, Long> {

	/**
	 * 공연 ID 기준 회차 목록 조회 (시간 순 정렬)
	 */
	List<PerformanceSchedule> findAllByPerformance_PerformanceIdOrderByStartAtAsc(Long performanceId);

	/**
	 * 특정 공연의 특정 회차 단건 조회(공연ID + 회차ID)
	 */
	Optional<PerformanceSchedule> findByPerformance_PerformanceIdAndPerformanceScheduleId(
			Long performanceId,
			Long performanceScheduleId
	);

	/**
	 * 예매 오픈된 회차들 조회 (ex: 배치/운영용)
	 */
	List<PerformanceSchedule> findAllByBookingOpenAtBeforeOrderByBookingOpenAtAsc(LocalDateTime now);
}
