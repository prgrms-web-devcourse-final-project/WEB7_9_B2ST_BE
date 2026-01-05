package com.back.b2st.domain.lottery.entry.service;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.back.b2st.domain.lottery.entry.dto.request.RegisterLotteryEntryReq;
import com.back.b2st.domain.lottery.entry.dto.response.AppliedLotteryInfo;
import com.back.b2st.domain.lottery.entry.dto.response.LotteryEntryInfo;
import com.back.b2st.domain.lottery.entry.dto.response.SectionLayoutRes;
import com.back.b2st.domain.lottery.entry.entity.LotteryEntry;
import com.back.b2st.domain.lottery.entry.error.LotteryEntryErrorCode;
import com.back.b2st.domain.lottery.entry.repository.LotteryEntryRepository;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.queue.service.QueueAccessService;
import com.back.b2st.domain.seat.grade.entity.SeatGradeType;
import com.back.b2st.domain.seat.seat.dto.response.SeatInfoRes;
import com.back.b2st.domain.seat.seat.service.SeatService;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LotteryEntryService {

	// 응모 내역 조회 기간
	private static final Period LOOKUP_PERIOD = Period.ofMonths(3);
	// 최대 응모 인원 수
	private static final int MAX_LOTTERY_ENTRY_COUNT = 4;
	private static final int PAGE_SIZE = 10;

	private final LotteryEntryRepository lotteryEntryRepository;
	private final MemberRepository memberRepository;
	private final PerformanceRepository performanceRepository;
	private final SeatService seatService;
	private final PerformanceScheduleRepository performanceScheduleRepository;

	private final QueueAccessService queueAccessService;

	/**
	 * 선택한 회차의 좌석 배치도 전달
	 */
	public List<SectionLayoutRes> getSeatLayout(Long memberId, Long performanceId) {
		queueAccessService.assertEnterable(performanceId, memberId);
		validateMember(memberId);
		Long venudId = validatePerformance(performanceId);
		List<SeatInfoRes> seatInfo = seatService.getSeatInfoByVenueId(venudId);

		return SectionLayoutRes.from(seatInfo);
	}

	/**
	 * 추첨 응모 등록
	 */
	public LotteryEntryInfo createLotteryEntry(Long memberId, Long performanceId, RegisterLotteryEntryReq request) {
		validatePerformance(performanceId);
		validateMember(memberId);
		validateSchedule(request.scheduleId(), performanceId);
		validateEntryData(request);
		validateEntryNotDuplicated(memberId, performanceId, request.scheduleId());

		SeatGradeType grade = SeatGradeType.fromString(request.grade());

		LotteryEntry lotteryEntry = LotteryEntry.builder()
			.memberId(memberId)
			.performanceId(performanceId)
			.scheduleId(request.scheduleId())
			.grade(grade)
			.quantity(request.quantity())
			.build();

		try {
			return LotteryEntryInfo.from(lotteryEntryRepository.save(lotteryEntry));
		} catch (DataAccessException e) {
			throw new BusinessException(LotteryEntryErrorCode.CREATE_ENTRY_FAILED);
		}
	}

	/**
	 * 내 응모 내역 조회
	 */
	public Slice<AppliedLotteryInfo> getMyLotteryEntry(Long memberId, int page) {
		validateMember(memberId);
		LocalDateTime fromDate = LocalDateTime.now().minus(LOOKUP_PERIOD);

		Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));

		return lotteryEntryRepository.findAppliedLotteryByMemberId(memberId, fromDate, pageable);
	}

	/**
	 * 공연장 검증
	 */
	private Long validatePerformance(Long performanceId) {
		Performance performance = performanceRepository.findById(performanceId)
			.orElseThrow(() -> new BusinessException(LotteryEntryErrorCode.PERFORMANCE_NOT_FOUND));

		return performance.getVenue().getVenueId();
	}

	/**
	 * 고객 검증
	 */
	private void validateMember(Long memberId) {
		if (!memberRepository.existsById(memberId)) {
			throw new BusinessException(LotteryEntryErrorCode.MEMBER_NOT_FOUND);
		}
	}

	/**
	 * 공연, 회차 검증
	 */
	private void validateSchedule(Long scheduleId, Long performanceId) {
		if (!(performanceScheduleRepository.existsByPerformanceAndScheduleMatch(
			scheduleId, performanceId, BookingType.LOTTERY))) {
			throw new BusinessException(LotteryEntryErrorCode.SCHEDULE_NOT_FOUND);
		}
	}

	/**
	 * 응모 인원 검증
	 */
	private void validateEntryData(RegisterLotteryEntryReq requset) {
		if (requset.quantity() > MAX_LOTTERY_ENTRY_COUNT) {
			throw new BusinessException(LotteryEntryErrorCode.EXCEEDS_MAX_ALLOCATION);
		}
	}

	/**
	 * 기등록 검증
	 */
	private void validateEntryNotDuplicated(Long memberId, Long performanceId, Long secheduleId) {
		if (lotteryEntryRepository.existsByMemberIdAndPerformanceIdAndScheduleId(memberId, performanceId,
			secheduleId)) {
			throw new BusinessException(LotteryEntryErrorCode.DUPLICATE_ENTRY);
		}
	}
}
