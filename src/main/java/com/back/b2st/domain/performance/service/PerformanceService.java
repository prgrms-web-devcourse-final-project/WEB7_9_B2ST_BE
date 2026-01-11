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
import com.back.b2st.domain.performance.mapper.PerformanceMapper;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.lottery.entry.repository.LotteryEntryRepository;
import com.back.b2st.domain.lottery.result.repository.LotteryResultRepository;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.prereservation.booking.repository.PrereservationBookingRepository;
import com.back.b2st.domain.prereservation.entry.repository.PrereservationRepository;
import com.back.b2st.domain.prereservation.policy.repository.PrereservationTimeTableRepository;
import com.back.b2st.domain.queue.repository.QueueRepository;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.reservation.repository.ReservationSeatRepository;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.seat.grade.entity.SeatGrade;
import com.back.b2st.domain.seat.grade.entity.SeatGradeType;
import com.back.b2st.domain.seat.grade.repository.SeatGradeRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.error.SeatErrorCode;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.ticket.repository.TicketRepository;
import com.back.b2st.domain.trade.repository.TradeRepository;
import com.back.b2st.domain.trade.repository.TradeRequestRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.domain.venue.venue.repository.VenueRepository;
import com.back.b2st.global.error.exception.BusinessException;
import com.back.b2st.global.s3.dto.response.PresignedUrlRes;
import com.back.b2st.global.s3.service.S3Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceService {

	private final PerformanceRepository performanceRepository;
	private final VenueRepository venueRepository;
	private final PerformanceScheduleRepository performanceScheduleRepository;
	private final SeatRepository seatRepository;
	private final SeatGradeRepository seatGradeRepository;
	private final ScheduleSeatRepository scheduleSeatRepository;
	private final PrereservationBookingRepository prereservationBookingRepository;
	private final PrereservationRepository prereservationRepository;
	private final PrereservationTimeTableRepository prereservationTimeTableRepository;
	private final ReservationRepository reservationRepository;
	private final ReservationSeatRepository reservationSeatRepository;
	private final TicketRepository ticketRepository;
	private final LotteryEntryRepository lotteryEntryRepository;
	private final LotteryResultRepository lotteryResultRepository;
	private final TradeRepository tradeRepository;
	private final TradeRequestRepository tradeRequestRepository;
	private final QueueRepository queueRepository;

	private final PerformanceMapper performanceMapper;
	private final S3Service s3Service;

	private static final String POSTER_PREFIX = "performances/posters";

	/*
	 * =========================
	 * 관리자 기능
	 * =========================
	 */

	/**
	 * 포스터 이미지 업로드용 Presigned PUT URL 발급 (관리자)
	 *
	 * - prefix는 도메인에서 결정
	 * - 반환되는 objectKey를 DB에 저장하여 추후 조회 시 Presigned GET으로 변환
	 */
	public PresignedUrlRes generatePosterPresign(String contentType, long fileSize) {
		return s3Service.generatePresignedUploadUrl(POSTER_PREFIX, contentType, fileSize);
	}

	@Transactional
	public PerformanceDetailRes createPerformance(CreatePerformanceReq request) {
		if (!request.startDate().isBefore(request.endDate())) {
			throw new BusinessException(PerformanceErrorCode.INVALID_PERFORMANCE_PERIOD);
		}

		Venue venue = venueRepository.findById(request.venueId())
			.orElseThrow(() -> new BusinessException(PerformanceErrorCode.VENUE_NOT_FOUND));

		// DB에는 posterKey(objectKey)만 저장 (S3 Private)
		String posterKey = normalizePosterKey(request.posterKey());

		Performance performance = Performance.builder()
			.venue(venue)
			.title(request.title())
			.category(request.category())
			.posterKey(posterKey)
			.description(blankToNull(request.description()))
			.startDate(request.startDate())
			.endDate(request.endDate())
			.status(PerformanceStatus.ACTIVE)
			.bookingType(request.bookingType())
			.bookingOpenAt(null)
			.bookingCloseAt(null)
			.build();

		Performance saved = performanceRepository.save(performance);
		createDefaultSeatGrades(saved);

		return performanceMapper.toDetailRes(saved, LocalDateTime.now(), null);
	}

	private void createDefaultSeatGrades(Performance performance) {
		Long performanceId = performance.getPerformanceId();
		Long venueId = performance.getVenue().getVenueId();

		List<Seat> seats = seatRepository.findByVenueId(venueId);
		if (seats.isEmpty()) {
			throw new BusinessException(SeatErrorCode.SEAT_NOT_FOUND);
		}

		List<SeatGrade> toCreate = seats.stream()
			.filter(seat -> !seatGradeRepository.existsByPerformanceIdAndSeatId(performanceId, seat.getId()))
			.map(seat -> {
				SeatGradeType grade = defaultGrade(seat.getSectionName());
				int price = defaultPrice(grade);

				return SeatGrade.builder()
					.performanceId(performanceId)
					.seatId(seat.getId())
					.grade(grade)
					.price(price)
					.build();
			})
			.toList();

		if (!toCreate.isEmpty()) {
			seatGradeRepository.saveAll(toCreate);
		}
	}

	private SeatGradeType defaultGrade(String sectionName) {
		if ("A".equalsIgnoreCase(sectionName))
			return SeatGradeType.VIP;
		if ("B".equalsIgnoreCase(sectionName))
			return SeatGradeType.ROYAL;
		return SeatGradeType.STANDARD;
	}

	private int defaultPrice(SeatGradeType grade) {
		return switch (grade) {
			case VIP -> 30000;
			case ROYAL -> 20000;
			default -> 10000;
		};
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

		if (closeAt != null && !openAt.isBefore(closeAt)) {
			throw new BusinessException(PerformanceErrorCode.INVALID_BOOKING_TIME);
		}

		if (openAt.isAfter(endDate)) {
			throw new BusinessException(PerformanceErrorCode.INVALID_BOOKING_TIME);
		}

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

	// 관리자: 삭제
	@Transactional
	public void deletePerformance(Long performanceId) {
		if (!performanceRepository.existsById(performanceId)) {
			throw new BusinessException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND);
		}

		List<Long> scheduleIds = performanceScheduleRepository.findIdsByPerformanceId(performanceId);
		if (!scheduleIds.isEmpty()) {
			if (reservationRepository.existsByScheduleIdIn(scheduleIds)
				|| lotteryEntryRepository.existsByScheduleIdIn(scheduleIds)
				|| prereservationRepository.existsByPerformanceScheduleIdIn(scheduleIds)
				|| prereservationBookingRepository.existsByScheduleIdIn(scheduleIds)) {
				throw new BusinessException(PerformanceErrorCode.PERFORMANCE_DELETE_NOT_ALLOWED);
			}
		}

		if (tradeRepository.existsByPerformanceId(performanceId)) {
			throw new BusinessException(PerformanceErrorCode.PERFORMANCE_DELETE_NOT_ALLOWED);
		}

		List<Long> tradeIds = tradeRepository.findIdsByPerformanceId(performanceId);
		if (!tradeIds.isEmpty()) {
			tradeRequestRepository.deleteAllByTrade_IdIn(tradeIds);
			tradeRepository.deleteAllByPerformanceId(performanceId);
		}

		queueRepository.deleteByPerformanceId(performanceId);

		if (!scheduleIds.isEmpty()) {
			List<Long> lotteryEntryIds = lotteryEntryRepository.findIdsByScheduleIdIn(scheduleIds);
			if (!lotteryEntryIds.isEmpty()) {
				lotteryResultRepository.deleteAllByLotteryEntryIdIn(lotteryEntryIds);
			}
			lotteryEntryRepository.deleteAllByScheduleIdIn(scheduleIds);

			prereservationRepository.deleteAllByPerformanceScheduleIdIn(scheduleIds);
			prereservationTimeTableRepository.deleteAllByPerformanceScheduleIdIn(scheduleIds);
			prereservationBookingRepository.deleteAllByScheduleIdIn(scheduleIds);

			List<Long> reservationIds = reservationRepository.findIdsByScheduleIdIn(scheduleIds);
			if (!reservationIds.isEmpty()) {
				reservationSeatRepository.deleteAllByReservationIdIn(reservationIds);
				ticketRepository.deleteAllByReservationIdIn(reservationIds);
			}
			reservationRepository.deleteAllByScheduleIdIn(scheduleIds);

			scheduleSeatRepository.deleteAllByScheduleIdIn(scheduleIds);

			performanceScheduleRepository.deleteAllByPerformanceId(performanceId);
		}

		seatGradeRepository.deleteAllByPerformanceId(performanceId);
		performanceRepository.deleteById(performanceId);
	}

	/*
	 * =========================
	 * 사용자 기능
	 * =========================
	 */

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

	/*
	 * =========================
	 * Cursor 기반 페이징 (사용자 기능)
	 * =========================
	 */

	public PerformanceCursorPageRes getActivePerformancesWithCursor(Long cursor, int size) {
		Pageable pageable = PageRequest.of(0, size + 1);
		LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();

		List<Performance> performances = performanceRepository
			.findByStatusWithCursor(PerformanceStatus.ACTIVE, todayStart, cursor, pageable);

		return mapToCursorRes(performances, size);
	}

	public PerformanceCursorPageRes searchActivePerformancesWithCursor(Long cursor, String keyword, int size) {
		if (keyword == null || keyword.trim().isEmpty()) {
			return getActivePerformancesWithCursor(cursor, size);
		}

		Pageable pageable = PageRequest.of(0, size + 1);
		LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();

		List<Performance> performances = performanceRepository
			.searchActiveWithCursor(PerformanceStatus.ACTIVE, todayStart, keyword.trim(), cursor, pageable);

		return mapToCursorRes(performances, size);
	}

	/*
	 * =========================
	 * Cursor 기반 페이징 (관리자 기능)
	 * =========================
	 */

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

	/*
	 * =========================
	 * 공통 유틸 (Private)
	 * =========================
	 */

	private PerformanceCursorPageRes mapToCursorRes(List<Performance> performances, int size) {
		LocalDateTime now = LocalDateTime.now();
		List<PerformanceListRes> content = performances.stream()
			.map(p -> performanceMapper.toListRes(p, now))
			.toList();

		return PerformanceCursorPageRes.of(content, size);
	}

	private String blankToNull(String v) {
		if (v == null)
			return null;
		String t = v.trim();
		return t.isEmpty() ? null : t;
	}

	/**
	 * posterKey 정규화 (저장 단계에서 수행)
	 *
	 * - null/blank 처리
	 * - 선행/후행 슬래시 제거
	 * - 중간 연속 슬래시 축약 (정규식/replace 루프 미사용)
	 */
	private String normalizePosterKey(String posterKey) {
		String s = blankToNull(posterKey);
		if (s == null)
			return null;

		int start = 0;
		int end = s.length();

		while (start < end && s.charAt(start) == '/')
			start++;
		while (start < end && s.charAt(end - 1) == '/')
			end--;

		if (start >= end)
			return null;

		StringBuilder sb = new StringBuilder(end - start);
		boolean prevSlash = false;

		for (int i = start; i < end; i++) {
			char ch = s.charAt(i);
			if (ch == '/') {
				if (prevSlash)
					continue;
				prevSlash = true;
			} else {
				prevSlash = false;
			}
			sb.append(ch);
		}

		return sb.length() == 0 ? null : sb.toString();
	}
}
