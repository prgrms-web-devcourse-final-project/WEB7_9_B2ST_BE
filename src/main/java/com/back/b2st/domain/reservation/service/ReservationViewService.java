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
import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.repository.TicketRepository;
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
	private final TicketRepository ticketRepository;
	private final PaymentViewService paymentViewService;

	/** === 예매 상세 조회 === */
	public ReservationDetailWithPaymentRes getReservationDetail(Long reservationId, Long memberId) {
		// 1) 기본: 본인 예약 상세
		ReservationDetailRes ownedReservation = reservationRepository.findReservationDetail(reservationId, memberId);
		if (ownedReservation != null) {
			List<ReservationSeatInfo> seats = reservationSeatRepository.findSeatInfos(reservationId);
			PaymentConfirmRes payment = paymentViewService.getByReservationId(reservationId, memberId);
			return new ReservationDetailWithPaymentRes(ownedReservation, seats, payment);
		}

		// 2) 티켓 소유자 기반 상세 (교환/양도 등으로 예약자 != 티켓소유자 가능)
		List<Ticket> tickets = ticketRepository.findAllByReservationIdAndMemberId(reservationId, memberId);

		// 티켓이 없으면 조회 권한 없음
		if (tickets.isEmpty()) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND);
		}

		// 2-1) 신청예매: Ticket.reservationId가 prereservationBookingId로 저장된 레거시 케이스
		PrereservationBooking booking = prereservationBookingRepository.findById(reservationId).orElse(null);
		if (booking != null) {
			ScheduleSeat scheduleSeat = scheduleSeatRepository.findById(booking.getScheduleSeatId())
				.orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

			boolean hasMatchingTicketSeat = tickets.stream()
				.anyMatch(ticket -> ticket.getSeatId().equals(scheduleSeat.getSeatId()));

			if (hasMatchingTicketSeat) {
				PerformanceSchedule schedule = performanceScheduleRepository.findById(booking.getScheduleId())
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
		}

		// 2-2) 일반 예약: "티켓을 가진 사용자"가 좌석 매칭되는 경우 조회 허용
		ReservationDetailRes reservationByTicket = reservationRepository.findReservationDetail(reservationId);
		if (reservationByTicket == null) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND);
		}

		List<ReservationSeatInfo> seats = reservationSeatRepository.findSeatInfos(reservationId);
		boolean hasMatchingTicketSeat = seats.stream()
			.anyMatch(seat -> tickets.stream().anyMatch(ticket -> ticket.getSeatId().equals(seat.seatId())));

		if (!hasMatchingTicketSeat) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND);
		}

		PaymentConfirmRes payment = paymentViewService.getByReservationId(reservationId, memberId);
		return new ReservationDetailWithPaymentRes(reservationByTicket, seats, payment);
	}

	/** === 예매 다건 조회 === */
	public List<ReservationRes> getMyReservations(Long memberId) {
		return reservationRepository.findMyReservations(memberId);
	}
}
