package com.back.b2st.domain.performance.service;

import com.back.b2st.domain.performance.dto.response.PerformanceDetailRes;
import com.back.b2st.domain.performance.dto.response.PerformanceListRes;
import com.back.b2st.domain.performance.entity.PerformanceStatus;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.global.error.code.CommonErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceService {

	private final PerformanceRepository performanceRepository;

	/**
	 * 공연 목록 조회 (판매중인 공연만)
	 */
	public Page<PerformanceListRes> getOnSalePerformances(Pageable pageable) {
		return performanceRepository
				.findByStatus(PerformanceStatus.ON_SALE, pageable)
				.map(PerformanceListRes::from);
	}

	/**
	 * 공연 상세 조회 (판매중인 공연만)
	 */
	public PerformanceDetailRes getOnSalePerformance(Long performanceId) {
		return performanceRepository
				.findWithVenueByPerformanceIdAndStatus(
						performanceId,
						PerformanceStatus.ON_SALE
				)
				.map(PerformanceDetailRes::from)
				.orElseThrow(() ->
						new BusinessException(CommonErrorCode.NOT_FOUND)
				);
	}

	/**
	 * 공연 검색 (판매중 + 키워드)
	 */
	public Page<PerformanceListRes> searchOnSalePerformances(
			String keyword,
			Pageable pageable
	) {
		return performanceRepository
				.searchOnSale(PerformanceStatus.ON_SALE, keyword, pageable)
				.map(PerformanceListRes::from);
	}
}
