package com.back.b2st.domain.reservation.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationStatus;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.reservation.service.ReservationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReservationScheduler {

	private final ReservationRepository reservationRepository;
	private final ReservationService reservationService;

	/** === PENDING 만료 회수 === */
	@Scheduled(fixedDelay = 5000)
	@Transactional
	public void expirePendingReservations() {

		LocalDateTime now = LocalDateTime.now();

		List<Reservation> expiredTargets =
			reservationRepository.findAllByStatusAndExpiresAtLessThanEqual(ReservationStatus.PENDING, now);

		for (Reservation reservation : expiredTargets) {
			// ReservationService 내부에서 좌석/토큰 정리까지 수행
			reservationService.expireReservation(reservation.getId());
		}
	}
}
