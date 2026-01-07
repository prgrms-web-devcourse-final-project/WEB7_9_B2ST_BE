package com.back.b2st.domain.queue.dto.response;

import com.back.b2st.domain.queue.entity.Queue;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 대기열 정보 응답 DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueueRes(
	Long queueId,
	Long performanceId,
	String queueType,
	Integer maxActiveUsers,
	Integer entryTtlMinutes,
	Integer currentWaiting,      // 현재 대기 중인 인원 (Redis)
	Integer currentEnterable     // 현재 입장 가능 인원 (Redis)
) {

	/**
	 * Queue Entity → Response 변환 (Redis 정보 없음)
	 */
	public static QueueRes from(Queue queue) {
		return new QueueRes(
			queue.getId(),
			queue.getPerformanceId(),
			queue.getQueueType().name(),
			queue.getMaxActiveUsers(),
			queue.getEntryTtlMinutes(),
			null,
			null
		);
	}

	/**
	 * Queue Entity + Redis 정보 → Response 변환
	 */
	public static QueueRes of(Queue queue, Integer currentWaiting, Integer currentEnterable) {
		return new QueueRes(
			queue.getId(),
			queue.getPerformanceId(),
			queue.getQueueType().name(),
			queue.getMaxActiveUsers(),
			queue.getEntryTtlMinutes(),
			currentWaiting,
			currentEnterable
		);
	}
}

