package com.back.b2st.domain.lottery.entry.service;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.back.b2st.domain.lottery.constants.LotteryConstants;
import com.back.b2st.domain.lottery.entry.dto.request.RegisterLotteryEntryReq;
import com.back.b2st.domain.lottery.entry.dto.response.LotteryEntryInfo;
import com.back.b2st.domain.lottery.entry.dto.response.SeatLayoutRes;
import com.back.b2st.domain.lottery.entry.entity.LotteryEntry;
import com.back.b2st.domain.lottery.entry.error.LotteryEntryErrorCode;
import com.back.b2st.domain.lottery.entry.repository.LotteryEntryRepository;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LotteryEntryService {

	private final LotteryEntryRepository lotteryEntryRepository;
	private final MemberRepository memberRepository;

	// 선택한 회차의 좌석 배치도 전달
	public SeatLayoutRes getSeatLayout(Long performanceId) {
		validatePerformance(performanceId);
		// TODO : performanceId로 구역 정보 조회
		// 공연 ID -> 공연장 ID 조회 -> Seat테이블에서 sectionName을 전부 가져오기
		return SeatLayoutRes.fromEntity(1L, "A", "8", "7");    // A 구역 8번 7번
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
			return new LotteryEntryInfo(lotteryEntryRepository.save(lotteryEntry));
		} catch (DataAccessException e) {
			throw new BusinessException(LotteryEntryErrorCode.CREATE_ENTRY_FAILED);
		}
	}

	private void validatePerformance(Long performanceId) {
		// TODO : 공연 Repo 연결
		if (false) {
			throw new BusinessException(LotteryEntryErrorCode.INVALID_PERFORMANCE_INFO);
		}
	}

	private void validateMember(Long memberId) {
		if (!memberRepository.existsById(memberId)) {
			throw new BusinessException(LotteryEntryErrorCode.INVALID_MEMBER_INFO);
		}
	}

	private void validateSchedule(Long scheduleId, Long performanceId) {
		// TODO: 회차정보 Repo 연결
		// requset.scheduleId() + 공연 ID 확인
		if (false) {
			throw new BusinessException(LotteryEntryErrorCode.INVALID_SCHEDULE_INFO);
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
