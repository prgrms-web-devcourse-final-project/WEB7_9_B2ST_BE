package com.back.b2st.domain.queue.dto.response;

import com.back.b2st.domain.queue.entity.QueueEntry;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 대기열 상태 조회 응답 DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueueStatusRes(
	Long queueId,
	Long userId,
	String status,            // WAITING(Redis), ENTERABLE, EXPIRED, COMPLETED
	Integer myRank,
	Integer waitingAhead,
	Integer totalWaiting,
	Integer totalEnterable,
	Integer maxActiveUsers
) {

	/**
	 * WAITING 상태 응답 (Redis에만 존재)
	 */
	public static QueueStatusRes waiting(
		Long queueId,
		Long userId,
		Integer myRank,
		Integer waitingAhead,
		Integer totalWaiting
	) {
		return new QueueStatusRes(
			queueId,
			userId,
			"WAITING",
			myRank,
			waitingAhead,
			totalWaiting,
			null,
			null
		);
	}

	/**
	 * ENTERABLE 상태 응답 (Redis 토큰 존재)
	 */
	public static QueueStatusRes enterable(Long queueId, Long userId) {
		return new QueueStatusRes(
			queueId,
			userId,
			"ENTERABLE",
			null,
			null,
			null,
			null,
			null
		);
	}

	/**
	 * EXPIRED 상태 응답 (표시용)
	 */
	public static QueueStatusRes expired(Long queueId, Long userId) {
		return new QueueStatusRes(
			queueId,
			userId,
			"EXPIRED",
			null,
			null,
			null,
			null,
			null
		);
	}

	/**
	 * QueueEntry로부터 생성 (DB 저장된 상태)
	 */
	public static QueueStatusRes fromEntry(QueueEntry entry) {
		return new QueueStatusRes(
			entry.getQueueId(),
			entry.getUserId(),
			entry.getStatus().name(),
			null,
			null,
			null,
			null,
			null
		);
	}

	/**
	 * 대기열 통계 응답
	 */
	public static QueueStatusRes statistics(
		Long queueId,
		Integer totalWaiting,
		Integer totalEnterable,
		Integer maxActiveUsers
	) {
		return new QueueStatusRes(
			queueId,
			null,
			null,
			null,
			null,
			totalWaiting,
			totalEnterable,
			maxActiveUsers
		);
	}
}
