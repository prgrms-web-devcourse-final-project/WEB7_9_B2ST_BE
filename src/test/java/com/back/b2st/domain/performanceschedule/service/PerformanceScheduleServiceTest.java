package com.back.b2st.domain.performanceschedule.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.dto.request.PerformanceScheduleCreateReq;
import com.back.b2st.domain.performanceschedule.dto.response.PerformanceScheduleCreateRes;
import com.back.b2st.domain.performanceschedule.dto.response.PerformanceScheduleListRes;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.error.PerformanceScheduleErrorCode;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.prereservation.policy.service.PrereservationTimeTableService;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class PerformanceScheduleServiceTest {

	@Mock
	private PerformanceRepository performanceRepository;

	@Mock
	private PerformanceScheduleRepository performanceScheduleRepository;

	@Mock
	private SeatRepository seatRepository;

	@Mock
	private ScheduleSeatRepository scheduleSeatRepository;

	@Mock
	private PrereservationTimeTableService prereservationTimeTableService;

	@InjectMocks
	private PerformanceScheduleService performanceScheduleService;

	@Test
	@DisplayName("회차 생성: 성공 시 생성된 정보가 담긴 DTO를 반환한다")
	void createSchedule_success() {
		// given
		Long performanceId = 1L;

		Performance performance = mock(Performance.class);
		Venue venue = mock(Venue.class);

		given(performanceRepository.findById(performanceId)).willReturn(Optional.of(performance));

		// 여기 핵심: mock이라 기본값(0) 나오는 걸 방지
		given(performance.getPerformanceId()).willReturn(performanceId);

		// createScheduleSeats() 내부에서 venueId 필요
		given(performance.getVenue()).willReturn(venue);
		given(venue.getVenueId()).willReturn(10L);

		PerformanceScheduleCreateReq req =
			createReq(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

		// save 결과는 id가 있어야 createScheduleSeats(saved.getPerformanceScheduleId(), ...)가 정상 동작
		PerformanceSchedule saved = mock(PerformanceSchedule.class);
		given(saved.getPerformanceScheduleId()).willReturn(35L);
		given(saved.getPerformance()).willReturn(performance);
		given(saved.getRoundNo()).willReturn(req.roundNo());
		given(saved.getStartAt()).willReturn(req.startAt());
		given(saved.getBookingType()).willReturn(req.bookingType());
		given(saved.getBookingOpenAt()).willReturn(req.bookingOpenAt());
		given(saved.getBookingCloseAt()).willReturn(req.bookingCloseAt());

		given(performanceScheduleRepository.save(any(PerformanceSchedule.class))).willReturn(saved);

		// createScheduleSeats() 흐름 mocking
		given(scheduleSeatRepository.existsByScheduleId(35L)).willReturn(false);

		Seat seat = mock(Seat.class);
		given(seat.getId()).willReturn(1L);
		given(seatRepository.findByVenueId(10L)).willReturn(List.of(seat));

		given(scheduleSeatRepository.saveAll(any(List.class))).willReturn(List.of(mock(ScheduleSeat.class)));

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
		given(performanceScheduleRepository
			.findByPerformance_PerformanceIdAndPerformanceScheduleId(performanceId, scheduleId))
			.willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> performanceScheduleService.getSchedule(performanceId, scheduleId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", PerformanceScheduleErrorCode.SCHEDULE_NOT_FOUND);
	}

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
