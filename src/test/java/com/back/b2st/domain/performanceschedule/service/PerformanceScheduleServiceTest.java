package com.back.b2st.domain.performanceschedule.service;

import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.dto.request.PerformanceScheduleCreateReq;
import com.back.b2st.domain.performanceschedule.dto.response.PerformanceScheduleCreateRes;
import com.back.b2st.domain.performanceschedule.dto.response.PerformanceScheduleListRes;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.error.PerformanceScheduleErrorCode;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.global.error.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PerformanceScheduleServiceTest {

	@Mock
	private PerformanceRepository performanceRepository;

	@Mock
	private PerformanceScheduleRepository performanceScheduleRepository;

	@InjectMocks
	private PerformanceScheduleService performanceScheduleService;

	@Test
	@DisplayName("회차 생성: 성공 시 생성된 정보가 담긴 DTO를 반환한다")
	void createSchedule_success() {
		// given
		Long performanceId = 1L;
		Performance performance = mock(Performance.class);
		given(performance.getPerformanceId()).willReturn(performanceId);
		given(performanceRepository.findById(performanceId)).willReturn(Optional.of(performance));

		// BookingType.FIRST_COME 사용
		PerformanceScheduleCreateReq req = createReq(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

		PerformanceSchedule saved = PerformanceSchedule.builder()
				.performance(performance)
				.roundNo(req.roundNo())
				.startAt(req.startAt())
				.bookingType(req.bookingType())
				.bookingOpenAt(req.bookingOpenAt())
				.bookingCloseAt(req.bookingCloseAt())
				.build();

		given(performanceScheduleRepository.save(any(PerformanceSchedule.class))).willReturn(saved);

		// when
		PerformanceScheduleCreateRes res = performanceScheduleService.createSchedule(performanceId, req);

		// then
		assertThat(res).isNotNull();
		assertThat(res.performanceId()).isEqualTo(performanceId);
		assertThat(res.roundNo()).isEqualTo(req.roundNo());
		then(performanceScheduleRepository).should().save(any(PerformanceSchedule.class));
	}

	@Test
	@DisplayName("회차 생성: 공연이 없으면 PERFORMANCE_NOT_FOUND 예외가 발생한다")
	void createSchedule_performanceNotFound() {
		// given
		Long performanceId = 1L;
		PerformanceScheduleCreateReq req = createReq(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
		given(performanceRepository.findById(performanceId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> performanceScheduleService.createSchedule(performanceId, req))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", PerformanceScheduleErrorCode.PERFORMANCE_NOT_FOUND);

		then(performanceScheduleRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("회차 생성: 예매 오픈 시간이 종료 시간보다 늦으면 INVALID_BOOKING_TIME 예외가 발생한다")
	void createSchedule_invalidBookingTime() {
		// given
		Long performanceId = 1L;
		Performance performance = mock(Performance.class);
		given(performanceRepository.findById(performanceId)).willReturn(Optional.of(performance));

		// 오픈(12/20)이 종료(12/19)보다 늦은 잘못된 케이스
		PerformanceScheduleCreateReq req = createReq(
				LocalDateTime.of(2025, 12, 20, 10, 0),
				LocalDateTime.of(2025, 12, 19, 10, 0)
		);

		// when & then
		assertThatThrownBy(() -> performanceScheduleService.createSchedule(performanceId, req))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", PerformanceScheduleErrorCode.INVALID_BOOKING_TIME);
	}

	@Test
	@DisplayName("공연별 목록 조회: 성공 시 목록 리스트를 반환한다")
	void getSchedules_success() {
		// given
		Long performanceId = 1L;
		given(performanceRepository.existsById(performanceId)).willReturn(true);

		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(performanceScheduleRepository.findAllByPerformance_PerformanceIdOrderByStartAtAsc(performanceId))
				.willReturn(List.of(schedule));

		// when
		List<PerformanceScheduleListRes> res = performanceScheduleService.getSchedules(performanceId);

		// then
		assertThat(res).hasSize(1);
	}

	@Test
	@DisplayName("단건 조회: 존재하지 않는 회차면 SCHEDULE_NOT_FOUND 예외가 발생한다")
	void getSchedule_notFound() {
		// given
		Long performanceId = 1L;
		Long scheduleId = 99L;
		given(performanceScheduleRepository.findByPerformance_PerformanceIdAndPerformanceScheduleId(performanceId, scheduleId))
				.willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> performanceScheduleService.getSchedule(performanceId, scheduleId))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", PerformanceScheduleErrorCode.SCHEDULE_NOT_FOUND);
	}

	// 헬퍼 메서드: BookingType.FIRST_COME으로 고정
	private PerformanceScheduleCreateReq createReq(LocalDateTime open, LocalDateTime close) {
		return new PerformanceScheduleCreateReq(
				LocalDateTime.now().plusDays(10),
				1,
				BookingType.FIRST_COME,
				open,
				close
		);
	}
}