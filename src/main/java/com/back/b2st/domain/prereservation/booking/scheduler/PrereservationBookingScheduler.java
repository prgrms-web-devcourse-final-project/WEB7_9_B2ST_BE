package com.back.b2st.domain.prereservation.booking.scheduler;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.back.b2st.domain.prereservation.booking.service.PrereservationBookingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class PrereservationBookingScheduler {

	private final PrereservationBookingService prereservationBookingService;

	@Scheduled(fixedDelayString = "${scheduler.prereservation-booking-expire.delay-ms:5000}")
	public void expireCreatedBookingsBatch() {
		try {
			int expired = prereservationBookingService.expireCreatedBookingsBatch();
			if (expired > 0) {
				log.info("스케줄러 처리 결과 - 만료된 신청예매(prereservationBooking)={}건", expired);
			}
		} catch (Exception e) {
			log.error("스케줄러 처리 중 오류가 발생했습니다. (신청예매 만료)", e);
		}
	}
}

