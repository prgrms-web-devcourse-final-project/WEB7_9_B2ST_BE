package com.back.b2st.global.error.handler;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.back.b2st.domain.auth.error.AuthErrorCode;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.global.error.code.CommonErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/* =========================
	   도메인 비즈니스 예외
	   ========================= */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<BaseResponse<Void>> handleBusinessException(BusinessException ex) {
		log.error("BusinessException: {}", ex.getMessage(), ex);

		return ResponseEntity
			.status(ex.getErrorCode().getStatus())
			.body(BaseResponse.error(ex.getErrorCode())); // data = null
	}

	/* =========================
	   검증/바인딩 예외
	   ========================= */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<BaseResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {

		String errorMessage = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();

		// null이거나 빈 문자열이면 기본 메시지 사용하게
		if (!StringUtils.hasText(errorMessage)) {
			errorMessage = CommonErrorCode.BAD_REQUEST.getMessage(); // "잘못된 요청입니다."
		}

		log.error("Validation Error: {}", errorMessage);

		return ResponseEntity
			.status(CommonErrorCode.BAD_REQUEST.getStatus())
			.body(new BaseResponse<>(
				CommonErrorCode.BAD_REQUEST.getStatus().value(),
				errorMessage, // DTO 메시지 or 기본 메시지
				null
			)); // data = null
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<BaseResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
		log.error("HttpMessageNotReadableException: {}", ex.getMessage(), ex);

		return ResponseEntity
			.status(CommonErrorCode.BAD_REQUEST.getStatus())
			.body(BaseResponse.error(CommonErrorCode.BAD_REQUEST)); // data = null
	}

	/* =========================
	   HTTP 관련 예외
	   ========================= */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<BaseResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
		log.error("HttpRequestMethodNotSupportedException: {}", ex.getMessage(), ex);

		return ResponseEntity
			.status(CommonErrorCode.METHOD_NOT_ALLOWED.getStatus())
			.body(BaseResponse.error(CommonErrorCode.METHOD_NOT_ALLOWED)); // data = null
	}

	/* =========================
            인증 관련 예외
            ========================= */
	// 로그인 실패
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<BaseResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
		log.error("BadCredentialsException: {}", ex.getMessage());
		return ResponseEntity
			.status(AuthErrorCode.LOGIN_FAILED.getStatus())
			.body(BaseResponse.error(AuthErrorCode.LOGIN_FAILED));
	}

	// 권한 부족
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<BaseResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
		log.error("AccessDeniedException: {}", ex.getMessage());
		return ResponseEntity
			.status(AuthErrorCode.UNAUTHORIZED_ACCESS.getStatus())
			.body(BaseResponse.error(AuthErrorCode.UNAUTHORIZED_ACCESS));
	}

	/* =========================
	   그 외 모든 예외
	   ========================= */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<BaseResponse<Void>> handleException(Exception ex) {
		log.error("Unhandled Exception: {}", ex.getMessage(), ex);

		return ResponseEntity
			.status(CommonErrorCode.INTERNAL_SERVER_ERROR.getStatus())
			.body(BaseResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR)); // data = null
	}
}
