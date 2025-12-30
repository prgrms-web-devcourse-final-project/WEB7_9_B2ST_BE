package com.back.b2st.domain.reservation.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.reservation.entity.ReservationSeat;
import com.back.b2st.domain.reservation.repository.ReservationSeatRepository;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.scheduleseat.service.ScheduleSeatService;
import com.back.b2st.domain.scheduleseat.service.ScheduleSeatStateService;
import com.back.b2st.domain.scheduleseat.service.SeatHoldTokenService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationSeatManager {

	private final ReservationSeatRepository reservationSeatRepository;
	private final ScheduleSeatRepository scheduleSeatRepository;

	private final SeatHoldTokenService seatHoldTokenService;
	private final ScheduleSeatService scheduleSeatService;
	private final ScheduleSeatStateService scheduleSeatStateService;

	/** === HOLD된 좌석을 예매에 귀속 === */
	@Transactional
	public void attachSeatsForReservation(
		Long reservationId,
		Long scheduleId,
		List<Long> seatIds,
		Long memberId
	) {
		for (Long seatId : seatIds) {

			// 1. HOLD 소유권 검증 (Redis)
			seatHoldTokenService.validateOwnership(scheduleId, seatId, memberId);

			// 2. DB 좌석 상태 검증 (HOLD + 만료)
			scheduleSeatService.validateHoldState(scheduleId, seatId);

			// 3. ScheduleSeat 조회
			ScheduleSeat scheduleSeat =
				scheduleSeatRepository.findByScheduleIdAndSeatId(scheduleId, seatId)
					.orElseThrow();

			// 4. ReservationSeat 생성
			reservationSeatRepository.save(
				ReservationSeat.builder()
					.reservationId(reservationId)
					.scheduleSeatId(scheduleSeat.getId())
					.build()
			);
		}
	}

	/** === 예매에 포함된 모든 좌석 HOLD 해제 === */
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
