package com.back.b2st.domain.payment.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationSeat;
import com.back.b2st.domain.reservation.entity.ReservationStatus;
import com.back.b2st.domain.reservation.error.ReservationErrorCode;
import com.back.b2st.domain.reservation.repository.ReservationSeatRepository;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.ticket.service.TicketService;
import com.back.b2st.global.error.exception.BusinessException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PrereservationPaymentFinalizer implements PaymentFinalizer {

	private final EntityManager entityManager;
	private final PerformanceScheduleRepository performanceScheduleRepository;
	private final ReservationSeatRepository reservationSeatRepository;
	private final TicketService ticketService;
	private final Clock clock;

	@Override
	public boolean supports(DomainType domainType) {
		return domainType == DomainType.PRERESERVATION;
	}

	@Override
	@Transactional
		public void finalizePayment(Payment payment) {
			Reservation reservation = entityManager.find(Reservation.class, payment.getDomainId(), LockModeType.PESSIMISTIC_WRITE);
			if (reservation == null) {
				throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND);
			}

		if (!reservation.getMemberId().equals(payment.getMemberId())) {
			throw new BusinessException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
		}

		if (reservation.getStatus() == ReservationStatus.CANCELED) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_ALREADY_CANCELED);
		}

		PerformanceSchedule schedule = performanceScheduleRepository.findById(reservation.getScheduleId())
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND));

			if (schedule.getBookingType() != BookingType.PRERESERVE) {
				throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE, "신청 예매 결제 대상이 아닙니다.");
			}

			ReservationSeat rs = reservationSeatRepository.findByReservationId(reservation.getId()).stream()
				.findFirst()
				.orElseThrow(() -> new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND));

			ScheduleSeat scheduleSeat = findScheduleSeatWithLock(rs.getScheduleSeatId());

			if (!scheduleSeat.getScheduleId().equals(reservation.getScheduleId())) {
				throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND, "예매 좌석 정보를 찾을 수 없습니다.");
			}

			if (reservation.getStatus() == ReservationStatus.COMPLETED) {
				ensureSeatSold(scheduleSeat);
				ensureTicketExists(reservation, scheduleSeat.getSeatId());
				return;
			}

			ensureSeatHoldOrSold(scheduleSeat);

			LocalDateTime now = LocalDateTime.now(clock);
			reservation.complete(now);
			scheduleSeat.sold();
			ensureTicketExists(reservation, scheduleSeat.getSeatId());
		}

		private ScheduleSeat findScheduleSeatWithLock(Long scheduleSeatId) {
			ScheduleSeat scheduleSeat =
				entityManager.find(ScheduleSeat.class, scheduleSeatId, LockModeType.PESSIMISTIC_WRITE);

			if (scheduleSeat == null) {
				throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND, "예매 좌석 정보를 찾을 수 없습니다.");
			}
			return scheduleSeat;
		}

	private void ensureSeatHoldOrSold(ScheduleSeat scheduleSeat) {
		if (scheduleSeat.getStatus() == SeatStatus.SOLD) {
			return;
		}
		if (scheduleSeat.getStatus() != SeatStatus.HOLD) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE, "좌석이 HOLD 상태가 아닙니다.");
		}
	}

	private void ensureSeatSold(ScheduleSeat scheduleSeat) {
		if (scheduleSeat.getStatus() != SeatStatus.SOLD) {
			scheduleSeat.sold();
		}
	}

		private void ensureTicketExists(Reservation reservation, Long seatId) {
			ticketService.createTicket(reservation.getId(), reservation.getMemberId(), seatId);
		}
	}
