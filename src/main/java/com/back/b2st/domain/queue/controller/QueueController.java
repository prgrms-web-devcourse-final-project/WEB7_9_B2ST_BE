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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Queue Controller
 */
@RestController
@RequestMapping("/api/queues")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
public class QueueController {

	private final QueueService queueService;

	/**
	 * 대기열 입장
	 * POST /api/queues/{queueId}/enter
	 */
	@PostMapping("/{queueId}/enter")
	public ResponseEntity<QueueEntryRes> enterQueue(
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
	@GetMapping("/{queueId}/status")
	public ResponseEntity<QueueStatusRes> getMyStatus(
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
	@PostMapping("/{queueId}/complete")
	public ResponseEntity<Void> completeEntry(
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
	@DeleteMapping("/{queueId}/exit")
	public ResponseEntity<Void> exitQueue(
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
	@GetMapping("/{queueId}/statistics")
	public ResponseEntity<QueueStatusRes> getQueueStatistics(
		@PathVariable Long queueId
	) {
		log.debug("Getting queue statistics - queueId: {}", queueId);

		QueueStatusRes response = queueService.getQueueStatistics(queueId);
		return ResponseEntity.ok(response);
	}
}

