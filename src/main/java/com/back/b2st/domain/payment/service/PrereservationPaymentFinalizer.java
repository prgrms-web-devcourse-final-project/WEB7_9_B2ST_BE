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
import com.back.b2st.domain.prereservation.booking.entity.PrereservationBooking;
import com.back.b2st.domain.prereservation.booking.entity.PrereservationBookingStatus;
import com.back.b2st.domain.prereservation.booking.repository.PrereservationBookingRepository;
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
	private final PrereservationBookingRepository prereservationBookingRepository;
	private final TicketService ticketService;
	private final Clock clock;

	@Override
	public boolean supports(DomainType domainType) {
		return domainType == DomainType.PRERESERVATION;
	}

	@Override
	@Transactional
	public void finalizePayment(Payment payment) {
		PrereservationBooking booking = prereservationBookingRepository.findByIdWithLock(payment.getDomainId())
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND));

		if (!booking.getMemberId().equals(payment.getMemberId())) {
			throw new BusinessException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
		}

		if (booking.getStatus() == PrereservationBookingStatus.CANCELED) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE, "이미 취소된 신청예매입니다.");
		}

		PerformanceSchedule schedule = performanceScheduleRepository.findById(booking.getScheduleId())
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND));

		if (schedule.getBookingType() != BookingType.PRERESERVE) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE, "신청 예매 결제 대상이 아닙니다.");
		}

		ScheduleSeat scheduleSeat = findScheduleSeatWithLock(booking.getScheduleSeatId());

		if (booking.getStatus() == PrereservationBookingStatus.COMPLETED) {
			ensureSeatSold(scheduleSeat);
			ensureTicketExists(booking, scheduleSeat.getSeatId());
			return;
		}

		ensureSeatHoldOrSold(scheduleSeat);

		LocalDateTime now = LocalDateTime.now(clock);
		booking.complete(now);
		scheduleSeat.sold();
		ensureTicketExists(booking, scheduleSeat.getSeatId());
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

	private void ensureTicketExists(PrereservationBooking booking, Long seatId) {
		ticketService.createTicket(booking.getId(), booking.getMemberId(), seatId);
	}
}
