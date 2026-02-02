package com.back.b2st.domain.performance.controller;

import com.back.b2st.domain.performance.dto.response.PerformanceCursorPageRes;
import com.back.b2st.domain.performance.dto.response.PerformanceDetailRes;
import com.back.b2st.domain.performance.service.PerformanceService;
import com.back.b2st.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/performances")
@Tag(name = "PerformanceController", description = "공연 조회 API")
public class PerformanceController {

	private final PerformanceService performanceService;

	/**
	 * 공연 목록 조회 (사용자용: ACTIVE만) - Cursor 기반 페이징
	 * GET /api/performances?cursor=123&size=20
	 */
	@Operation(
		summary = "공연 목록 조회",
		description = "활성(ACTIVE) 상태인 공연 목록을 Cursor 기반으로 조회합니다. 예매 가능 여부는 isBookable 필드로 확인할 수 있습니다. cursor는 마지막으로 조회한 공연 ID입니다."
	)
	@GetMapping
	public BaseResponse<PerformanceCursorPageRes> getPerformances(
		@Parameter(description = "마지막으로 조회한 공연 ID (첫 조회 시 생략)", example = "123")
		@RequestParam(required = false) Long cursor,
		@Parameter(description = "가져올 개수", example = "20")
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return BaseResponse.success(performanceService.getActivePerformancesWithCursor(cursor, size));
	}

	/**
	 * 공연 상세 조회 (사용자용: ACTIVE만)
	 * GET /api/performances/{performanceId}
	 */
	@Operation(summary = "공연 상세 조회", description = "활성(ACTIVE) 상태인 공연의 상세 정보를 조회합니다.")
	@GetMapping("/{performanceId}")
	public BaseResponse<PerformanceDetailRes> getPerformance(
		@Parameter(description = "공연 ID", example = "1")
		@PathVariable Long performanceId
	) {
		return BaseResponse.success(performanceService.getActivePerformance(performanceId));
	}

	/**
	 * 공연 검색 (사용자용: ACTIVE + 키워드) - Cursor 기반 페이징
	 * GET /api/performances/search?keyword=뮤지컬&cursor=123&size=20
	 * - keyword null/blank면 ACTIVE 목록으로 처리(서비스에서 분기)
	 */
	@Operation(
		summary = "공연 검색",
		description = "키워드로 활성(ACTIVE) 상태인 공연을 Cursor 기반으로 검색합니다. 제목 또는 장르에서 검색됩니다. 키워드가 없으면 활성 공연 목록을 반환합니다."
	)
	@GetMapping("/search")
	public BaseResponse<PerformanceCursorPageRes> searchPerformances(
		@Parameter(description = "검색 키워드", example = "뮤지컬")
		@RequestParam(required = false) String keyword,
		@Parameter(description = "마지막으로 조회한 공연 ID (첫 조회 시 생략)", example = "123")
		@RequestParam(required = false) Long cursor,
		@Parameter(description = "가져올 개수", example = "20")
		@RequestParam(defaultValue = "20") int size
	) {
		return BaseResponse.success(performanceService.searchActivePerformancesWithCursor(cursor, keyword, size));
	}
}
