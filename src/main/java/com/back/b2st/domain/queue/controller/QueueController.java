package com.back.b2st.domain.queue.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.queue.dto.response.QueueEntryRes;
import com.back.b2st.domain.queue.dto.response.QueuePositionRes;
import com.back.b2st.domain.queue.service.QueueService;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.global.error.code.CommonErrorCode;
import com.back.b2st.global.error.exception.BusinessException;
import com.back.b2st.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/queues")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
@Tag(name = "QueueController", description = "대기열 API (로그인 필요)")
@SecurityRequirement(name = "BearerAuth")
public class QueueController {

	private final QueueService queueService;

	/**
	 * 대기열 입장
	 */
	@Operation(summary = "대기열 입장", description = "사용자를 대기열에 등록합니다.")
	@PostMapping("/{queueId}/enter")
	public BaseResponse<QueueEntryRes> enterQueue(
		@Parameter(description = "대기열 ID", example = "1")
		@PathVariable Long queueId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		// 방어 코드: Security 설정 실수/예외 케이스 대응
		if (principal == null) {
			throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
		}

		Long userId = principal.getId();
		log.info("User entering queue - queueId: {}, userId: {}", queueId, userId);

		QueueEntryRes response = queueService.enterQueue(queueId, userId);
		return BaseResponse.created(response);
	}

	/**
	 * 내 대기 상태 조회 (Deprecated)
	 */
	@Deprecated
	@GetMapping("/{queueId}/status")
	public BaseResponse<QueuePositionRes> getMyStatus(
		@PathVariable Long queueId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		if (principal == null) {
			throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
		}

		Long userId = principal.getId();
		log.debug("User checking queue status - queueId: {}, userId: {}", queueId, userId);

		QueuePositionRes response = queueService.getMyPosition(queueId, userId);
		return BaseResponse.success(response);
	}

	/**
	 * 입장 완료 처리
	 */
	@PostMapping("/{queueId}/complete")
	public BaseResponse<Void> completeEntry(
		@PathVariable Long queueId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		if (principal == null) {
			throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
		}

		Long userId = principal.getId();
		log.info("User completing entry - queueId: {}, userId: {}", queueId, userId);

		queueService.completeEntry(queueId, userId);
		return BaseResponse.success(null);
	}

	/**
	 * 대기열 나가기
	 */
	@DeleteMapping("/{queueId}/exit")
	public BaseResponse<Void> exitQueue(
		@PathVariable Long queueId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		if (principal == null) {
			throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
		}

		Long userId = principal.getId();
		log.info("User exiting queue - queueId: {}, userId: {}", queueId, userId);

		queueService.exitQueue(queueId, userId);
		return BaseResponse.success(null);
	}

	/**
	 * 내 대기 위치 조회
	 */
	@GetMapping("/{queueId}/position")
	public BaseResponse<QueuePositionRes> getMyPosition(
		@PathVariable Long queueId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		if (principal == null) {
			throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
		}

		Long userId = principal.getId();
		log.debug("User checking queue position - queueId: {}, userId: {}", queueId, userId);

		QueuePositionRes response = queueService.getMyPosition(queueId, userId);
		return BaseResponse.success(response);
	}
}