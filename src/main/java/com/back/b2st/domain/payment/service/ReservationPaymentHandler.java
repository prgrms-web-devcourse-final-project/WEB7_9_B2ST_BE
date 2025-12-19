package com.back.b2st.domain.payment.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationStatus;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.reservation.service.SeatHoldTokenService;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.seat.grade.entity.SeatGrade;
import com.back.b2st.domain.seat.grade.repository.SeatGradeRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReservationPaymentHandler implements PaymentDomainHandler {

	private final ReservationRepository reservationRepository;
	private final ScheduleSeatRepository scheduleSeatRepository;
	private final SeatHoldTokenService seatHoldTokenService;
	private final PerformanceScheduleRepository performanceScheduleRepository;
	private final SeatGradeRepository seatGradeRepository;

	@Override
	public boolean supports(DomainType domainType) {
		return domainType == DomainType.RESERVATION;
	}

	@Override
	@Transactional(readOnly = true)
	public PaymentTarget loadAndValidate(Long reservationId, Long memberId) {
		Reservation reservation = reservationRepository.findById(reservationId)
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND));

		if (!reservation.getMemberId().equals(memberId)) {
			throw new BusinessException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
		}

		if (reservation.getStatus() != ReservationStatus.PENDING) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE);
		}

		Long scheduleId = reservation.getPerformanceId();
		Long seatId = reservation.getSeatId();

		ScheduleSeat scheduleSeat = scheduleSeatRepository.findByScheduleIdAndSeatId(scheduleId, seatId)
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND));

		if (scheduleSeat.getStatus() != SeatStatus.HOLD) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE);
		}

		seatHoldTokenService.validateOwnership(scheduleId, seatId, memberId);

		PerformanceSchedule schedule = performanceScheduleRepository.findById(scheduleId)
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND));
		Long performanceId = schedule.getPerformance().getPerformanceId();

		SeatGrade seatGrade = seatGradeRepository.findTopByPerformanceIdAndSeatIdOrderByIdDesc(performanceId, seatId)
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE));

		Long expectedAmount = seatGrade.getPrice().longValue();
		return new PaymentTarget(DomainType.RESERVATION, reservationId, expectedAmount);
	}
}

