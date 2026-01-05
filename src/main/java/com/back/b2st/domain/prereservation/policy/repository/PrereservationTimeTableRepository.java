package com.back.b2st.domain.prereservation.policy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.b2st.domain.prereservation.policy.entity.PrereservationTimeTable;

public interface PrereservationTimeTableRepository extends JpaRepository<PrereservationTimeTable, Long> {

	Optional<PrereservationTimeTable> findByPerformanceScheduleIdAndSectionId(
		Long performanceScheduleId,
		Long sectionId
	);

	/**
	 * (performanceScheduleId, sectionId) 조합이 중복으로 존재하더라도 단건 조회 시 예외가 나지 않도록 Top1로 조회합니다.
	 */
	Optional<PrereservationTimeTable> findTopByPerformanceScheduleIdAndSectionIdOrderByIdDesc(
		Long performanceScheduleId,
		Long sectionId
	);

	List<PrereservationTimeTable> findAllByPerformanceScheduleIdOrderByBookingStartAtAscSectionIdAsc(
		Long performanceScheduleId
	);

	void deleteAllByPerformanceScheduleIdIn(List<Long> performanceScheduleIds);
}
