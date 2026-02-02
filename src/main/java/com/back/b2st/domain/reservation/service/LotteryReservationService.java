package com.back.b2st.domain.reservation.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.reservation.dto.response.LotteryReservationCreatedRes;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationSeat;
import com.back.b2st.domain.reservation.entity.ReservationStatus;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.reservation.repository.ReservationSeatRepository;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.error.ScheduleSeatErrorCode;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LotteryReservationService {

	private final ReservationRepository reservationRepository;
	private final ReservationSeatRepository reservationSeatRepository;
	private final ScheduleSeatRepository scheduleSeatRepository;

	/** === 추첨 예매 생성 (결제 완료 기준) === */
	@Transactional
	public LotteryReservationCreatedRes createCompletedReservation(Long memberId, Long scheduleId) {
		Reservation reservation = getOrCreateCompletedReservation(memberId, scheduleId);
		return LotteryReservationCreatedRes.from(reservation);
	}

	@Transactional
	public Reservation getOrCreateCompletedReservation(Long memberId, Long scheduleId) {
		LocalDateTime now = LocalDateTime.now();

		Optional<Reservation> completed =
			reservationRepository.findTopByMemberIdAndScheduleIdAndStatusOrderByIdDesc(
				memberId, scheduleId, ReservationStatus.COMPLETED
			);

		if (completed.isPresent()) {
			return completed.get();
		}

		Optional<Reservation> pending =
			reservationRepository.findTopByMemberIdAndScheduleIdAndStatusOrderByIdDesc(
				memberId, scheduleId, ReservationStatus.PENDING
			);

		if (pending.isPresent()) {
			Reservation reservation = pending.get();
			reservation.complete(now);
			return reservation;
		}

		Reservation reservation = Reservation.builder()
			.scheduleId(scheduleId)
			.memberId(memberId)
			.expiresAt(now)
			.build();
		reservation.complete(now);

		return reservationRepository.save(reservation);
	}

	/** === 추첨 좌석 확정 === */
	@Transactional
	public void confirmAssignedSeats(Long reservationId, Long scheduleId, List<Long> scheduleSeatIds) {

		// 1. 좌석 상태 AVAILABLE → SOLD (추첨 확정)
		int updated = scheduleSeatRepository.updateStatusToSoldByScheduleSeatIds(
			scheduleId,
			scheduleSeatIds,
			SeatStatus.AVAILABLE,
			SeatStatus.SOLD
		);

		if (updated != scheduleSeatIds.size()) {
			throw new BusinessException(ScheduleSeatErrorCode.SEAT_ALREADY_SOLD);
		}

		// 2. 예매-좌석 매핑 생성
		for (Long scheduleSeatId : scheduleSeatIds) {
			reservationSeatRepository.save(
				ReservationSeat.builder()
					.reservationId(reservationId)
					.scheduleSeatId(scheduleSeatId)
					.build()
			);
		}
	}
}
