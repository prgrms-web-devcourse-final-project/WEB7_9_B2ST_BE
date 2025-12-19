package com.back.b2st.domain.payment.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationStatus;
import com.back.b2st.domain.reservation.error.ReservationErrorCode;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.ticket.service.TicketService;
import com.back.b2st.global.error.exception.BusinessException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReservationPaymentFinalizer implements PaymentFinalizer {

	@PersistenceContext
	private EntityManager entityManager;

	private final TicketService ticketService;
	private final Clock clock;

	@Override
	public boolean supports(DomainType domainType) {
		return domainType == DomainType.RESERVATION;
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

		ScheduleSeat scheduleSeat = findScheduleSeatWithLock(reservation.getScheduleId(), reservation.getSeatId());

		// 멱등: 이미 확정된 예매라면 좌석/티켓이 최종 상태인지 보정하고 종료
		if (reservation.getStatus() == ReservationStatus.COMPLETED) {
			ensureSeatSold(scheduleSeat);
			ensureTicketExists(reservation);
			return;
		}

		ensureSeatHoldOrSold(scheduleSeat);

		LocalDateTime now = LocalDateTime.now(clock);
		reservation.complete(now);
		scheduleSeat.sold();
		ensureTicketExists(reservation);
	}

	private ScheduleSeat findScheduleSeatWithLock(Long scheduleId, Long seatId) {
		ScheduleSeat scheduleSeat = entityManager
			.createQuery("SELECT s FROM ScheduleSeat s WHERE s.scheduleId = :scheduleId AND s.seatId = :seatId", ScheduleSeat.class)
			.setParameter("scheduleId", scheduleId)
			.setParameter("seatId", seatId)
			.setLockMode(LockModeType.PESSIMISTIC_WRITE)
			.getResultStream()
			.findFirst()
			.orElse(null);

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

	private void ensureTicketExists(Reservation reservation) {
		ticketService.createTicket(reservation.getId(), reservation.getMemberId(), reservation.getSeatId());
	}
}

