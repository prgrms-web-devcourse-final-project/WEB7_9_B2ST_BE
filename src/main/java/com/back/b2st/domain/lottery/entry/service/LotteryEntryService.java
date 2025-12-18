package com.back.b2st.domain.lottery.entry.service;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.back.b2st.domain.lottery.constants.LotteryConstants;
import com.back.b2st.domain.lottery.entry.dto.request.RegisterLotteryEntryReq;
import com.back.b2st.domain.lottery.entry.dto.response.LotteryEntryInfo;
import com.back.b2st.domain.lottery.entry.dto.response.SectionLayoutRes;
import com.back.b2st.domain.lottery.entry.entity.LotteryEntry;
import com.back.b2st.domain.lottery.entry.error.LotteryEntryErrorCode;
import com.back.b2st.domain.lottery.entry.repository.LotteryEntryRepository;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
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

	// 선택한 회차의 좌석 배치도 전달
	public List<SectionLayoutRes> getSeatLayout(Long performanceId) {
		Long venudId = validatePerformance(performanceId);
		List<SeatInfoRes> seatInfo = seatService.getSeatInfoByVenueId(venudId);

		return SectionLayoutRes.from(seatInfo);
	}

	// 추첨 응모 등록
	public LotteryEntryInfo createLotteryEntry(Long performanceId, RegisterLotteryEntryReq requset) {
		validatePerformance(performanceId);
		validateMember(requset.memberId());
		validateSchedule(requset.scheduleId(), performanceId);
		validateEntryData(requset);
		validateEntryNotDuplicated(requset.memberId(), performanceId, requset.scheduleId());

		LotteryEntry lotteryEntry = LotteryEntry.builder()
			.memberId(requset.memberId())
			.performanceId(performanceId)
			.scheduleId(requset.scheduleId())
			.seatGradeId(requset.seatGradeId())
			.quantity(requset.quantity())
			.build();

		try {
			return LotteryEntryInfo.from(lotteryEntryRepository.save(lotteryEntry));
		} catch (DataAccessException e) {
			throw new BusinessException(LotteryEntryErrorCode.CREATE_ENTRY_FAILED);
		}
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
		// TODO: 인입 데이터 검증
		// requset.seatGradeId()
		if (false) {
			throw new BusinessException(LotteryEntryErrorCode.INVALID_GRADE_INFO);
		}

		// TODO: 최대 응모인원 수 관련내용 없음
		if (requset.quantity() > LotteryConstants.MAX_LOTTERY_ENTRY_COUNT) {
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
