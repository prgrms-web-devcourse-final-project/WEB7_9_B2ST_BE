package com.back.b2st.domain.performanceschedule.repository;

import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformanceScheduleRepository extends JpaRepository<PerformanceSchedule, Long> {


	List<PerformanceSchedule> findAllByPerformance_PerformanceIdOrderByStartAtAsc(Long performanceId);


	Optional<PerformanceSchedule> findByPerformance_PerformanceIdAndPerformanceScheduleId(
			Long performanceId,
			Long performanceScheduleId
	);

	//특정 공연(Performance)에 속한 특정 회차(Schedule)가 맞는지 검증
	Optional<PerformanceSchedule> findByPerformanceScheduleIdAndPerformance_PerformanceId(
			Long scheduleId, Long performanceId);

	//예매 오픈 시간이 이미 지난(또는 지금 오픈해야 하는) 회차들을 목록으로 가져올 때 사용
	List<PerformanceSchedule> findAllByBookingOpenAtBeforeOrderByBookingOpenAtAsc(LocalDateTime now);
}
