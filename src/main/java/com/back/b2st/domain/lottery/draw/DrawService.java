package com.back.b2st.domain.lottery.draw;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.lottery.draw.dto.LotteryApplicationInfo;
import com.back.b2st.domain.lottery.entry.repository.LotteryEntryRepository;
import com.back.b2st.domain.performanceschedule.dto.DrawTargetPerformance;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DrawService {

	private static final String P = "[DrawService] ";

	private final PerformanceScheduleRepository performanceScheduleRepository;
	private final LotteryEntryRepository lotteryEntryRepository;

	public void executeDraws() {
		log.info("추첨 시작");

		List<DrawTargetPerformance> targetPerformances = findBookingClosedPerformances();

		log.info("== [DrawService.executeDraws] 추첨 대상 공연 수 : {}", targetPerformances.size());

		// 각 공연별 추첨
		for (DrawTargetPerformance performance : targetPerformances) {
			try {
				drawForPerformance(performance.performanceScheduleId());
				log.info("== [DrawService.executeDraws] 공연 추첨 완료 - scheduleId: {}",
					performance.performanceScheduleId());
			} catch (Exception e) {
				log.error("== [DrawService.executeDraws] 공연 추첨 실패 - scheduleId: {}",
					performance.performanceScheduleId(), e);
				// todo 실패 공연 따로 저장 후 재시도 진행?
			}
		}
	}

	/**
	 * 마감 공연 조회
	 * @return DrawTargetPerformance = 공연id, 회차id
	 */
	private List<DrawTargetPerformance> findBookingClosedPerformances() {
		LocalDateTime startDate = LocalDate.now().minusDays(1).atStartOfDay();
		LocalDateTime endDate = LocalDate.now().atStartOfDay();

		return performanceScheduleRepository.findByClosedBetweenAndNotDrawn(startDate, endDate);
	}

	/**
	 * 공연 추첨 진행
	 * @param scheduleId    회차 id
	 */
	@Transactional
	protected void drawForPerformance(Long scheduleId) {
		// 응모자 목록 가져오기
		List<LotteryApplicationInfo> entryInfos = lotteryEntryRepository.findAppliedInfoByScheduleId(
			scheduleId);

		if (entryInfos.size() == 0) {
			log.info("== [DrawService.drawForPerformance] 데이터 없음");
			return;
		}
		log.info("== [DrawService.drawForPerformance] 응모자 수 : {}", entryInfos.size());
		log.debug("== [DrawService.drawForPerformance] 응모자 Id : {}, 신청 등급 : {}, 신청 인원 : {}",
			entryInfos.getFirst().id(), entryInfos.getFirst().grade(), entryInfos.getFirst().quantity());

		// 추첨
		// todo 가중치 고민

		// 등급, 인원수
		// 	Collections.shuffle(entryIds);

		// 결과 저장
		// lotteryEntry -> status WIN/LOSE 변경 필요
		// 당점자 id 가져오기
		// lotteryResult 생성 (lotteryEntryId, memberId)

	}
}
