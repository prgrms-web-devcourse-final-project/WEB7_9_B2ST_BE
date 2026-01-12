package com.back.b2st.domain.queue.dto.response;

import java.util.List;

import com.back.b2st.domain.queue.dto.QueueEntryStatusCount;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 대기열 통계 조회 응답 DTO (관리자용)
 *
 * 전체 통계 정보를 포함합니다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueueStatisticsRes(
	Long queueId,
	Integer totalWaiting,        // Redis WAITING 카운트
	Integer totalEnterable,      // Redis ENTERABLE 카운트
	Integer maxActiveUsers,      // 최대 활성 사용자 수
	List<StatusCount> statusCounts  // DB 상태별 통계
) {
	/**
	 * 상태별 카운트
	 */
	public record StatusCount(
		String status,  // WAITING, ENTERABLE, EXPIRED, COMPLETED
		Long count
	) {
		public static StatusCount from(QueueEntryStatusCount count) {
			return new StatusCount(
				count.getStatus().name(),
				count.getCount()
			);
		}
	}

	/**
	 * 통계 응답 생성
	 */
	public static QueueStatisticsRes of(
		Long queueId,
		Integer totalWaiting,
		Integer totalEnterable,
		Integer maxActiveUsers,
		List<QueueEntryStatusCount> statusCounts
	) {
		List<StatusCount> counts = statusCounts.stream()
			.map(StatusCount::from)
			.toList();

		return new QueueStatisticsRes(
			queueId,
			totalWaiting,
			totalEnterable,
			maxActiveUsers,
			counts
		);
	}
}

