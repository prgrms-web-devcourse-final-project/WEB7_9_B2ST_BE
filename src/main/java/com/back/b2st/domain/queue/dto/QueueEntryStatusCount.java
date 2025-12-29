package com.back.b2st.domain.queue.dto;

import com.back.b2st.domain.queue.entity.QueueEntryStatus;

/**
 * 대기열 상태별 통계 DTO
 */
public interface QueueEntryStatusCount {

	/**
	 * 대기열 입장 상태
	 */
	QueueEntryStatus getStatus();

	/**
	 * 해당 상태의 개수
	 */
	Long getCount();
}

