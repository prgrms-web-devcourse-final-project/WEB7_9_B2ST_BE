package com.back.b2st.domain.performanceschedule.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.b2st.domain.performanceschedule.dto.DrawTargetPerformance;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;

public interface PerformanceScheduleRepository extends JpaRepository<PerformanceSchedule, Long> {

	List<PerformanceSchedule> findAllByPerformance_PerformanceIdOrderByStartAtAsc(Long performanceId);

	Optional<PerformanceSchedule> findByPerformance_PerformanceIdAndPerformanceScheduleId(
		Long performanceId,
		Long performanceScheduleId
	);

	//특정 공연(Performance)에 속한 특정 회차(Schedule)가 맞는지 검증
	Optional<PerformanceSchedule> findByPerformanceScheduleIdAndPerformance_PerformanceId(
		Long scheduleId, Long performanceId);

	@Query("""
		select case when count(ps) > 0 then true else false end
		from PerformanceSchedule ps
		where ps.performanceScheduleId = :scheduleId
		  and ps.performance.performanceId = :performanceId
		  and ps.bookingType = : bookingType
		""")
	boolean existsByPerformanceAndScheduleMatch(
		@Param("scheduleId") Long scheduleId,
		@Param("performanceId") Long performanceId,
		@Param("bookingType") BookingType bookingType);

	//예매 오픈 시간이 이미 지난(또는 지금 오픈해야 하는) 회차들을 목록으로 가져올 때 사용
	List<PerformanceSchedule> findAllByBookingOpenAtBeforeOrderByBookingOpenAtAsc(LocalDateTime now);

	/**
	 * 응모 종료 된 공연 중 아직 추첨하지 않은 공연을 조회
	 */
	@Query("""
			SELECT new com.back.b2st.domain.performanceschedule.dto.DrawTargetPerformance(
					ps.performance.performanceId,
					ps.performanceScheduleId
			)
			FROM PerformanceSchedule ps
			JOIN ps.performance
			WHERE ps.bookingCloseAt >= :start
			  AND ps.bookingCloseAt < :end
			  AND ps.drawCompleted = false
			  AND ps.performance.performanceId = ps.performance.performanceId
		""")
	List<DrawTargetPerformance> findByClosedBetweenAndNotDrawn(
		@Param("start") LocalDateTime startDate,
		@Param("end") LocalDateTime endDate);

	/**
	 * 회차 추첨 완료 업데이트
	 * @param scheduleId
	 */
	@Modifying(clearAutomatically = true)
	@Query("""
		update PerformanceSchedule ps
		set ps.drawCompleted = true
		where ps.performanceScheduleId = :scheduleId
		""")
	void updateStautsById(@Param("scheduleId") Long scheduleId);
}
