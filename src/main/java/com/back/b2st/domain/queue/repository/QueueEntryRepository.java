package com.back.b2st.domain.queue.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.b2st.domain.queue.dto.QueueEntryStatusCount;
import com.back.b2st.domain.queue.entity.QueueEntry;
import com.back.b2st.domain.queue.entity.QueueEntryStatus;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

	/**
	 * 대기열 ID와 사용자 ID로 조회
	 */
	Optional<QueueEntry> findByQueueIdAndUserId(Long queueId, Long userId);

	/**
	 * 입장권 토큰으로 조회
	 */
	Optional<QueueEntry> findByEntryToken(UUID entryToken);

	/**
	 * 사용자의 모든 입장 기록 조회
	 */
	List<QueueEntry> findByUserId(Long userId);

	/**
	 * 대기열의 모든 입장 기록 조회
	 */
	List<QueueEntry> findByQueueId(Long queueId);

	/**
	 * 대기열의 특정 상태 입장 기록 조회
	 */
	List<QueueEntry> findByQueueIdAndStatus(Long queueId, QueueEntryStatus status);

	/**
	 * 특정 상태의 모든 입장 기록 조회 (정합성 보정용)
	 */
	List<QueueEntry> findAllByStatus(QueueEntryStatus status);

	/**
	 * 대기열의 특정 상태 입장 기록 조회
	 */
	Page<QueueEntry> findByQueueIdAndStatus(Long queueId, QueueEntryStatus status, Pageable pageable);

	/**
	 * 대기열의 활성(ENTERABLE) 입장 수
	 */
	@Query("""
		SELECT COUNT(qe) FROM QueueEntry qe
		WHERE qe.queueId = :queueId
			AND qe.status = 'ENTERABLE'
		""")
	long countActiveEntries(@Param("queueId") Long queueId);

	/**
	 * 대기열별 상태별 통계 (DTO Projection)
	 */
	@Query("""
		SELECT qe.status as status, COUNT(qe) as count
		FROM QueueEntry qe
		WHERE qe.queueId = :queueId
		GROUP BY qe.status
		""")
	List<QueueEntryStatusCount> countByStatusGrouped(@Param("queueId") Long queueId);

	/**
	 * 사용자별 완료 횟수
	 */
	@Query("""
		SELECT COUNT(qe) FROM QueueEntry qe
		WHERE qe.userId = :userId
			AND qe.status = 'COMPLETED'
		""")
	long countCompletedByUser(@Param("userId") Long userId);

	/**
	 * 만료된 입장 일괄 업데이트 (Redis 동기화용)
	 */
	@Modifying(clearAutomatically = true)
	@Query("""
		UPDATE QueueEntry qe
		SET qe.status = 'EXPIRED'
		WHERE qe.queueId = :queueId
			AND qe.userId IN :userIds
			AND qe.status = 'ENTERABLE'
		""")
	int bulkUpdateStatusToExpired(@Param("queueId") Long queueId, @Param("userIds") List<Long> userIds);

	/**
	 * 만료 예정 입장 조회 (배치 작업용)
	 */
	@Query("""
		SELECT qe FROM QueueEntry qe
		WHERE qe.queueId = :queueId
			AND qe.status = 'ENTERABLE'
			AND qe.expiresAt <= :now
		""")
	List<QueueEntry> findExpiredEntries(@Param("queueId") Long queueId, @Param("now") LocalDateTime now);

	/**
	 * 전체 대기열에서 만료 예정 입장 조회
	 */
	@Query("""
		SELECT qe FROM QueueEntry qe
		WHERE qe.status = 'ENTERABLE'
			AND qe.expiresAt <= :now
		""")
	List<QueueEntry> findAllExpiredEntries(@Param("now") LocalDateTime now);

	/**
	 * 입장권 존재 여부 확인
	 */
	boolean existsByQueueIdAndUserId(Long queueId, Long userId);

	/**
	 * 입장권 토큰 존재 여부 확인
	 */
	boolean existsByEntryToken(UUID entryToken);

	/**
	 * 사용자가 활성 상태인지 확인
	 */
	@Query("""
		SELECT CASE WHEN COUNT(qe) > 0 THEN true ELSE false END
		FROM QueueEntry qe
		WHERE qe.queueId = :queueId
			AND qe.userId = :userId
			AND qe.status = 'ENTERABLE'
			AND qe.expiresAt > :now
		""")
	boolean isUserActiveInQueue(
		@Param("queueId") Long queueId,
		@Param("userId") Long userId,
		@Param("now") LocalDateTime now
	);

	/**
	 * 특정 기간 동안의 입장 기록 조회
	 */
	@Query("""
		SELECT qe FROM QueueEntry qe
		WHERE qe.queueId = :queueId
			AND qe.joinedAt BETWEEN :startTime AND :endTime
		ORDER BY qe.joinedAt ASC
		""")
	List<QueueEntry> findByQueueIdAndJoinedAtBetween(
		@Param("queueId") Long queueId,
		@Param("startTime") LocalDateTime startTime,
		@Param("endTime") LocalDateTime endTime
	);

	/**
	 * 특정 시간 이후 입장한 기록 조회
	 */
	@Query("""
		SELECT qe FROM QueueEntry qe
		WHERE qe.queueId = :queueId
			AND qe.joinedAt >= :afterTime
		ORDER BY qe.joinedAt DESC
		""")
	List<QueueEntry> findRecentEntries(
		@Param("queueId") Long queueId,
		@Param("afterTime") LocalDateTime afterTime
	);

	/**
	 * 특정 상태의 오래된 기록 삭제
	 */
	@Modifying(clearAutomatically = true)
	@Query("""
		DELETE FROM QueueEntry qe
		WHERE qe.status = :status
			AND qe.createdAt < :beforeTime
		""")
	int deleteOldEntriesByStatus(
		@Param("status") QueueEntryStatus status,
		@Param("beforeTime") LocalDateTime beforeTime
	);

	/**
	 * 특정 대기열의 모든 기록 삭제
	 */
	@Modifying(clearAutomatically = true)
	@Query("DELETE FROM QueueEntry qe WHERE qe.queueId = :queueId")
	int deleteByQueueId(@Param("queueId") Long queueId);
}

