package com.back.b2st.domain.reservation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.reservation.dto.request.ReservationRequest;
import com.back.b2st.domain.reservation.dto.response.ReservationResponse;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationService {

	private final SeatHoldService seatHoldService;
	private final ReservationRepository reservationRepository;

	/** === 예매 생성 === */
	@Transactional
	public ReservationResponse createReservation(Long memberId, ReservationRequest request) {

		// 1. 좌석 HOLD 진행
		seatHoldService.holdSeat(request.performanceId(), request.seatId());

		// 2. Reservation 엔티티 생성
		Reservation reservation = Reservation.builder()
			.performanceId(request.performanceId())
			.memberId(memberId)
			.seatId(request.seatId())
			.build();

		Reservation savedReservation = reservationRepository.save(reservation);

		// 3. Response 변환
		return new ReservationResponse(
			savedReservation.getId(),
			savedReservation.getMemberId(),
			savedReservation.getPerformanceId(),
			savedReservation.getSeatId()
		);
	}

}
