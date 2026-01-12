package com.back.b2st.domain.queue.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.b2st.domain.queue.dto.QueueEntryStatusCount;
import com.back.b2st.domain.queue.entity.QueueEntry;
import com.back.b2st.domain.queue.entity.QueueEntryStatus;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

	/* ==================== Core Lookups ==================== */

	Optional<QueueEntry> findByQueueIdAndUserId(Long queueId, Long userId);

	Optional<QueueEntry> findByEntryToken(UUID entryToken);

	List<QueueEntry> findAllByStatus(QueueEntryStatus status);

	/* ==================== Circuit Breaker Fallback 메서드 ==================== */

	/**
	 * Circuit Breaker Fallback: isInEnterable
	 * Redis 장애 시 DB에서 ENTERABLE 상태 확인
	 */
	boolean existsByQueueIdAndUserIdAndStatus(Long queueId, Long userId, QueueEntryStatus status);

	/**
	 * Circuit Breaker Fallback: getTotalEnterableCount
	 * Redis 장애 시 DB에서 ENTERABLE 개수 조회
	 */
	long countByQueueIdAndStatus(Long queueId, QueueEntryStatus status);

	/* ==================== Statistics ==================== */

	@Query("""
       SELECT qe.status as status, COUNT(qe) as count
       FROM QueueEntry qe
       WHERE qe.queueId = :queueId
       GROUP BY qe.status
    """)
	List<QueueEntryStatusCount> countByStatusGrouped(@Param("queueId") Long queueId);

	/* ==================== Cleanup Targets (Batch Read) ==================== */

	@Query("""
       SELECT qe
       FROM QueueEntry qe
       WHERE qe.status = :status
         AND qe.expiresAt IS NOT NULL
         AND qe.expiresAt <= :now
       ORDER BY qe.expiresAt ASC
    """)
	List<QueueEntry> findExpiredByStatus(
		@Param("status") QueueEntryStatus status,
		@Param("now") LocalDateTime now,
		Pageable pageable
	);

	@Query("""
       SELECT qe
       FROM QueueEntry qe
       WHERE qe.status = :status
         AND qe.expiresAt IS NOT NULL
         AND qe.expiresAt > :now
       ORDER BY qe.expiresAt ASC
    """)
	List<QueueEntry> findNonExpiredByStatus(
		@Param("status") QueueEntryStatus status,
		@Param("now") LocalDateTime now,
		Pageable pageable
	);

	/* ==================== Active Existence ==================== */

	@Query("""
       SELECT (COUNT(qe) > 0)
       FROM QueueEntry qe
       WHERE qe.queueId = :queueId
         AND qe.userId = :userId
         AND qe.status = :status
         AND qe.expiresAt IS NOT NULL
         AND qe.expiresAt > :now
    """)
	boolean existsActive(
		@Param("queueId") Long queueId,
		@Param("userId") Long userId,
		@Param("status") QueueEntryStatus status,
		@Param("now") LocalDateTime now
	);

	/* ==================== Optional Bulk Ops ==================== */

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
       UPDATE QueueEntry qe
       SET qe.status = :toStatus
       WHERE qe.queueId = :queueId
         AND qe.userId IN :userIds
         AND qe.status = :fromStatus
    """)
	int bulkUpdateStatus(
		@Param("queueId") Long queueId,
		@Param("userIds") List<Long> userIds,
		@Param("fromStatus") QueueEntryStatus fromStatus,
		@Param("toStatus") QueueEntryStatus toStatus
	);

	@Query("SELECT DISTINCT qe.queueId FROM QueueEntry qe")
	List<Long> findDistinctQueueIds();
}