package com.back.b2st.global.error.handler;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.back.b2st.global.error.code.CommonErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 비즈니스 예외 처리
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<Map<String, String>> handleBusinessException(BusinessException ex) {
		log.error("BusinessException: {}", ex.getMessage(), ex);
		return ResponseEntity
			.status(ex.getErrorCode().getStatus())
			.body(createErrorResponse(ex.getErrorCode().getCode(), ex.getErrorCode().getMessage()));
	}

	/**
	 * Validation 예외 처리
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex) {
		log.error("MethodArgumentNotValidException: {}", ex.getMessage(), ex);
		return ResponseEntity
			.status(CommonErrorCode.BAD_REQUEST.getStatus())
			.body(createErrorResponse(
				CommonErrorCode.BAD_REQUEST.getCode(),
				CommonErrorCode.BAD_REQUEST.getMessage()
			));
	}


	/**
	 * 요청 본문 파싱 실패
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, String>> handleNotReadableException(HttpMessageNotReadableException ex) {
		log.error("HttpMessageNotReadableException: {}", ex.getMessage(), ex);
		return ResponseEntity
			.status(CommonErrorCode.BAD_REQUEST.getStatus())
			.body(createErrorResponse(
				CommonErrorCode.BAD_REQUEST.getCode(),
				CommonErrorCode.BAD_REQUEST.getMessage()
			));
	}

	/**
	 * HTTP 메서드 불일치
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<Map<String, String>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
		log.error("HttpRequestMethodNotSupportedException: {}", ex.getMessage(), ex);
		return ResponseEntity
			.status(CommonErrorCode.METHOD_NOT_ALLOWED.getStatus())
			.body(createErrorResponse(
				CommonErrorCode.METHOD_NOT_ALLOWED.getCode(),
				CommonErrorCode.METHOD_NOT_ALLOWED.getMessage()
			));
	}

	/**
	 * 그 외 모든 예외 처리
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> handleException(Exception ex) {
		log.error("Unexpected Exception: {}", ex.getMessage(), ex);
		return ResponseEntity
			.status(CommonErrorCode.INTERNAL_SERVER_ERROR.getStatus())
			.body(createErrorResponse(
				CommonErrorCode.INTERNAL_SERVER_ERROR.getCode(),
				CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage()
			));
	}

	/**
	 * 에러 응답 생성
	 */
	private Map<String, String> createErrorResponse(String code, String message) {
		Map<String, String> response = new HashMap<>();
		response.put("code", code);
		response.put("message", message);
		return response;
	}
}
