package com.back.b2st.domain.lottery.draw;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.back.b2st.domain.lottery.entry.repository.LotteryEntryRepository;
import com.back.b2st.domain.performanceschedule.dto.DrawTargetDto;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DrawService {

	private final PerformanceScheduleRepository performanceScheduleRepository;
	private final LotteryEntryRepository lotteryEntryRepository;

	public void executeDraws() {
		log.info("추첨 시작");

		List<DrawTargetDto> targetPerformances = findBookingClosedPerformances();

		log.info("추첨 대상 공연 수 : {}", targetPerformances.size());

		// 각 공연별 추첨
		for (DrawTargetDto performance : targetPerformances) {
			try {
				drawForPerformance(performance.performanceScheduleId(), performance.performanceId());
				log.info("공연 추첨 완료 - scheduleId: {}", performance.performanceScheduleId());
			} catch (Exception e) {
				log.error("공연 추첨 실패 - scheduleId: {}", performance.performanceScheduleId(), e);
				// todo 실패 공연 따로 저장 후 재시도 진행?
			}
		}
	}

	/**
	 * 마감 공연 조회
	 * @return DrawTargetDto = 공연id, 회차id
	 */
	private List<DrawTargetDto> findBookingClosedPerformances() {
		LocalDateTime startDate = LocalDate.now().minusDays(1).atStartOfDay();
		LocalDateTime endDate = LocalDate.now().atStartOfDay();

		return performanceScheduleRepository.findByClosedBetweenAndNotDrawn(startDate, endDate);
	}

	/**
	 * 공연 추첨 진행
	 * @param performanceId    공연 id
	 * @param performanceScheduleId    회차 id
	 */
	private void drawForPerformance(Long performanceId, Long performanceScheduleId) {

	}
}
