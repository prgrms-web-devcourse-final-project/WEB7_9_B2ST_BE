package com.back.b2st.domain.lottery.entry.service;

import org.springframework.stereotype.Service;

import com.back.b2st.domain.lottery.entry.dto.response.SeatLayoutRes;
import com.back.b2st.domain.lottery.entry.repository.LotteryEntryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LotteryEntryService {

	private final LotteryEntryRepository lotteryEntryRepository;

	// 선택한 회차의 좌석 배치도 전달
	// TODO: /performances/{performanceId}/lottery/section
	public SeatLayoutRes getSeatLayout(Long performanceId) {
		// TODO : performanceId로 구역 정보 조회
		// 공연 ID -> 공연장 ID 조회 -> Seat테이블에서 sectionName을 전부 가져오기
		return SeatLayoutRes.fromEntity(1L, "A", "8", "7");    // A 구역 8번 7번
	}

}
