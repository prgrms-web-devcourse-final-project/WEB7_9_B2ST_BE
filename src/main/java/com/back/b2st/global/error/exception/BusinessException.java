package com.back.b2st.global.error.exception;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public BusinessException(ErrorCode errorCode, String detailMessage) {
		super(errorCode.getMessage() + " - " + detailMessage);
		this.errorCode = errorCode;
	}
}
