package com.back.b2st.domain.prereservation.entry.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.b2st.domain.prereservation.entry.entity.Prereservation;

public interface PrereservationRepository extends JpaRepository<Prereservation, Long> {

	boolean existsByPerformanceScheduleIdAndMemberIdAndSectionId(Long performanceScheduleId, Long memberId,
		Long sectionId);

	List<Prereservation> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);

	List<Prereservation> findAllByPerformanceScheduleIdAndMemberIdOrderByCreatedAtDesc(
		Long performanceScheduleId,
		Long memberId
	);

	void deleteAllByPerformanceScheduleIdIn(List<Long> performanceScheduleIds);
}
