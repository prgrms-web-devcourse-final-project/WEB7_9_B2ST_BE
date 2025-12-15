package com.back.b2st.domain.performanceschedule.service;

import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.dto.request.PerformanceScheduleCreateReq;
import com.back.b2st.domain.performanceschedule.dto.response.PerformanceScheduleCreateRes;
import com.back.b2st.domain.performanceschedule.dto.response.PerformanceScheduleDetailRes;
import com.back.b2st.domain.performanceschedule.dto.response.PerformanceScheduleListRes;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.error.PerformanceScheduleErrorCode;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.global.error.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceScheduleService {

	private final PerformanceRepository performanceRepository;
	private final PerformanceScheduleRepository performanceScheduleRepository;

	/**
	 * 회차 생성 (관리자)
	 */
	@Transactional
	public PerformanceScheduleCreateRes createSchedule(PerformanceScheduleCreateReq request) {
		Performance performance = performanceRepository.findById(request.performanceId())
				.orElseThrow(() -> new BusinessException(PerformanceScheduleErrorCode.PERFORMANCE_NOT_FOUND));

		// 시간/라운드 중복 방지 규칙은 후에 repository 기반으로 여기서 검증
		// validateDuplicatedRoundNo(performance.getPerformanceId(), request.roundNo());
		// validateDuplicatedStartAt(performance.getPerformanceId(), request.startAt());
		//생성되는 회차의 bookingType이 BOOKING_ORDER(선착순)라면
		// 해당 회차를 위한 Queue 설정 레코드도 자동으로 생성(추후구현)


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
		return PerformanceScheduleCreateRes.from(saved);
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
	 */
	public PerformanceScheduleDetailRes getSchedule(Long performanceScheduleId) {
		PerformanceSchedule schedule = performanceScheduleRepository.findById(performanceScheduleId)
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
