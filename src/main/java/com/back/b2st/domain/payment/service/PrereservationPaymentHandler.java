package com.back.b2st.domain.payment.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.prereservation.booking.entity.PrereservationBooking;
import com.back.b2st.domain.prereservation.booking.entity.PrereservationBookingStatus;
import com.back.b2st.domain.prereservation.booking.service.PrereservationBookingService;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.scheduleseat.service.SeatHoldTokenService;
import com.back.b2st.domain.seat.grade.entity.SeatGrade;
import com.back.b2st.domain.seat.grade.repository.SeatGradeRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PrereservationPaymentHandler implements PaymentDomainHandler {

	private final ScheduleSeatRepository scheduleSeatRepository;
	private final SeatHoldTokenService seatHoldTokenService;
	private final PerformanceScheduleRepository performanceScheduleRepository;
	private final SeatGradeRepository seatGradeRepository;
	private final PrereservationBookingService prereservationBookingService;

	@Override
	public boolean supports(DomainType domainType) {
		return domainType == DomainType.PRERESERVATION;
	}

	@Override
	@Transactional(readOnly = true)
	public PaymentTarget loadAndValidate(Long bookingId, Long memberId) {
		PrereservationBooking booking = prereservationBookingService.getBookingOrThrow(bookingId);

		if (!booking.getMemberId().equals(memberId)) {
			throw new BusinessException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
		}

		if (booking.getStatus() != PrereservationBookingStatus.CREATED) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE);
		}

		ScheduleSeat scheduleSeat = scheduleSeatRepository.findById(booking.getScheduleSeatId())
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND));

		PerformanceSchedule schedule = performanceScheduleRepository.findById(scheduleSeat.getScheduleId())
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND));

		if (schedule.getBookingType() != BookingType.PRERESERVE) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE, "신청 예매 결제 대상이 아닙니다.");
		}

		if (scheduleSeat.getStatus() != SeatStatus.HOLD) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE);
		}

		seatHoldTokenService.validateOwnership(scheduleSeat.getScheduleId(), scheduleSeat.getSeatId(), memberId);

		Long performanceId = schedule.getPerformance().getPerformanceId();
		SeatGrade seatGrade = seatGradeRepository.findTopByPerformanceIdAndSeatIdOrderByIdDesc(
				performanceId,
				scheduleSeat.getSeatId()
			)
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE));

		Long expectedAmount = seatGrade.getPrice().longValue();
		return new PaymentTarget(DomainType.PRERESERVATION, bookingId, expectedAmount);
	}
}
