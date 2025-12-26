package com.back.b2st.global.error.code;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
	String getCode();

	String getMessage();

	HttpStatus getStatus();
}
