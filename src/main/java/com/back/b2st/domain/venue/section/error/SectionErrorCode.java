package com.back.b2st.domain.venue.section.error;

import org.springframework.http.HttpStatus;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SectionErrorCode implements ErrorCode {
	INVALID_VENUE_INFO(HttpStatus.BAD_REQUEST, "E401", "공연장 정보가 올바르지 않습니다."),
	DUPLICATE_SECTION(HttpStatus.CONFLICT, "E402", "이미 등록된 구역입니다."),
	SECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "E002", "구역 정보를 찾을 수 없습니다."),

	CREATE_SECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "E501", "구역 등록 중 서버 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
