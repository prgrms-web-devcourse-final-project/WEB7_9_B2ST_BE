package com.back.b2st.domain.lottery.entry.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataAccessException;
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
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.seat.grade.entity.SeatGradeType;
import com.back.b2st.domain.seat.seat.dto.response.SeatInfoRes;
import com.back.b2st.domain.seat.seat.service.SeatService;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LotteryEntryService {

	private final LotteryEntryRepository lotteryEntryRepository;
	private final MemberRepository memberRepository;
	private final PerformanceRepository performanceRepository;
	private final SeatService seatService;
	private final PerformanceScheduleRepository performanceScheduleRepository;

	private static final int MAX_LOTTERY_ENTRY_COUNT = 4;
	private static final int MONTHS = 3;

	// 선택한 회차의 좌석 배치도 전달
	public List<SectionLayoutRes> getSeatLayout(Long performanceId) {
		Long venudId = validatePerformance(performanceId);
		List<SeatInfoRes> seatInfo = seatService.getSeatInfoByVenueId(venudId);

		return SectionLayoutRes.from(seatInfo);
	}

	// 추첨 응모 등록
	public LotteryEntryInfo createLotteryEntry(Long performanceId, RegisterLotteryEntryReq request) {
		validatePerformance(performanceId);
		validateMember(request.memberId());
		validateSchedule(request.scheduleId(), performanceId);
		validateEntryData(request);
		validateEntryNotDuplicated(request.memberId(), performanceId, request.scheduleId());

		SeatGradeType grade = SeatGradeType.fromString(request.grade());

		LotteryEntry lotteryEntry = LotteryEntry.builder()
			.memberId(request.memberId())
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

	// 추첨 진행 전인 응모 내역 조회
	public List<AppliedLotteryInfo> getMyLotteryEntry(Long memberId) {
		validateMember(memberId);
		LocalDateTime fromMonthsAgo = LocalDateTime.now().minusMonths(MONTHS);
		return lotteryEntryRepository.findAppliedLotteryByMememberId(memberId, fromMonthsAgo);
	}

	// 공연장 검증 후 장소 확인
	private Long validatePerformance(Long performanceId) {
		Performance performance = performanceRepository.findById(performanceId)
			.orElseThrow(() -> new BusinessException(LotteryEntryErrorCode.PERFORMANCE_NOT_FOUND));

		return performance.getVenue().getVenueId();
	}

	// 고객 검증
	private void validateMember(Long memberId) {
		if (!memberRepository.existsById(memberId)) {
			throw new BusinessException(LotteryEntryErrorCode.MEMBER_NOT_FOUND);
		}
	}

	// 공연 + 회차 확인
	private void validateSchedule(Long scheduleId, Long performanceId) {
		if (!(performanceScheduleRepository.existsByPerformanceScheduleIdAndPerformance_PerformanceId(
			scheduleId, performanceId))) {
			throw new BusinessException(LotteryEntryErrorCode.SCHEDULE_NOT_FOUND);
		}
	}

	private void validateEntryData(RegisterLotteryEntryReq requset) {
		// 최대 응모인원 수 관련논의 없음
		if (requset.quantity() > MAX_LOTTERY_ENTRY_COUNT) {
			throw new BusinessException(LotteryEntryErrorCode.EXCEEDS_MAX_ALLOCATION);
		}
	}

	private void validateEntryNotDuplicated(Long memberId, Long performanceId, Long secheduleId) {
		if (lotteryEntryRepository.existsByMemberIdAndPerformanceIdAndScheduleId(memberId, performanceId,
			secheduleId)) {
			throw new BusinessException(LotteryEntryErrorCode.DUPLICATE_ENTRY);
		}
	}
}
