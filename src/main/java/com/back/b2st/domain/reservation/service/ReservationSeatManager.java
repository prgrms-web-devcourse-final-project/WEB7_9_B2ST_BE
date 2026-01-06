package com.back.b2st.domain.reservation.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.reservation.dto.response.SeatReservationResult;
import com.back.b2st.domain.reservation.entity.ReservationSeat;
import com.back.b2st.domain.reservation.repository.ReservationSeatRepository;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.scheduleseat.service.ScheduleSeatService;
import com.back.b2st.domain.scheduleseat.service.ScheduleSeatStateService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationSeatManager {

	private final ReservationSeatRepository reservationSeatRepository;
	private final ScheduleSeatRepository scheduleSeatRepository;

	private final ScheduleSeatService scheduleSeatService;
	private final ScheduleSeatStateService scheduleSeatStateService;

	/** === 예매 전 좌석 검사 === */
	@Transactional(readOnly = true)
	public SeatReservationResult prepareSeatReservation(
		Long scheduleId,
		List<Long> seatIds,
		Long memberId
	) {
		LocalDateTime expiresAt = null;
		List<Long> scheduleSeatIds = new ArrayList<>();

		for (Long seatId : seatIds) {
			ScheduleSeat seat =
				scheduleSeatService.validateAndGetAttachableSeat(
					scheduleId, seatId, memberId
				);

			scheduleSeatIds.add(seat.getId());

			// 만료 시각은 가장 빠른 HOLD 기준
			if (expiresAt == null || seat.getHoldExpiredAt().isBefore(expiresAt)) {
				expiresAt = seat.getHoldExpiredAt();
			}
		}

		return new SeatReservationResult(
			scheduleSeatIds,
			expiresAt
		);
	}

	/** === 예매용 좌석 저장 === */
	@Transactional
	public void attachSeats(
		Long reservationId,
		List<Long> scheduleSeatIds
	) {
		for (Long scheduleSeatId : scheduleSeatIds) {
			reservationSeatRepository.save(
				ReservationSeat.builder()
					.reservationId(reservationId)
					.scheduleSeatId(scheduleSeatId)
					.build()
			);
		}
	}

	/** === 예매에 포함된 좌석 HOLD 해제 === */
	@Transactional
	public void releaseAllSeats(Long reservationId) {
		reservationSeatRepository.findByReservationId(reservationId)
			.forEach(rs -> {
				ScheduleSeat scheduleSeat =
					scheduleSeatRepository.findById(rs.getScheduleSeatId())
						.orElseThrow();

				scheduleSeatStateService.releaseHold(
					scheduleSeat.getScheduleId(),
					scheduleSeat.getSeatId()
				);
			});
	}

	/** === 예매에 포함된 모든 좌석 HOLD 해제 === */
	@Transactional
	public void releaseForceAllSeats(Long reservationId) {
		reservationSeatRepository.findByReservationId(reservationId)
			.forEach(rs -> {
				ScheduleSeat scheduleSeat =
					scheduleSeatRepository.findById(rs.getScheduleSeatId())
						.orElseThrow();

				scheduleSeatStateService.releaseForceHold(
					scheduleSeat.getScheduleId(),
					scheduleSeat.getSeatId()
				);
			});
	}

	/** === 예매에 포함된 모든 좌석 SOLD 처리 === */
	@Transactional
	public void confirmAllSeats(Long reservationId) {

		reservationSeatRepository.findByReservationId(reservationId)
			.forEach(rs -> {
				ScheduleSeat scheduleSeat =
					scheduleSeatRepository.findById(rs.getScheduleSeatId())
						.orElseThrow();

				scheduleSeatStateService.confirmHold(
					scheduleSeat.getScheduleId(),
					scheduleSeat.getSeatId()
				);
			});
	}
}
