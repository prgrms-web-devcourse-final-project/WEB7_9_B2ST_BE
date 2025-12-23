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

	@Scheduled(fixedDelay = 5000)
	public void expirePendingReservations() {
		// 예매 해제 (예매랑 연관된 좌석도 해제)
		int expiredReservations = reservationService.expirePendingReservations();

		// 좌석 해제
		int releasedHolds = scheduleSeatStateService.releaseExpiredHolds();

		if (expiredReservations > 0 || releasedHolds > 0) {
			log.info("스케줄러 처리 결과 - 만료된 예매={}건, 해제된 좌석 HOLD={}건",
				expiredReservations,
				releasedHolds);
		}
	}
}