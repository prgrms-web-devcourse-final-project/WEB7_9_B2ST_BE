package com.back.b2st.domain.queue.service;

public interface QueueAccessService {

	/**
	 * 사용자가 해당 공연의 대기열을 통과했는지 확인 (권장)
	 *
	 * ENTERABLE 상태가 아니면 BusinessException throw
	 */
	void assertEnterable(Long performanceId, Long userId);

}
