package com.back.b2st.domain.scheduleseat.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.back.b2st.domain.scheduleseat.service.ScheduleSeatStateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleSeatScheduler {

	private final ScheduleSeatStateService scheduleSeatStateService;

	/** === HOLD 만료 좌석 자동 복구 스케줄러 === */
	@Scheduled(fixedDelayString = "${seat.hold.expire-release-interval-ms:5000}")
	public void releaseExpiredHolds() {
		int updated = scheduleSeatStateService.releaseExpiredHolds();
		if (updated > 0) {
			log.info("[HOLD 만료 복구] 복구된 좌석 수={}", updated);
		}
	}
}