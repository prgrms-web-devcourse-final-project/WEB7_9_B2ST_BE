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

	private final SeatSelectionService seatSelectionService;
	private final ReservationRepository reservationRepository;

	/** === 예매 생성 === */
	@Transactional
	public ReservationResponse createReservation(Long memberId, ReservationRequest request) {

		// 1. 락 + HOLD
		seatSelectionService.selectSeat(memberId, request.performanceId(), request.seatId());

		// 2. Reservation 생성
		Reservation reservation = Reservation.builder()
			.performanceId(request.performanceId())
			.memberId(memberId)
			.seatId(request.seatId())
			.build();

		Reservation saved = reservationRepository.save(reservation);

		// 3. Response 변환
		return new ReservationResponse(
			saved.getId(),
			saved.getMemberId(),
			saved.getPerformanceId(),
			saved.getSeatId()
		);
	}

}
