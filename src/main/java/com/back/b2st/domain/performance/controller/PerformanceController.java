package com.back.b2st.domain.performance.controller;

import com.back.b2st.domain.performance.dto.response.PerformanceDetailRes;
import com.back.b2st.domain.performance.dto.response.PerformanceListRes;
import com.back.b2st.domain.performance.service.PerformanceService;
import com.back.b2st.global.common.BaseResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/performances")
public class PerformanceController {

	private final PerformanceService performanceService;

	/**
	 * 공연 목록 조회 (판매중만)
	 */
	@GetMapping
	public BaseResponse<Page<PerformanceListRes>> getPerformances(
			@PageableDefault(size = 20) Pageable pageable
	) {
		return BaseResponse.success(performanceService.getOnSalePerformances(pageable));
	}

	/**
	 * 공연 상세 조회 (판매중만)
	 */
	@GetMapping("/{performanceId}")
	public BaseResponse<PerformanceDetailRes> getPerformance(
			@PathVariable Long performanceId
	) {
		return BaseResponse.success(performanceService.getOnSalePerformance(performanceId));
	}

	/**
	 * 공연 검색
	 */
	@GetMapping("/search")
	public BaseResponse<Page<PerformanceListRes>> searchPerformances(
			@RequestParam String keyword,
			@PageableDefault(size = 20) Pageable pageable
	) {
		return BaseResponse.success(performanceService.searchOnSalePerformances(keyword, pageable));
	}
}
