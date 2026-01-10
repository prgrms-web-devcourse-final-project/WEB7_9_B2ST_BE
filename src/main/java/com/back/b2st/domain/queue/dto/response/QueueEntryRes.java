package com.back.b2st.domain.queue.dto.response;

import com.back.b2st.domain.queue.entity.QueueEntry;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 대기열 입장 응답 DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueueEntryRes(
	Long queueId,
	Long performanceId,
	Long scheduleId,         // 프론트 UX용 진입 정보
	Long userId,
	String status,            // WAITING(Redis), ENTERABLE, EXPIRED, COMPLETED
	Integer aheadCount,      // 내 앞에 대기 중인 사람 수
	Integer myRank           // 내 순번 (1부터 시작)
) {

	/**
	 * WAITING 상태 응답 (Redis에만 존재)
	 */
	public static QueueEntryRes waiting(Long queueId, Long performanceId, Long scheduleId, Long userId, Integer aheadCount, Integer myRank) {
		return new QueueEntryRes(
			queueId,
			performanceId,
			scheduleId,
			userId,
			"WAITING",
			aheadCount,
			myRank
		);
	}

	/**
	 * QueueEntry → Response 변환 (DB 저장된 상태)
	 */
	public static QueueEntryRes of(QueueEntry entry, Long performanceId, Long scheduleId, Integer aheadCount, Integer myRank) {
		return new QueueEntryRes(
			entry.getQueueId(),
			performanceId,
			scheduleId,
			entry.getUserId(),
			entry.getStatus().name(),
			aheadCount,
			myRank
		);
	}

	/**
	 * 간단한 응답 (순번 정보 없음)
	 */
	public static QueueEntryRes simple(QueueEntry entry, Long performanceId, Long scheduleId) {
		return new QueueEntryRes(
			entry.getQueueId(),
			performanceId,
			scheduleId,
			entry.getUserId(),
			entry.getStatus().name(),
			null,
			null
		);
	}
}

