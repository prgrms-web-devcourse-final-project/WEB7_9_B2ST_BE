package com.back.b2st.domain.queue.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.b2st.domain.queue.entity.Queue;
import com.back.b2st.domain.queue.entity.QueueType;

public interface QueueRepository extends JpaRepository<Queue, Long> {

	/**
	 * 회차 ID와 대기열 타입으로 대기열 조회
	 */
	Optional<Queue> findByScheduleIdAndQueueType(Long scheduleId, QueueType queueType);

	/**
	 * 회차 ID로 모든 대기열 조회
	 */
	List<Queue> findByScheduleId(Long scheduleId);

	/**
	 * 대기열 타입으로 조회
	 */
	List<Queue> findByQueueType(QueueType queueType);

	/**
	 * 여러 회차의 대기열 목록 일괄 조회
	 */
	List<Queue> findByScheduleIdIn(Collection<Long> scheduleIds);

	/**
	 * 대기열 존재 여부 확인
	 */
	boolean existsByScheduleIdAndQueueType(Long scheduleId, QueueType queueType);

	/**
	 * 특정 회차의 대기열 개수
	 */
	long countByScheduleId(Long scheduleId);

	/**
	 * 대용량 대기열 조회 (동시 입장 허용 수가 특정 값 이상)
	 */
	@Query("SELECT q FROM Queue q WHERE q.maxActiveUsers >= :minActiveUsers")
	List<Queue> findLargeCapacityQueues(@Param("minActiveUsers") Integer minActiveUsers);
}
