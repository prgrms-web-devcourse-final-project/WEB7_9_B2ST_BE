package com.back.b2st.domain.reservation.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.payment.dto.response.PaymentConfirmRes;
import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.service.PaymentViewService;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.prereservation.booking.entity.PrereservationBooking;
import com.back.b2st.domain.prereservation.booking.repository.PrereservationBookingRepository;
import com.back.b2st.domain.reservation.dto.response.ReservationDetailRes;
import com.back.b2st.domain.reservation.dto.response.ReservationDetailWithPaymentRes;
import com.back.b2st.domain.reservation.dto.response.ReservationRes;
import com.back.b2st.domain.reservation.dto.response.ReservationSeatInfo;
import com.back.b2st.domain.reservation.error.ReservationErrorCode;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.reservation.repository.ReservationSeatRepository;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationViewService {

	private final ReservationRepository reservationRepository;
	private final ReservationSeatRepository reservationSeatRepository;
	private final PrereservationBookingRepository prereservationBookingRepository;
	private final ScheduleSeatRepository scheduleSeatRepository;
	private final SeatRepository seatRepository;
	private final PerformanceScheduleRepository performanceScheduleRepository;

	private final PaymentViewService paymentViewService;

	/** === 예매 상세 조회 === */
	public ReservationDetailWithPaymentRes getReservationDetail(Long reservationId, Long memberId) {

		ReservationDetailRes reservation =
			reservationRepository.findReservationDetail(reservationId, memberId);

		if (reservation != null) {
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

		// 신청 예매 티켓의 경우, Ticket.reservationId가 실제 Reservation PK가 아니라 prereservationBookingId로 저장되어 있음
		PrereservationBooking booking = prereservationBookingRepository.findById(reservationId)
			.filter(b -> b.getMemberId().equals(memberId))
			.orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

		PerformanceSchedule schedule = performanceScheduleRepository.findById(booking.getScheduleId())
			.orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

		ScheduleSeat scheduleSeat = scheduleSeatRepository.findById(booking.getScheduleSeatId())
			.orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

		Seat seat = seatRepository.findById(scheduleSeat.getSeatId())
			.orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

		ReservationDetailRes prereservation = new ReservationDetailRes(
			booking.getId(),
			booking.getStatus().name(),
			new ReservationDetailRes.PerformanceInfo(
				schedule.getPerformance().getPerformanceId(),
				schedule.getPerformanceScheduleId(),
				schedule.getPerformance().getTitle(),
				schedule.getPerformance().getCategory(),
				schedule.getPerformance().getStartDate(),
				schedule.getStartAt()
			)
		);

		List<ReservationSeatInfo> seats = List.of(
			new ReservationSeatInfo(
				seat.getId(),
				seat.getSectionId(),
				seat.getSectionName(),
				seat.getRowLabel(),
				seat.getSeatNumber()
			)
		);

		PaymentConfirmRes payment = paymentViewService.getByDomain(DomainType.PRERESERVATION, booking.getId(), memberId);

		return new ReservationDetailWithPaymentRes(prereservation, seats, payment);
	}

	/** === 예매 다건 조회 === */
	public List<ReservationRes> getMyReservations(Long memberId) {
		return reservationRepository.findMyReservations(memberId);
	}
}
