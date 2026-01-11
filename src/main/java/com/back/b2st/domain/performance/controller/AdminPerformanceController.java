package com.back.b2st.domain.performance.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.performance.dto.request.CreatePerformanceReq;
import com.back.b2st.domain.performance.dto.request.CreatePresignedUrlReq;
import com.back.b2st.domain.performance.dto.request.UpsertBookingPolicyReq;
import com.back.b2st.domain.performance.dto.response.PerformanceCursorPageRes;
import com.back.b2st.domain.performance.dto.response.PerformanceDetailRes;
import com.back.b2st.domain.performance.service.PerformanceService;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.global.s3.dto.response.PresignedUrlRes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/performances")
@Tag(name = "AdminPerformanceController", description = "공연 관리 API (관리자 전용)")
@SecurityRequirement(name = "BearerAuth")
public class AdminPerformanceController {

	private final PerformanceService performanceService;

	/**
	 * 공연 생성 (관리자)
	 * POST /api/admin/performances
	 */
	@Operation(summary = "공연 생성", description = "새로운 공연을 생성합니다. 생성 즉시 ACTIVE 상태로 노출되며, 예매 정책은 별도로 설정해야 합니다.")
	@PostMapping
	public BaseResponse<PerformanceDetailRes> createPerformance(
		@Valid @RequestBody CreatePerformanceReq request) {
		return BaseResponse.created(performanceService.createPerformance(request));
	}

	/**
	 * 예매 정책 설정 (관리자)
	 * PUT /api/admin/performances/{performanceId}/booking-policy
	 */
	@Operation(summary = "예매 정책 설정", description = "공연의 예매 오픈/마감 시간을 설정합니다. ENDED 상태의 공연은 설정할 수 없습니다.")
	@PutMapping("/{performanceId}/booking-policy")
	public BaseResponse<Void> updateBookingPolicy(
		@Parameter(description = "공연 ID", example = "1") @PathVariable Long performanceId,
		@Valid @RequestBody UpsertBookingPolicyReq request) {
		performanceService.updateBookingPolicy(performanceId, request);
		return BaseResponse.success(null);
	}

	/**
	 * 공연 목록 조회 (관리자용: 상태 무관) - Cursor 기반 페이징
	 * GET /api/admin/performances?cursor=123&size=20
	 */
	@Operation(summary = "공연 목록 조회", description = "모든 공연 목록을 Cursor 기반으로 조회합니다. 상태(ACTIVE/ENDED)와 무관하게 조회됩니다. cursor는 마지막으로 조회한 공연 ID입니다.")
	@GetMapping
	public BaseResponse<PerformanceCursorPageRes> getPerformancesForAdmin(
		@Parameter(description = "마지막으로 조회한 공연 ID (첫 조회 시 생략)", example = "123") @RequestParam(required = false) Long cursor,
		@Parameter(description = "가져올 개수", example = "20") @RequestParam(defaultValue = "20") int size) {
		return BaseResponse.success(performanceService.getPerformancesForAdminWithCursor(cursor, size));
	}

	/**
	 * 공연 검색 (관리자용: 상태 무관) - Cursor 기반 페이징
	 * GET /api/admin/performances/search?keyword=뮤지컬&cursor=123&size=20
	 * - keyword null/blank면 전체 목록으로 처리(서비스에서 분기)
	 */
	@Operation(summary = "공연 검색", description = "키워드로 공연을 Cursor 기반으로 검색합니다. 제목 또는 장르에서 검색됩니다. 키워드가 없으면 전체 목록을 반환합니다.")
	@GetMapping("/search")
	public BaseResponse<PerformanceCursorPageRes> searchPerformancesForAdmin(
		@Parameter(description = "검색 키워드", example = "뮤지컬") @RequestParam(required = false) String keyword,
		@Parameter(description = "마지막으로 조회한 공연 ID (첫 조회 시 생략)", example = "123") @RequestParam(required = false) Long cursor,
		@Parameter(description = "가져올 개수", example = "20") @RequestParam(defaultValue = "20") int size) {
		return BaseResponse.success(performanceService.searchPerformancesForAdminWithCursor(cursor, keyword, size));
	}

	/**
	 * 공연 상세 조회 (관리자용: 상태 무관)
	 * GET /api/admin/performances/{performanceId}
	 */
	@Operation(summary = "공연 상세 조회", description = "공연의 상세 정보를 조회합니다. 상태와 무관하게 조회됩니다.")
	@GetMapping("/{performanceId}")
	public BaseResponse<PerformanceDetailRes> getPerformanceForAdmin(
		@Parameter(description = "공연 ID", example = "1") @PathVariable Long performanceId) {
		return BaseResponse.success(performanceService.getPerformanceForAdmin(performanceId));
	}

	/**
	 * 공연 삭제 (관리자)
	 * DELETE /api/admin/performances/{performanceId}
	 */
	@Operation(summary = "공연 삭제", description = "개발 환경의 테스트 데이터 정리용 삭제 API입니다. 운영(prod) 환경에서는 409를 반환합니다.")
	@DeleteMapping("/{performanceId}")
	public BaseResponse<Void> deletePerformance(
		@Parameter(description = "공연 ID", example = "1") @PathVariable Long performanceId) {
		performanceService.deletePerformance(performanceId);
		return BaseResponse.success(null);
	}

	/**
	 * 포스터 이미지 업로드용 Presigned URL 발급 (관리자)
	 * POST /api/admin/performances/poster/presign
	 */
	@Operation(summary = "포스터 이미지 업로드용 Presigned URL 발급", description =
		"S3에 포스터 이미지를 직접 업로드하기 위한 Presigned URL을 발급합니다. "
			+
			"허용된 이미지 형식: image/jpeg, image/png, image/webp. 최대 파일 크기: 10MB.")
	@PostMapping("/poster/presign")
	public BaseResponse<PresignedUrlRes> generatePresignedUrl(
		@Valid @RequestBody CreatePresignedUrlReq request) {
		return BaseResponse.success(performanceService.generatePosterPresign(
			request.contentType(),
			request.fileSize()));
	}
}
