package com.back.b2st.domain.performance.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.performance.dto.request.CreatePerformanceReq;
import com.back.b2st.domain.performance.dto.request.UpsertBookingPolicyReq;
import com.back.b2st.domain.performance.dto.response.PerformanceCursorPageRes;
import com.back.b2st.domain.performance.dto.response.PerformanceDetailRes;
import com.back.b2st.domain.performance.dto.response.PerformanceListRes;
import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.entity.PerformanceStatus;
import com.back.b2st.domain.performance.error.PerformanceErrorCode;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.domain.venue.venue.repository.VenueRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceService {

	private final PerformanceRepository performanceRepository;
	private final VenueRepository venueRepository;

	/* =========================
	 * 관리자 기능
	 * ========================= */

	@Transactional
	public PerformanceDetailRes createPerformance(CreatePerformanceReq request) {
		if (!request.startDate().isBefore(request.endDate())) {
			throw new BusinessException(PerformanceErrorCode.INVALID_PERFORMANCE_PERIOD);
		}

		Venue venue = venueRepository.findById(request.venueId())
			.orElseThrow(() -> new BusinessException(PerformanceErrorCode.VENUE_NOT_FOUND));

		Performance performance = Performance.builder()
			.venue(venue)
			.title(request.title())
			.category(request.category())
			.posterUrl(blankToNull(request.posterUrl()))
			.description(blankToNull(request.description()))
			.startDate(request.startDate())
			.endDate(request.endDate())
			.status(PerformanceStatus.ACTIVE)
			.bookingOpenAt(null)
			.bookingCloseAt(null)
			.build();

		Performance saved = performanceRepository.save(performance);
		return PerformanceDetailRes.from(saved, LocalDateTime.now(), null);
	}

	@Transactional
	public void updateBookingPolicy(Long performanceId, UpsertBookingPolicyReq request) {
		Performance performance = performanceRepository.findById(performanceId)
			.orElseThrow(() -> new BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));

		if (performance.getStatus() == PerformanceStatus.ENDED) {
			throw new BusinessException(PerformanceErrorCode.PERFORMANCE_ALREADY_ENDED);
		}

		LocalDateTime openAt = request.bookingOpenAt();
		LocalDateTime closeAt = request.bookingCloseAt();

		if (closeAt != null && !openAt.isBefore(closeAt)) {
			throw new BusinessException(PerformanceErrorCode.INVALID_BOOKING_TIME);
		}
		if (openAt.isAfter(performance.getEndDate())) {
			throw new BusinessException(PerformanceErrorCode.INVALID_BOOKING_TIME);
		}

		performance.updateBookingPolicy(openAt, closeAt);
	}

	// 관리자: Offset 목록
	public Page<PerformanceListRes> getPerformancesForAdmin(Pageable pageable) {
		LocalDateTime now = LocalDateTime.now();
		return performanceRepository.findAll(pageable)
			.map(p -> PerformanceListRes.from(p, now));
	}

	// 관리자: Offset 검색
	public Page<PerformanceListRes> searchPerformancesForAdmin(String keyword, Pageable pageable) {
		LocalDateTime now = LocalDateTime.now();
		if (keyword == null || keyword.trim().isEmpty()) {
			return performanceRepository.findAll(pageable)
				.map(p -> PerformanceListRes.from(p, now));
		}
		return performanceRepository.searchAll(keyword.trim(), pageable)
			.map(p -> PerformanceListRes.from(p, now));
	}

	// 관리자: 상세
	public PerformanceDetailRes getPerformanceForAdmin(Long performanceId) {
		return performanceRepository.findWithVenueByPerformanceId(performanceId)
			.map(p -> PerformanceDetailRes.from(p, LocalDateTime.now(), null))
			.orElseThrow(() -> new BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));
	}

	/* =========================
	 * 사용자 기능
	 * ========================= */

	// 사용자: Offset 목록
	public Page<PerformanceListRes> getActivePerformances(Pageable pageable) {
		LocalDateTime now = LocalDateTime.now();
		return performanceRepository.findByStatus(PerformanceStatus.ACTIVE, pageable)
			.map(p -> PerformanceListRes.from(p, now));
	}

	// 사용자: 상세
	public PerformanceDetailRes getActivePerformance(Long performanceId) {
		return performanceRepository.findWithVenueByPerformanceIdAndStatus(performanceId, PerformanceStatus.ACTIVE)
			.map(p -> PerformanceDetailRes.from(p, LocalDateTime.now(), null))
			.orElseThrow(() -> new BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));
	}

	// 사용자: Offset 검색
	public Page<PerformanceListRes> searchActivePerformances(String keyword, Pageable pageable) {
		LocalDateTime now = LocalDateTime.now();
		if (keyword == null || keyword.trim().isEmpty()) {
			return performanceRepository.findByStatus(PerformanceStatus.ACTIVE, pageable)
				.map(p -> PerformanceListRes.from(p, now));
		}
		return performanceRepository.searchActive(PerformanceStatus.ACTIVE, keyword.trim(), pageable)
			.map(p -> PerformanceListRes.from(p, now));
	}

	/* =========================
	 * Cursor 기반 페이징 (사용자 기능)
	 * ========================= */

	public PerformanceCursorPageRes getActivePerformancesWithCursor(Long cursor, int size) {
		Pageable pageable = PageRequest.of(0, size + 1);

		List<Performance> performances = performanceRepository
			.findByStatusWithCursor(PerformanceStatus.ACTIVE, cursor, pageable);

		return mapToCursorRes(performances, size);
	}

	public PerformanceCursorPageRes searchActivePerformancesWithCursor(Long cursor, String keyword, int size) {
		if (keyword == null || keyword.trim().isEmpty()) {
			return getActivePerformancesWithCursor(cursor, size);
		}

		Pageable pageable = PageRequest.of(0, size + 1);
		List<Performance> performances = performanceRepository
			.searchActiveWithCursor(PerformanceStatus.ACTIVE, keyword.trim(), cursor, pageable);

		return mapToCursorRes(performances, size);
	}

	/* =========================
	 * Cursor 기반 페이징 (관리자 기능)
	 * ========================= */

	public PerformanceCursorPageRes getPerformancesForAdminWithCursor(Long cursor, int size) {
		Pageable pageable = PageRequest.of(0, size + 1);

		List<Performance> performances = performanceRepository
			.findAllWithCursor(cursor, pageable);

		return mapToCursorRes(performances, size);
	}

	public PerformanceCursorPageRes searchPerformancesForAdminWithCursor(Long cursor, String keyword, int size) {
		if (keyword == null || keyword.trim().isEmpty()) {
			return getPerformancesForAdminWithCursor(cursor, size);
		}

		Pageable pageable = PageRequest.of(0, size + 1);
		List<Performance> performances = performanceRepository
			.searchAllWithCursor(keyword.trim(), cursor, pageable);

		return mapToCursorRes(performances, size);
	}

	/* =========================
	 * 공통 유틸 (Private)
	 * ========================= */

	/**
	 * Entity 리스트를 Cursor 응답 DTO로 변환
	 * (DTO of() 메서드에서 hasNext, nextCursor 계산 수행)
	 */
	private PerformanceCursorPageRes mapToCursorRes(List<Performance> performances, int size) {
		LocalDateTime now = LocalDateTime.now();
		List<PerformanceListRes> content = performances.stream()
			.map(p -> PerformanceListRes.from(p, now))
			.toList();

		return PerformanceCursorPageRes.of(content, size);
	}

	private String blankToNull(String v) {
		if (v == null) return null;
		String t = v.trim();
		return t.isEmpty() ? null : t;
	}
}