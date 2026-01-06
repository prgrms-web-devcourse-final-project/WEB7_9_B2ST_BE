package com.back.b2st.domain.queue.error;

import org.springframework.http.HttpStatus;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 대기열 시스템 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum QueueErrorCode implements ErrorCode {

	// 대기열 관련
	QUEUE_NOT_FOUND(HttpStatus.NOT_FOUND, "Q001", "존재하지 않는 대기열입니다."),
	QUEUE_FULL(HttpStatus.BAD_REQUEST, "Q002", "대기열이 가득 찼습니다."),
	QUEUE_CLOSED(HttpStatus.BAD_REQUEST, "Q003", "마감된 대기열입니다."),

	// 대기열 입장 관련
	ALREADY_IN_QUEUE(HttpStatus.CONFLICT, "Q101", "이미 대기열에 등록되어 있습니다."),
	NOT_IN_QUEUE(HttpStatus.BAD_REQUEST, "Q102", "대기열에 등록되지 않은 사용자입니다."),
	INVALID_QUEUE_STATUS(HttpStatus.BAD_REQUEST, "Q103", "유효하지 않은 대기열 상태입니다."),

	// Redis 연동 관련
	REDIS_CONNECTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Q201", "Redis 연결 오류가 발생했습니다."),
	REDIS_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Q202", "Redis 작업 중 오류가 발생했습니다."),
	QUEUE_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Q203", "대기열 시스템이 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요."),

	// 데이터 정합성 관련
	QUEUE_DATA_INCONSISTENT(HttpStatus.INTERNAL_SERVER_ERROR, "Q301", "대기열 데이터 불일치가 발생했습니다."),
	QUEUE_ENTRY_EXPIRED(HttpStatus.BAD_REQUEST, "Q302", "입장 권한이 만료되었습니다."),

	// 내부 오류
	QUEUE_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Q999", "대기열 시스템 내부 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}