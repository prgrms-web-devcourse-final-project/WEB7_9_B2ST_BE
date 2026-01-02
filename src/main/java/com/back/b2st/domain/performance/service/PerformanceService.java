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
import com.back.b2st.global.s3.dto.response.PresignedUrlRes;
import com.back.b2st.domain.performance.entity.PerformanceStatus;
import com.back.b2st.domain.performance.error.PerformanceErrorCode;
import com.back.b2st.domain.performance.mapper.PerformanceMapper;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.domain.venue.venue.repository.VenueRepository;
import com.back.b2st.global.error.exception.BusinessException;
import com.back.b2st.global.s3.service.S3Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceService {

	private final PerformanceRepository performanceRepository;
	private final VenueRepository venueRepository;
	private final PerformanceMapper performanceMapper;
	private final S3Service s3Service;

	/* =========================
	 * 관리자 기능
	 * ========================= */

	private static final String POSTER_PREFIX = "performances/posters";

	/**
	 * 포스터 이미지 업로드용 Presigned URL 발급 (관리자)
	 * S3 Presigned URL을 생성하고 검증하여 반환합니다.
	 *
	 * 도메인에서 prefix를 결정하여 global 레이어로 전달합니다.
	 * 이렇게 하면 확장성과 유지보수성이 향상됩니다.
	 */
	public PresignedUrlRes generatePosterPresign(String contentType, long fileSize) {
		return s3Service.generatePresignedUrl(POSTER_PREFIX, contentType, fileSize);
	}

	@Transactional
	public PerformanceDetailRes createPerformance(CreatePerformanceReq request) {
		if (!request.startDate().isBefore(request.endDate())) {
			throw new BusinessException(PerformanceErrorCode.INVALID_PERFORMANCE_PERIOD);
		}

		Venue venue = venueRepository.findById(request.venueId())
			.orElseThrow(() -> new BusinessException(PerformanceErrorCode.VENUE_NOT_FOUND));

		// DB에는 posterKey만 저장, 응답에서 URL 조립
		// 저장 단계에서 정규화 수행 (선행 '/' 제거)
		String posterKey = normalizePosterKey(request.posterKey());

		Performance performance = Performance.builder()
			.venue(venue)
			.title(request.title())
			.category(request.category())
			.posterKey(posterKey) // DB에는 posterKey만 저장 (PerformanceMapper에서 URL로 변환)
			.description(blankToNull(request.description()))
			.startDate(request.startDate())
			.endDate(request.endDate())
			.status(PerformanceStatus.ACTIVE)
			.bookingOpenAt(null)
			.bookingCloseAt(null)
			.build();

		Performance saved = performanceRepository.save(performance);
		return performanceMapper.toDetailRes(saved, LocalDateTime.now(), null);
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
		LocalDateTime endDate = performance.getEndDate();

		// 1. openAt < closeAt (closeAt이 있을 때)
		if (closeAt != null && !openAt.isBefore(closeAt)) {
			throw new BusinessException(PerformanceErrorCode.INVALID_BOOKING_TIME);
		}

		// 2. openAt <= endDate
		if (openAt.isAfter(endDate)) {
			throw new BusinessException(PerformanceErrorCode.INVALID_BOOKING_TIME);
		}

		// 3. closeAt <= endDate (closeAt이 있을 때)
		if (closeAt != null && closeAt.isAfter(endDate)) {
			throw new BusinessException(PerformanceErrorCode.INVALID_BOOKING_TIME);
		}

		performance.updateBookingPolicy(openAt, closeAt);
	}

	// 관리자: Offset 목록
	public Page<PerformanceListRes> getPerformancesForAdmin(Pageable pageable) {
		LocalDateTime now = LocalDateTime.now();
		return performanceRepository.findAll(pageable)
			.map(p -> performanceMapper.toListRes(p, now));
	}

	// 관리자: Offset 검색
	public Page<PerformanceListRes> searchPerformancesForAdmin(String keyword, Pageable pageable) {
		LocalDateTime now = LocalDateTime.now();
		if (keyword == null || keyword.trim().isEmpty()) {
			return performanceRepository.findAll(pageable)
				.map(p -> performanceMapper.toListRes(p, now));
		}
		return performanceRepository.searchAll(keyword.trim(), pageable)
			.map(p -> performanceMapper.toListRes(p, now));
	}

	// 관리자: 상세
	public PerformanceDetailRes getPerformanceForAdmin(Long performanceId) {
		return performanceRepository.findWithVenueByPerformanceId(performanceId)
			.map(p -> performanceMapper.toDetailRes(p, LocalDateTime.now(), null))
			.orElseThrow(() -> new BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));
	}

	/* =========================
	 * 사용자 기능
	 * ========================= */

	// 사용자: Offset 목록
	public Page<PerformanceListRes> getActivePerformances(Pageable pageable) {
		LocalDateTime now = LocalDateTime.now();
		return performanceRepository.findByStatus(PerformanceStatus.ACTIVE, pageable)
			.map(p -> performanceMapper.toListRes(p, now));
	}

	// 사용자: 상세
	public PerformanceDetailRes getActivePerformance(Long performanceId) {
		return performanceRepository.findWithVenueByPerformanceIdAndStatus(performanceId, PerformanceStatus.ACTIVE)
			.map(p -> performanceMapper.toDetailRes(p, LocalDateTime.now(), null))
			.orElseThrow(() -> new BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));
	}

	// 사용자: Offset 검색
	public Page<PerformanceListRes> searchActivePerformances(String keyword, Pageable pageable) {
		LocalDateTime now = LocalDateTime.now();
		if (keyword == null || keyword.trim().isEmpty()) {
			return performanceRepository.findByStatus(PerformanceStatus.ACTIVE, pageable)
				.map(p -> performanceMapper.toListRes(p, now));
		}
		return performanceRepository.searchActive(PerformanceStatus.ACTIVE, keyword.trim(), pageable)
			.map(p -> performanceMapper.toListRes(p, now));
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
			.map(p -> performanceMapper.toListRes(p, now))
			.toList();

		return PerformanceCursorPageRes.of(content, size);
	}

	private String blankToNull(String v) {
		if (v == null) return null;
		String t = v.trim();
		return t.isEmpty() ? null : t;
	}

	/**
	 * posterKey 정규화 (저장 단계에서 수행)
	 *
	 * S3 objectKey로 사용되는 posterKey를 정규화합니다.
	 * - null/빈 문자열 처리
	 * - 선행/후행 슬래시 제거 (여러 개)
	 * - 중간 연속 슬래시 정규화
	 *
	 * Service 레이어에서 확실히 정규화하여 DB에 저장하므로,
	 * Mapper에서는 URL 조립만 수행하면 됩니다.
	 */
	private String normalizePosterKey(String posterKey) {
		String normalized = blankToNull(posterKey);
		if (normalized == null) {
			return null;
		}

		// 선행 슬래시 제거 (여러 개)
		while (normalized.startsWith("/")) {
			normalized = normalized.substring(1);
		}

		// 후행 슬래시 제거 (여러 개)
		while (normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}

		// 중간 연속 슬래시 정규화 (정규식 대신 단순 루프 사용 - 성능 고려)
		// 예: "performances//posters" -> "performances/posters", "////a//b///c" -> "a/b/c"
		while (normalized.contains("//")) {
			normalized = normalized.replace("//", "/");
		}

		// 정규화 후 빈 값이면 null 반환
		if (normalized.isEmpty()) {
			return null;
		}

		return normalized;
	}
}