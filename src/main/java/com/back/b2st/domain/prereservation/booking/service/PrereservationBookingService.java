package com.back.b2st.domain.prereservation.booking.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.prereservation.booking.entity.PrereservationBooking;
import com.back.b2st.domain.prereservation.booking.entity.PrereservationBookingStatus;
import com.back.b2st.domain.prereservation.booking.repository.PrereservationBookingRepository;
import com.back.b2st.domain.prereservation.entry.error.PrereservationErrorCode;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.scheduleseat.service.ScheduleSeatStateService;
import com.back.b2st.domain.scheduleseat.service.SeatHoldTokenService;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrereservationBookingService {

	private final PerformanceScheduleRepository performanceScheduleRepository;
	private final ScheduleSeatRepository scheduleSeatRepository;
	private final SeatHoldTokenService seatHoldTokenService;
	private final PrereservationBookingRepository prereservationBookingRepository;
	private final ScheduleSeatStateService scheduleSeatStateService;

	@Transactional
	public PrereservationBooking createBooking(Long memberId, Long scheduleId, Long seatId) {
		PerformanceSchedule schedule = performanceScheduleRepository.findById(scheduleId)
			.orElseThrow(() -> new BusinessException(PrereservationErrorCode.SCHEDULE_NOT_FOUND));

		if (schedule.getBookingType() != BookingType.PRERESERVE) {
			throw new BusinessException(PrereservationErrorCode.BOOKING_TYPE_NOT_SUPPORTED);
		}

		ScheduleSeat scheduleSeat = scheduleSeatRepository.findByScheduleIdAndSeatId(scheduleId, seatId)
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND));

		if (scheduleSeat.getStatus() != SeatStatus.HOLD) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE);
		}

		seatHoldTokenService.validateOwnership(scheduleId, seatId, memberId);

		LocalDateTime now = LocalDateTime.now();

		if (scheduleSeat.getHoldExpiredAt() == null) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE);
		}

		if (scheduleSeat.getHoldExpiredAt().isBefore(now)) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE, "좌석 HOLD가 만료되었습니다.");
		}

			prereservationBookingRepository.findActiveByScheduleSeatIdAndNotExpired(
				scheduleSeat.getId(),
				List.of(PrereservationBookingStatus.CREATED),
				now
			)
				.ifPresent(existing -> {
					throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE, "이미 생성된 신청예매가 존재합니다.");
				});

		return prereservationBookingRepository.save(
			PrereservationBooking.builder()
				.scheduleId(scheduleId)
				.memberId(memberId)
				.scheduleSeatId(scheduleSeat.getId())
				.expiresAt(scheduleSeat.getHoldExpiredAt())
				.build()
		);
	}

	@Transactional
	public int expireCreatedBookingsBatch() {
		return prereservationBookingRepository.expireCreatedBookingsBatch(
			PrereservationBookingStatus.CREATED,
			PrereservationBookingStatus.FAILED,
			LocalDateTime.now()
		);
	}

	@Transactional(readOnly = true)
	public PrereservationBooking getBookingOrThrow(Long bookingId) {
		return prereservationBookingRepository.findById(bookingId)
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND));
	}

	@Transactional
	public void failBooking(Long bookingId) {
		PrereservationBooking booking = prereservationBookingRepository.findByIdWithLock(bookingId)
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND));

		if (booking.getStatus() == PrereservationBookingStatus.FAILED
			|| booking.getStatus() == PrereservationBookingStatus.CANCELED
			|| booking.getStatus() == PrereservationBookingStatus.COMPLETED) {
			return;
		}

		booking.fail();

		ScheduleSeat scheduleSeat = scheduleSeatRepository.findById(booking.getScheduleSeatId())
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND));

		scheduleSeatStateService.releaseHold(scheduleSeat.getScheduleId(), scheduleSeat.getSeatId());
	}
}
