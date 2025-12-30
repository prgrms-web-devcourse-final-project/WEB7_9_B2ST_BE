package com.back.b2st.domain.queue.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.queue.dto.response.QueueEntryRes;
import com.back.b2st.domain.queue.dto.response.QueueStatusRes;
import com.back.b2st.domain.queue.service.QueueService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Queue Controller
 *
 * 대기열 시스템 REST API
 */
@RestController
@RequestMapping("/api/queues")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
@Tag(name = "QueueController", description = "대기열 API")
@SecurityRequirement(name = "BearerAuth")
public class QueueController {

	private final QueueService queueService;

	/**
	 * 대기열 입장
	 * POST /api/queues/{queueId}/enter
	 */
	@Operation(summary = "대기열 입장", description = "사용자를 대기열에 등록합니다. WAITING 상태로 Redis에 저장됩니다.")
	@PostMapping("/{queueId}/enter")
	public ResponseEntity<QueueEntryRes> enterQueue(
		@Parameter(description = "대기열 ID", example = "1")
		@PathVariable Long queueId,
		Authentication authentication
	) {
		Long userId = Long.parseLong(authentication.getName());
		log.info("User entering queue - queueId: {}, userId: {}", queueId, userId);

		QueueEntryRes response = queueService.enterQueue(queueId, userId);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * 내 대기 상태 조회
	 * GET /api/queues/{queueId}/status
	 */
	@Operation(summary = "내 대기 상태 조회", description = "현재 사용자의 대기열 상태를 조회합니다.")
	@GetMapping("/{queueId}/status")
	public ResponseEntity<QueueStatusRes> getMyStatus(
		@Parameter(description = "대기열 ID", example = "1")
		@PathVariable Long queueId,
		Authentication authentication
	) {
		Long userId = Long.parseLong(authentication.getName());
		log.debug("User checking queue status - queueId: {}, userId: {}", queueId, userId);

		QueueStatusRes response = queueService.getMyStatus(queueId, userId);
		return ResponseEntity.ok(response);
	}

	/**
	 * 입장 완료 처리
	 * POST /api/queues/{queueId}/complete
	 */
	@Operation(summary = "입장 완료 처리", description = "ENTERABLE 상태의 사용자가 입장을 완료했을 때 호출합니다. 상태가 COMPLETED로 변경됩니다.")
	@PostMapping("/{queueId}/complete")
	public ResponseEntity<Void> completeEntry(
		@Parameter(description = "대기열 ID", example = "1")
		@PathVariable Long queueId,
		Authentication authentication
	) {
		Long userId = Long.parseLong(authentication.getName());
		log.info("User completing entry - queueId: {}, userId: {}", queueId, userId);

		queueService.completeEntry(queueId, userId);
		return ResponseEntity.ok().build();
	}

	/**
	 * 대기열 나가기(취소)
	 * DELETE /api/queues/{queueId}/exit
	 */
	@Operation(summary = "대기열 나가기", description = "사용자가 대기열에서 나가거나 취소할 때 호출합니다.")
	@DeleteMapping("/{queueId}/exit")
	public ResponseEntity<Void> exitQueue(
		@Parameter(description = "대기열 ID", example = "1")
		@PathVariable Long queueId,
		Authentication authentication
	) {
		Long userId = Long.parseLong(authentication.getName());
		log.info("User exiting queue - queueId: {}, userId: {}", queueId, userId);

		queueService.exitQueue(queueId, userId);
		return ResponseEntity.ok().build();
	}

	/**
	 * 대기열 통계 조회
	 * GET /api/queues/{queueId}/statistics
	 */
	@Operation(summary = "대기열 통계 조회", description = "대기열의 통계 정보를 조회합니다. 인증이 필요하지 않습니다.")
	@GetMapping("/{queueId}/statistics")
	public ResponseEntity<QueueStatusRes> getQueueStatistics(
		@Parameter(description = "대기열 ID", example = "1")
		@PathVariable Long queueId
	) {
		log.debug("Getting queue statistics - queueId: {}", queueId);

		QueueStatusRes response = queueService.getQueueStatistics(queueId);
		return ResponseEntity.ok(response);
	}
}

