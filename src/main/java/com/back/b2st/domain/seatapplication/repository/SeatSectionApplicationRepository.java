package com.back.b2st.domain.seatapplication.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.b2st.domain.seatapplication.entity.SeatSectionApplication;

public interface SeatSectionApplicationRepository extends JpaRepository<SeatSectionApplication, Long> {

	boolean existsByPerformanceScheduleIdAndMemberIdAndSectionId(Long performanceScheduleId, Long memberId,
		Long sectionId);

	List<SeatSectionApplication> findAllByPerformanceScheduleIdAndMemberIdOrderByCreatedAtDesc(
		Long performanceScheduleId,
		Long memberId
	);
}
