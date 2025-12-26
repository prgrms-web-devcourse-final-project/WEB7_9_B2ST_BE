package com.back.b2st.domain.queue.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 대기열 나가기 요청 DTO
 */
public record ExitQueueReq(
	@NotNull(message = "대기열 ID는 필수입니다.")
	Long queueId,

	@NotNull(message = "사용자 ID는 필수입니다.")
	Long userId
) {
}

