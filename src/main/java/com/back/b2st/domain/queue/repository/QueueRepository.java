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
	 * 공연 ID로 대기열 조회
	 * UNIQUE 제약: (performance_id)
	 */
	Optional<Queue> findByPerformanceId(Long performanceId);

	void deleteByPerformanceId(Long performanceId);

	/**
	 * 대기열 존재 여부 확인 (공연 기준)
	 */
	boolean existsByPerformanceId(Long performanceId);

	/**
	 * 여러 공연의 대기열 목록 일괄 조회
	 */
	List<Queue> findByPerformanceIdIn(Collection<Long> performanceIds);

	/**
	 * 대기열 타입으로 조회
	 */
	List<Queue> findByQueueType(QueueType queueType);

	/**
	 * 대용량 대기열 조회 (동시 입장 허용 수가 특정 값 이상)
	 */
	@Query("SELECT q FROM Queue q WHERE q.maxActiveUsers >= :minActiveUsers")
	List<Queue> findLargeCapacityQueues(@Param("minActiveUsers") Integer minActiveUsers);
}
