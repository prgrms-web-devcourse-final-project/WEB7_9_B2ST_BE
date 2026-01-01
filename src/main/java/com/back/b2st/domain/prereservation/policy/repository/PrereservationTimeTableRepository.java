package com.back.b2st.domain.prereservation.policy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.b2st.domain.prereservation.policy.entity.PrereservationTimeTable;

public interface PrereservationTimeTableRepository extends JpaRepository<PrereservationTimeTable, Long> {

	Optional<PrereservationTimeTable> findByPerformanceScheduleIdAndSectionId(Long performanceScheduleId, Long sectionId);

	List<PrereservationTimeTable> findAllByPerformanceScheduleIdOrderByBookingStartAtAscSectionIdAsc(Long performanceScheduleId);
}
