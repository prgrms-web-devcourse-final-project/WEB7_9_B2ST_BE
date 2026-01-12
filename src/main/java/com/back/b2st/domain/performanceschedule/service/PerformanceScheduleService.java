package com.back.b2st.domain.performanceschedule.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.dto.request.PerformanceScheduleCreateReq;
import com.back.b2st.domain.performanceschedule.dto.response.PerformanceScheduleCreateRes;
import com.back.b2st.domain.performanceschedule.dto.response.PerformanceScheduleDetailRes;
import com.back.b2st.domain.performanceschedule.dto.response.PerformanceScheduleListRes;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.error.PerformanceScheduleErrorCode;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.prereservation.policy.service.PrereservationTimeTableService;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.error.ScheduleSeatErrorCode;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceScheduleService {

	private final PerformanceRepository performanceRepository;
	private final PerformanceScheduleRepository performanceScheduleRepository;

	private final SeatRepository seatRepository;
	private final ScheduleSeatRepository scheduleSeatRepository;
	private final PrereservationTimeTableService prereservationTimeTableService;

	/**
	 * 회차 생성 (관리자)
	 * - performanceId는 URL Path를 단일 기준으로 사용한다.
	 */
	@Transactional
	public PerformanceScheduleCreateRes createSchedule(Long performanceId, PerformanceScheduleCreateReq request) {
		Performance performance = performanceRepository.findById(performanceId)
			.orElseThrow(() -> new BusinessException(PerformanceScheduleErrorCode.PERFORMANCE_NOT_FOUND));

		// 시간/라운드 중복 방지 규칙은 추후 repository 기반으로 여기서 검증
		// validateDuplicatedRoundNo(performanceId, request.roundNo());
		// validateDuplicatedStartAt(performanceId, request.startAt());

		// 생성되는 회차의 bookingType이 BOOKING_ORDER(선착순)라면
		// 해당 회차를 위한 Queue 설정 레코드도 자동으로 생성 (추후 구현)

		validateBookingTime(request);

		PerformanceSchedule schedule = PerformanceSchedule.builder()
			.performance(performance)
			.startAt(request.startAt())
			.roundNo(request.roundNo())
			.bookingType(request.bookingType())
			.bookingOpenAt(request.bookingOpenAt())
			.bookingCloseAt(request.bookingCloseAt())
			.build();

		PerformanceSchedule saved = performanceScheduleRepository.save(schedule);

		if (saved.getBookingType() == BookingType.PRERESERVE) {
			prereservationTimeTableService.ensureDefaultTimeTablesIfMissing(saved.getPerformanceScheduleId());
		}

		createScheduleSeats(saved.getPerformanceScheduleId(), performance.getVenue().getVenueId());

		return PerformanceScheduleCreateRes.from(saved);
	}

	/**
	 * 회차별 좌석(ScheduleSeat) 생성
	 */
	private void createScheduleSeats(Long scheduleId, Long venueId) {

		// 이미 생성된 회차 좌석이 있으면 재생성하지 않음 (중복 방지)
		if (scheduleSeatRepository.existsByScheduleId(scheduleId)) {
			return;
		}

		List<Seat> seats = seatRepository.findByVenueId(venueId);
		if (seats.isEmpty()) {
			throw new BusinessException(ScheduleSeatErrorCode.SEAT_NOT_FOUND);
		}

		List<ScheduleSeat> scheduleSeats = seats.stream()
			.map(seat -> ScheduleSeat.builder()
				.scheduleId(scheduleId)
				.seatId(seat.getId())
				.build())
			.toList();

		scheduleSeatRepository.saveAll(scheduleSeats);
	}

	/**
	 * 공연별 회차 목록 조회
	 */
	public List<PerformanceScheduleListRes> getSchedules(Long performanceId) {
		if (!performanceRepository.existsById(performanceId)) {
			throw new BusinessException(PerformanceScheduleErrorCode.PERFORMANCE_NOT_FOUND);
		}

		return performanceScheduleRepository
			.findAllByPerformance_PerformanceIdOrderByStartAtAsc(performanceId)
			.stream()
			.map(PerformanceScheduleListRes::from)
			.toList();
	}

	/**
	 * 회차 단건 조회
	 * - (performanceId, scheduleId)로 조회하여 "해당 공연의 회차"가 맞는지까지 검증한다.
	 */
	public PerformanceScheduleDetailRes getSchedule(Long performanceId, Long scheduleId) {
		PerformanceSchedule schedule = performanceScheduleRepository
			.findByPerformance_PerformanceIdAndPerformanceScheduleId(performanceId, scheduleId)
			.orElseThrow(() -> new BusinessException(PerformanceScheduleErrorCode.SCHEDULE_NOT_FOUND));

		return PerformanceScheduleDetailRes.from(schedule);
	}

	private void validateBookingTime(PerformanceScheduleCreateReq request) {
		if (request.bookingOpenAt() == null || request.bookingCloseAt() == null) {
			throw new BusinessException(PerformanceScheduleErrorCode.INVALID_BOOKING_TIME);
		}
		if (!request.bookingOpenAt().isBefore(request.bookingCloseAt())) {
			throw new BusinessException(PerformanceScheduleErrorCode.INVALID_BOOKING_TIME);
		}
	}
}
