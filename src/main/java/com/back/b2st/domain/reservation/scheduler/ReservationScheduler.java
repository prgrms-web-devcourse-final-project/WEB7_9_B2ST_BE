package com.back.b2st.domain.reservation.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.back.b2st.domain.reservation.service.ReservationService;
import com.back.b2st.domain.scheduleseat.service.ScheduleSeatStateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduler {

	private final ReservationService reservationService;
	private final ScheduleSeatStateService scheduleSeatStateService;

	@Scheduled(fixedDelayString = "${scheduler.reservation-expire.delay-ms:5000}")
	public void expireAndReleaseBatch() {
		int expiredReservations = 0;
		int releasedHolds = 0;

		try {
			// 1) 예매 만료: Reservation만 EXPIRED
			expiredReservations = reservationService.expirePendingReservationsBatch();

			// 2) 좌석 만료: ScheduleSeat만 AVAILABLE + Redis 토큰 삭제
			releasedHolds = scheduleSeatStateService.releaseExpiredHoldsBatch();

			if (expiredReservations > 0 || releasedHolds > 0) {
				log.info("스케줄러 처리 결과 - 만료된 예매={}건, 해제된 좌석 HOLD={}건", expiredReservations, releasedHolds);
			}
		} catch (Exception e) {
			log.error("스케줄러 처리 중 오류가 발생했습니다. (만료된 예매={}건, 해제된 좌석 HOLD={}건)", expiredReservations, releasedHolds, e);
		}
	}
}
