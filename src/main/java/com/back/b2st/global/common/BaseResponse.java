package com.back.b2st.global.common;

import com.back.b2st.global.error.code.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BaseResponse<T> {

	private final String code;
	private final String message;
	private final T data;

	/* === 성공 응답 (데이터 있는 경우) === */
	public static <T> BaseResponse<T> success(T data) {
		return new BaseResponse<>("200", "성공적으로 처리되었습니다", data);
	}

	/* === 성공 응답 (데이터 없는 경우 -> null) === */
	public static <T> BaseResponse<T> success() {
		return new BaseResponse<>("200", "성공적으로 처리되었습니다", null);
	}

	/* === 성공 응답 (생성 완료) === */
	public static <T> BaseResponse<T> created(T data) {
		return new BaseResponse<>("201", "성공적으로 생성되었습니다.", data);
	}

	/* === 에러 응답 (항상 data = null) ==== */
	public static <T> BaseResponse<T> error(ErrorCode errorCode) {
		return new BaseResponse<>(
			errorCode.getCode(),
			errorCode.getMessage(),
			null
		);
	}
}