package com.back.b2st.domain.performance.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.entity.PerformanceStatus;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {

	/**
	 * 판매중인 공연만 목록 조회(사용자용)
	 */
	@EntityGraph(attributePaths = "venue")
	Page<Performance> findByStatus(PerformanceStatus status, Pageable pageable);

	/**
	 * 공연 상세 조회(판매중만)
	 */
	@EntityGraph(attributePaths = "venue")
	Optional<Performance> findWithVenueByPerformanceIdAndStatus(
			Long performanceId,
			PerformanceStatus status
	);

	/**
	 * 공연 검색
	 */
	@EntityGraph(attributePaths = "venue")
	@Query(
			"""
					select p
					from Performance p
					where p.status = :status
						and (
							lower(p.title) like lower(concat('%', :keyword, '%'))
							or lower(p.category) like lower(concat('%', :keyword, '%'))
						)
					"""
	)
	Page<Performance> searchOnSale(
			@Param("status") PerformanceStatus status,
			@Param("keyword") String keyword,
			Pageable pageable
	);
}
