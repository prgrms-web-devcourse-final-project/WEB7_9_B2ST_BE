package com.back.b2st.domain.performance.repository;

import java.util.List;
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

	/* =========================
	 * 사용자용 (ACTIVE만) - Offset(Page)
	 * ========================= */

	@EntityGraph(attributePaths = "venue")
	Page<Performance> findByStatus(PerformanceStatus status, Pageable pageable);

	@EntityGraph(attributePaths = "venue")
	Optional<Performance> findWithVenueByPerformanceIdAndStatus(Long performanceId, PerformanceStatus status);

	@EntityGraph(attributePaths = "venue")
	@Query("""
        select p
        from Performance p
        where p.status = :status
          and (
               lower(p.title) like lower(concat('%', :keyword, '%'))
            or lower(p.category) like lower(concat('%', :keyword, '%'))
          )
    """)
	Page<Performance> searchActive(
		@Param("status") PerformanceStatus status,
		@Param("keyword") String keyword,
		Pageable pageable
	);

	/* =========================
	 * 관리자용 (상태 무관) - Offset(Page)
	 * ========================= */

	@Override
	@EntityGraph(attributePaths = "venue")
	Page<Performance> findAll(Pageable pageable);

	@EntityGraph(attributePaths = "venue")
	Optional<Performance> findWithVenueByPerformanceId(Long performanceId);

	@EntityGraph(attributePaths = "venue")
	@Query("""
        select p
        from Performance p
        where lower(p.title) like lower(concat('%', :keyword, '%'))
           or lower(p.category) like lower(concat('%', :keyword, '%'))
    """)
	Page<Performance> searchAll(@Param("keyword") String keyword, Pageable pageable);

	/* =========================
	 * Cursor 기반 페이징 (공통 규칙)
	 * - 정렬: performanceId DESC (최신순)
	 * - 조건: performanceId < cursor
	 * - Pageable: PageRequest.of(0, size+1)
	 * ========================= */

	/* 사용자용 (ACTIVE만) - Cursor */
	@EntityGraph(attributePaths = "venue")
	@Query("""
        select p
        from Performance p
        where p.status = :status
          and (:cursor is null or p.performanceId < :cursor)
        order by p.performanceId desc
    """)
	List<Performance> findByStatusWithCursor(
		@Param("status") PerformanceStatus status,
		@Param("cursor") Long cursor,
		Pageable pageable
	);

	@EntityGraph(attributePaths = "venue")
	@Query("""
        select p
        from Performance p
        where p.status = :status
          and (:cursor is null or p.performanceId < :cursor)
          and (
               lower(p.title) like lower(concat('%', :keyword, '%'))
            or lower(p.category) like lower(concat('%', :keyword, '%'))
          )
        order by p.performanceId desc
    """)
	List<Performance> searchActiveWithCursor(
		@Param("status") PerformanceStatus status,
		@Param("keyword") String keyword,
		@Param("cursor") Long cursor,
		Pageable pageable
	);

	/* 관리자용 (상태 무관) - Cursor */
	@EntityGraph(attributePaths = "venue")
	@Query("""
        select p
        from Performance p
        where (:cursor is null or p.performanceId < :cursor)
        order by p.performanceId desc
    """)
	List<Performance> findAllWithCursor(
		@Param("cursor") Long cursor,
		Pageable pageable
	);

	@EntityGraph(attributePaths = "venue")
	@Query("""
        select p
        from Performance p
        where (:cursor is null or p.performanceId < :cursor)
          and (
               lower(p.title) like lower(concat('%', :keyword, '%'))
            or lower(p.category) like lower(concat('%', :keyword, '%'))
          )
        order by p.performanceId desc
    """)
	List<Performance> searchAllWithCursor(
		@Param("keyword") String keyword,
		@Param("cursor") Long cursor,
		Pageable pageable
	);
}