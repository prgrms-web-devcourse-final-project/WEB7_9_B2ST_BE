package com.back.b2st.domain.reservation.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.reservation.dto.request.ReservationRequest;
import com.back.b2st.domain.reservation.dto.response.ReservationResponse;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.error.ReservationErrorCode;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.global.error.exception.BusinessException;

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

	/** === 예매 단건 조회 === */
	@Transactional(readOnly = true)
	public ReservationResponse getReservation(Long reservationId) {

		Reservation reservation = reservationRepository.findById(reservationId)
			.orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

		return new ReservationResponse(
			reservation.getId(),
			reservation.getMemberId(),
			reservation.getPerformanceId(),
			reservation.getSeatId()
		);
	}

	/** === 로그인 유저의 예매 전체 조회 === */
	@Transactional(readOnly = true)
	public List<ReservationResponse> getMyReservations(Long memberId) {

		List<Reservation> reservations = reservationRepository.findAllByMemberId(memberId);

		return reservations.stream()
			.map(r -> new ReservationResponse(
				r.getId(),
				r.getMemberId(),
				r.getPerformanceId(),
				r.getSeatId()
			))
			.toList();
	}

}
