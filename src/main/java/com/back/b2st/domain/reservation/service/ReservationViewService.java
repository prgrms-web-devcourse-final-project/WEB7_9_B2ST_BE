package com.back.b2st.domain.reservation.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.payment.dto.response.PaymentConfirmRes;
import com.back.b2st.domain.payment.service.PaymentViewService;
import com.back.b2st.domain.reservation.dto.response.ReservationDetailRes;
import com.back.b2st.domain.reservation.dto.response.ReservationDetailWithPaymentRes;
import com.back.b2st.domain.reservation.dto.response.ReservationRes;
import com.back.b2st.domain.reservation.dto.response.ReservationSeatInfo;
import com.back.b2st.domain.reservation.error.ReservationErrorCode;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.reservation.repository.ReservationSeatRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationViewService {

	private final ReservationRepository reservationRepository;
	private final ReservationSeatRepository reservationSeatRepository;

	private final PaymentViewService paymentViewService;

	/** === 예매 상세 조회 === */
	public ReservationDetailWithPaymentRes getReservationDetail(Long reservationId, Long memberId) {

		ReservationDetailRes reservation =
			reservationRepository.findReservationDetail(reservationId, memberId);

		if (reservation == null) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND);
		}

		// 좌석 목록
		List<ReservationSeatInfo> seats = reservationSeatRepository.findSeatInfos(reservationId);

		// 결제 정보
		PaymentConfirmRes payment = paymentViewService.getByReservationId(reservationId, memberId);

		return new ReservationDetailWithPaymentRes(
			reservation,
			seats,
			payment
		);
	}

	/** === 예매 다건 조회 === */
	public List<ReservationRes> getMyReservations(Long memberId) {
		return reservationRepository.findMyReservations(memberId);
	}
}
