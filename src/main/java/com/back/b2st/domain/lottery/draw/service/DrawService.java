package com.back.b2st.domain.lottery.draw.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.back.b2st.domain.performanceschedule.dto.DrawTargetPerformance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DrawService {

	private final PerformanceDrawService performanceDrawService;
	private final SeatAllocationService seatAllocationService;
	private final CancelUnpaidService cancelUnpaidService;
	private final LotteryNotificationService notificationService;

	public void executeDraws() {
		List<DrawTargetPerformance> targetPerformances = performanceDrawService.findBookingClosedPerformances();
		log.debug("추첨 대상 공연 수 : {}", targetPerformances.size());

		// 각 공연별 추첨
		for (DrawTargetPerformance performance : targetPerformances) {
			Long performanceId = performance.performanceId();
			Long scheduleId = performance.performanceScheduleId();

			try {
				performanceDrawService.drawForPerformance(performanceId, scheduleId);
				notificationService.notifyWinners(scheduleId);
				log.debug("공연 추첨 완료 - scheduleId: {}", scheduleId);
			} catch (Exception e) {
				log.error("공연 추첨 실패 - scheduleId: {}", scheduleId, e);
			}
		}
	}

	/**
	 * 좌석 할당 실행
	 */
	public void executeAllocation() {
		// 좌석 할당이 필요한 공연 조회
		List<DrawTargetPerformance> targetPerformances = seatAllocationService.findBookingOpenPerformances();
		log.debug("좌석 할당 공연 수 : {}", targetPerformances.size());

		for (DrawTargetPerformance performance : targetPerformances) {
			Long scheduleId = performance.performanceScheduleId();

			try {
				seatAllocationService.allocateSeats(scheduleId);
				log.debug("공연 추첨 완료 - scheduleId: {}", scheduleId);
			} catch (Exception e) {
				log.error("공연 추첨 실패 - scheduleId: {}", scheduleId, e);
			}
		}
	}

	/**
	 * 미결제자 당첨 취소
	 */
	public void executecancelUnpaid() {
		try {
			List<Long> memberIds = cancelUnpaidService.cancelUnpaid();
			notificationService.notifyCancelUnpaid(memberIds);
		} catch (Exception e) {
			log.error("당첨 취소 실패");
		}
	}
}
