package com.back.b2st.domain.prereservation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.b2st.domain.prereservation.entity.Prereservation;

public interface PrereservationRepository extends JpaRepository<Prereservation, Long> {

	boolean existsByPerformanceScheduleIdAndMemberIdAndSectionId(Long performanceScheduleId, Long memberId,
		Long sectionId);

	List<Prereservation> findAllByPerformanceScheduleIdAndMemberIdOrderByCreatedAtDesc(
		Long performanceScheduleId,
		Long memberId
	);
}
