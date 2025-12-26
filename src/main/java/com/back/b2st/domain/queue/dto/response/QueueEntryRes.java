package com.back.b2st.domain.queue.dto.response;

import com.back.b2st.domain.queue.entity.QueueEntry;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 대기열 입장 응답 DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueueEntryRes(
	Long queueId,
	Long userId,
	String status,            // WAITING(Redis), ENTERABLE, EXPIRED, COMPLETED
	Integer myRank,           // 내 순번 (1부터 시작)
	Integer waitingAhead      // 내 앞에 대기 중인 사람 수
) {

	/**
	 * WAITING 상태 응답 (Redis에만 존재)
	 */
	public static QueueEntryRes waiting(Long queueId, Long userId, Integer myRank, Integer waitingAhead) {
		return new QueueEntryRes(
			queueId,
			userId,
			"WAITING",
			myRank,
			waitingAhead
		);
	}

	/**
	 * QueueEntry → Response 변환 (DB 저장된 상태)
	 */
	public static QueueEntryRes of(QueueEntry entry, Integer myRank, Integer waitingAhead) {
		return new QueueEntryRes(
			entry.getQueueId(),
			entry.getUserId(),
			entry.getStatus().name(),
			myRank,
			waitingAhead
		);
	}

	/**
	 * 간단한 응답 (순번 정보 없음)
	 */
	public static QueueEntryRes simple(QueueEntry entry) {
		return new QueueEntryRes(
			entry.getQueueId(),
			entry.getUserId(),
			entry.getStatus().name(),
			null,
			null
		);
	}
}

