package com.back.b2st.domain.queue.controller;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.queue.dto.request.CreateQueueReq;
import com.back.b2st.domain.queue.dto.request.UpdateQueueReq;
import com.back.b2st.domain.queue.dto.response.QueueRes;
import com.back.b2st.domain.queue.dto.response.QueueStatisticsRes;
import com.back.b2st.domain.queue.service.QueueManagementService;
import com.back.b2st.domain.queue.service.QueueService;
import com.back.b2st.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin Queue Controller
 *
 * 대기열 관리자용 REST API
 * - 관리자 권한(ROLE_ADMIN) 필요
 */
@RestController
@RequestMapping("/api/admin/queues")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
@Tag(name = "AdminQueueController", description = "대기열 관리 API (관리자 전용)")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminQueueController {

	private final QueueService queueService;
	private final QueueManagementService queueManagementService;

	/**
	 * 대기열 생성
	 * POST /api/admin/queues
	 */
	@Operation(
		summary = "대기열 생성",
		description = "새로운 대기열을 생성합니다. scheduleId와 queueType 조합은 중복될 수 없습니다."
	)
	@PostMapping
	public BaseResponse<QueueRes> createQueue(
		@Valid @RequestBody CreateQueueReq request
	) {
		log.info("Admin creating queue - scheduleId: {}, queueType: {}",
			request.scheduleId(), request.queueType());
		QueueRes response = queueManagementService.createQueue(request);
		return BaseResponse.created(response);
	}

	/**
	 * 대기열 목록 조회 (필터링)
	 * GET /api/admin/queues - 전체 목록 조회
	 * GET /api/admin/queues?scheduleId=1 - 회차별 조회
	 * GET /api/admin/queues?queueType=BOOKING_ORDER - 타입별 조회
	 */
	@Operation(
		summary = "대기열 목록 조회",
		description = "전체 대기열 목록을 조회합니다. scheduleId 또는 queueType으로 필터링할 수 있습니다."
	)
	@GetMapping
	public BaseResponse<List<QueueRes>> getQueues(
		@Parameter(description = "공연 회차 ID (선택)", example = "1")
		@RequestParam(required = false) Long scheduleId,
		@Parameter(description = "대기열 타입 (선택)", example = "BOOKING_ORDER")
		@RequestParam(required = false) String queueType
	) {
		if (scheduleId != null) {
			log.debug("Admin getting queues by schedule - scheduleId: {}", scheduleId);
			return BaseResponse.success(queueManagementService.getQueuesBySchedule(scheduleId));
		}

		if (queueType != null && !queueType.isBlank()) {
			log.debug("Admin getting queues by type - queueType: {}", queueType);
			return BaseResponse.success(queueManagementService.getQueuesByType(queueType));
		}

		log.debug("Admin getting all queues");
		return BaseResponse.success(queueManagementService.getAllQueues());
	}

	/**
	 * 대기열 상세 조회
	 * GET /api/admin/queues/{queueId}
	 */
	@Operation(
		summary = "대기열 상세 조회",
		description = "대기열의 상세 정보를 조회합니다. Redis 기반 실시간 대기/입장 가능 인원 수가 포함됩니다."
	)
	@GetMapping("/{queueId}")
	public BaseResponse<QueueRes> getQueue(
		@Parameter(description = "대기열 ID", example = "1")
		@PathVariable Long queueId
	) {
		log.debug("Admin getting queue - queueId: {}", queueId);
		QueueRes response = queueManagementService.getQueue(queueId);
		return BaseResponse.success(response);
	}

	/**
	 * 대기열 설정 수정
	 * PATCH /api/admin/queues/{queueId}
	 */
	@Operation(
		summary = "대기열 설정 수정",
		description = "대기열 설정(maxActiveUsers, entryTtlMinutes)을 부분 수정합니다."
	)
	@PatchMapping("/{queueId}")
	public BaseResponse<QueueRes> updateQueue(
		@Parameter(description = "대기열 ID", example = "1")
		@PathVariable Long queueId,
		@Valid @RequestBody UpdateQueueReq request
	) {
		log.info("Admin updating queue - queueId: {}, maxActiveUsers: {}, entryTtlMinutes: {}",
			queueId, request.maxActiveUsers(), request.entryTtlMinutes());
		QueueRes response = queueManagementService.updateQueue(queueId, request);
		return BaseResponse.success(response);
	}

	/**
	 * 대기열 삭제
	 * DELETE /api/admin/queues/{queueId}
	 */
	@Operation(
		summary = "대기열 삭제",
		description = "대기열을 삭제합니다. Redis 데이터도 함께 정리됩니다."
	)
	@DeleteMapping("/{queueId}")
	public BaseResponse<Void> deleteQueue(
		@Parameter(description = "대기열 ID", example = "1")
		@PathVariable Long queueId
	) {
		log.info("Admin deleting queue - queueId: {}", queueId);
		queueManagementService.deleteQueue(queueId);
		return BaseResponse.success(null);
	}

	/**
	 * 대기열 통계 조회
	 * GET /api/admin/queues/{queueId}/statistics
	 */
	@Operation(
		summary = "대기열 통계 조회",
		description = "대기열의 전체 통계 정보를 조회합니다. Redis 기반 실시간 카운트 + DB 기반 상태별 통계가 포함됩니다."
	)
	@GetMapping("/{queueId}/statistics")
	public BaseResponse<QueueStatisticsRes> getQueueStatistics(
		@Parameter(description = "대기열 ID", example = "1")
		@PathVariable Long queueId
	) {
		log.debug("Admin getting queue statistics - queueId: {}", queueId);
		QueueStatisticsRes response = queueService.getQueueStatisticsForAdmin(queueId);
		return BaseResponse.success(response);
	}
}