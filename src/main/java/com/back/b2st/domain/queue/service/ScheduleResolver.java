package com.back.b2st.domain.queue.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.global.error.code.CommonErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ScheduleId를 PerformanceId로 변환하는 Resolver
 *
 * 대기열 도메인에서 scheduleId(회차)를 performanceId(공연)로 변환하는 책임 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
public class ScheduleResolver {

	private final PerformanceScheduleRepository performanceScheduleRepository;

	/**
	 * scheduleId를 performanceId로 변환
	 *
	 * @param scheduleId 공연 회차 ID
	 * @return 공연 ID
	 * @throws BusinessException scheduleId가 존재하지 않을 때
	 */
	public Long resolvePerformanceId(Long scheduleId) {
		PerformanceSchedule schedule = performanceScheduleRepository.findById(scheduleId)
			.orElseThrow(() -> {
				log.warn("Schedule not found - scheduleId: {}", scheduleId);
				return new BusinessException(CommonErrorCode.NOT_FOUND);
			});

		Long performanceId = schedule.getPerformance().getPerformanceId();
		log.debug("Resolved scheduleId: {} -> performanceId: {}", scheduleId, performanceId);
		return performanceId;
	}
}

