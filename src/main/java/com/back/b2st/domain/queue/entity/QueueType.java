package com.back.b2st.domain.queue.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 대기열 타입
 */
@Getter
@RequiredArgsConstructor
@Schema(description = "대기열 타입")
public enum QueueType {

	@Schema(description = "선착순 예매")
	BOOKING_ORDER("선착순 예매"),

	@Schema(description = "접속 제어")
	ENTRY_GATE("접속 제어");

	private final String description;
}

