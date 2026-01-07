package com.back.b2st.domain.queue.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.queue.dto.response.QueuePositionRes;
import com.back.b2st.domain.queue.dto.response.StartBookingRes;
import com.back.b2st.domain.queue.service.QueueService;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.global.util.SecurityUtils;
import com.back.b2st.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Queue 관련 API 컨트롤러
 *
 * 사용자의 대기열 진입, 위치 조회, 권한 소진 등을 담당합니다.
 * 모든 엔드포인트는 로그인이 필요합니다.
 */
@RestController
@RequestMapping("/api/queues")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
@Tag(name = "QueueController", description = "대기열 API (로그인 필수)")
@SecurityRequirement(name = "BearerAuth")
public class QueueController {

	private final QueueService queueService;

	/**
	 * 예매 확정 후 권한 소진
	 *
	 * 사용자가 예매(좌석/결제)를 모두 완료한 후 호출합니다.
	 * ENTERABLE 상태의 권한을 COMPLETED로 변경하여 소진 처리합니다.
	 *
	 * @param queueId 대기열 ID
	 * @param principal 로그인한 사용자 정보
	 * @return 성공 응답
	 */
	@Operation(
		summary = "예매 확정 후 권한 소진",
		description = "예매(좌석 선택, 결제)를 모두 완료한 사용자의 ENTERABLE 권한을 COMPLETED 상태로 변경하여 소진 처리합니다. " +
			"이후 해당 사용자는 동일 공연 대기열에 다시 진입 가능합니다."
	)
	@PostMapping("/{queueId}/complete")
	public BaseResponse<Void> completeEntry(
		@Parameter(description = "대기열 ID", example = "1")
		@PathVariable @Positive Long queueId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		Long userId = SecurityUtils.requireUserId(principal);
		log.info("User completing booking (권한 소진) - queueId: {}, userId: {}", queueId, userId);
		queueService.completeEntry(queueId, userId);
		return BaseResponse.success(null);
	}

	/**
	 * 대기열 나가기
	 *
	 * 사용자가 대기 중이거나 입장 가능한 상태에서 대기열을 포기할 때 호출합니다.
	 * WAITING 또는 ENTERABLE 상태를 EXPIRED로 변경하고 Redis에서 제거합니다.
	 *
	 * @param queueId 대기열 ID
	 * @param principal 로그인한 사용자 정보
	 * @return 성공 응답
	 */
	@Operation(
		summary = "대기열 나가기",
		description = "대기 중(WAITING) 또는 입장 가능(ENTERABLE) 상태를 EXPIRED로 변경하고 Redis에서 제거합니다."
	)
	@DeleteMapping("/{queueId}/exit")
	public BaseResponse<Void> exitQueue(
		@Parameter(description = "대기열 ID", example = "1")
		@PathVariable @Positive Long queueId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		Long userId = SecurityUtils.requireUserId(principal);
		log.info("User exiting queue - queueId: {}, userId: {}", queueId, userId);
		queueService.exitQueue(queueId, userId);
		return BaseResponse.success(null);
	}

	/**
	 * 내 대기 위치 조회
	 *
	 * 현재 사용자의 대기열 내 위치, 상태, 앞의 인원 수를 조회합니다.
	 * Redis 실시간 데이터를 기반으로 합니다.
	 *
	 * @param queueId 대기열 ID
	 * @param principal 로그인한 사용자 정보
	 * @return 대기 위치 정보 (상태, 랭크, 앞 인원 수)
	 */
	@Operation(
		summary = "대기 위치 조회",
		description = "현재 사용자의 대기열 상태(WAITING/ENTERABLE/EXPIRED/COMPLETED), 랭크, 앞의 인원 수를 조회합니다."
	)
	@GetMapping("/{queueId}/position")
	public BaseResponse<QueuePositionRes> getMyPosition(
		@Parameter(description = "대기열 ID", example = "1")
		@PathVariable @Positive Long queueId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		Long userId = SecurityUtils.requireUserId(principal);
		log.debug("User checking queue position - queueId: {}, userId: {}", queueId, userId);
		QueuePositionRes response = queueService.getMyPosition(queueId, userId);
		return BaseResponse.success(response);
	}

	/**
	 * 예매 시작: scheduleId로 대기열 자동 생성 및 입장 (Idempotent)
	 *
	 * 공연 상세 페이지에서 예매하기 버튼 클릭 시 호출합니다.
	 *
	 * **Idempotent 동작:**
	 * - 이미 WAITING/ENTERABLE 상태면 409 대신 현재 상태 반환 (HTTP 201)
	 * - 프론트는 응답의 entry.status로 화면 렌더링 가능
	 * - 재시도/새로고침이 자주 일어나도 안전하게 처리됨
	 *
	 * **처리 과정:**
	 * 1. scheduleId → performanceId 변환
	 * 2. 공연 단위 큐 자동 생성/조회 (멱등성 보장, 레이스 컨디션 방어)
	 * 3. 이미 WAITING/ENTERABLE 상태면 현재 상태 반환
	 * 4. 아니면 새로운 입장 처리
	 *
	 * @param scheduleId 공연 회차 ID (프론트 UX용 진입 정보)
	 * @param principal 로그인한 사용자 정보
	 * @return 예매 시작 응답 (queueId, performanceId, scheduleId, entry 포함)
	 */
	@Operation(
		summary = "예매 시작 (Idempotent)",
		description = "공연 회차 ID로 대기열을 자동 생성하고 입장합니다. " +
			"대기열이 없으면 자동으로 생성됩니다. " +
			"공연 단위로 큐가 관리됩니다. " +
			"이미 대기 중이거나 입장 가능한 상태면 현재 상태를 반환합니다 (409 대신 201)."
	)
	@PostMapping("/start-booking/{scheduleId}")
	public BaseResponse<StartBookingRes> startBooking(
		@Parameter(description = "공연 회차 ID (프론트 UX용 진입 정보)", example = "1")
		@PathVariable @Positive Long scheduleId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		Long userId = SecurityUtils.requireUserId(principal);
		log.info("User starting booking - scheduleId: {}, userId: {}", scheduleId, userId);
		StartBookingRes response = queueService.startBooking(scheduleId, userId);
		return BaseResponse.created(response);
	}
}