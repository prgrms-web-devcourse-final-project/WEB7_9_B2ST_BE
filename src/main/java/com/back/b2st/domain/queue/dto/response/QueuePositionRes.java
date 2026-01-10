package com.back.b2st.domain.queue.dto.response;

import com.back.b2st.domain.queue.entity.QueueEntry;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 대기열 위치 조회 응답 DTO (사용자용)
 *
 * 사용자가 자신의 대기 위치 정보만 조회할 수 있습니다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueuePositionRes(
	Long queueId,
	Long userId,
	String status,        // WAITING, ENTERABLE, EXPIRED, COMPLETED, NOT_IN_QUEUE
	Integer aheadCount,   // 내 앞에 있는 사람 수 (WAITING 상태일 때만)
	Integer myRank        // 내 순번 (WAITING 상태일 때만)
) {
	/**
	 * WAITING 상태 응답
	 */
	public static QueuePositionRes waiting(
		Long queueId,
		Long userId,
		Integer aheadCount,
		Integer myRank
	) {
		return new QueuePositionRes(
			queueId,
			userId,
			"WAITING",
			aheadCount,
			myRank
		);
	}

	/**
	 * ENTERABLE 상태 응답
	 */
	public static QueuePositionRes enterable(Long queueId, Long userId) {
		return new QueuePositionRes(
			queueId,
			userId,
			"ENTERABLE",
			null,
			null
		);
	}

	/**
	 * EXPIRED 상태 응답
	 */
	public static QueuePositionRes expired(Long queueId, Long userId) {
		return new QueuePositionRes(
			queueId,
			userId,
			"EXPIRED",
			null,
			null
		);
	}

	/**
	 * COMPLETED 상태 응답
	 */
	public static QueuePositionRes completed(Long queueId, Long userId) {
		return new QueuePositionRes(
			queueId,
			userId,
			"COMPLETED",
			null,
			null
		);
	}

	/**
	 * 대기열에 등록되지 않은 경우
	 */
	public static QueuePositionRes notInQueue(Long queueId, Long userId) {
		return new QueuePositionRes(
			queueId,
			userId,
			"NOT_IN_QUEUE",
			null,
			null
		);
	}

	/**
	 * QueueEntry로부터 생성 (DB 저장된 상태)
	 */
	public static QueuePositionRes fromEntry(QueueEntry entry) {
		return new QueuePositionRes(
			entry.getQueueId(),
			entry.getUserId(),
			entry.getStatus().name(),
			null,
			null
		);
	}
}

