package com.back.b2st.domain.reservation.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.payment.dto.response.PaymentConfirmRes;
import com.back.b2st.domain.payment.service.PaymentViewService;
import com.back.b2st.domain.reservation.dto.response.AdminReservationSummaryRes;
import com.back.b2st.domain.reservation.dto.response.ReservationDetailRes;
import com.back.b2st.domain.reservation.dto.response.ReservationDetailWithPaymentRes;
import com.back.b2st.domain.reservation.dto.response.ReservationSeatInfo;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationStatus;
import com.back.b2st.domain.reservation.error.ReservationErrorCode;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.reservation.repository.ReservationSeatRepository;
import com.back.b2st.domain.scheduleseat.service.ScheduleSeatStateService;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReservationService {

	private final ReservationRepository reservationRepository;
	private final ReservationSeatRepository reservationSeatRepository;
	private final PaymentViewService paymentViewService;
	private final ScheduleSeatStateService scheduleSeatStateService;

	/** ===  관리자 예매 상태별 조회 === */
	public Page<AdminReservationSummaryRes> getReservationsByStatus(
		ReservationStatus status,
		Long scheduleId,
		Long memberId,
		Pageable pageable
	) {
		Page<Reservation> page =
			reservationRepository.findByStatusWithOptionalFilters(status, scheduleId, memberId, pageable);

		return page.map(r -> AdminReservationSummaryRes.builder()
			.reservationId(r.getId())
			.scheduleId(r.getScheduleId())
			.memberId(r.getMemberId())
			.status(r.getStatus())
			.seatCount(reservationSeatRepository.countByReservationId(r.getId()))
			.createdAt(r.getCreatedAt())
			.expiresAt(r.getExpiresAt())
			.build()
		);
	}

	/** === 관리자 예매 상세 조회 === */
	public ReservationDetailWithPaymentRes getReservationDetail(Long reservationId) {

		Reservation entity = reservationRepository.findById(reservationId)
			.orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

		Long memberId = entity.getMemberId();

		ReservationDetailRes reservation =
			reservationRepository.findReservationDetail(reservationId, memberId);

		if (reservation == null) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND);
		}

		List<ReservationSeatInfo> seats =
			reservationSeatRepository.findSeatInfos(reservationId);

		PaymentConfirmRes payment =
			paymentViewService.getByReservationId(reservationId, memberId);

		return new ReservationDetailWithPaymentRes(reservation, seats, payment);
	}

	/** === 관리자 예매 강제 취소 + 좌석 원복 === */
	@Transactional
	public void forceCancel(Long reservationId) {

		Reservation reservation = reservationRepository.findById(reservationId)
			.orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

		if (reservation.getStatus() == ReservationStatus.CANCELED) {
			return;
		}

		reservation.cancel(LocalDateTime.now());

		List<ReservationSeatInfo> seatInfos =
			reservationSeatRepository.findSeatInfos(reservationId);

		for (ReservationSeatInfo seatInfo : seatInfos) {
			scheduleSeatStateService.forceToAvailable(
				reservation.getScheduleId(),
				seatInfo.seatId()
			);
		}
	}
}