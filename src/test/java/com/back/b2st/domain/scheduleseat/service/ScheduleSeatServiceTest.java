package com.back.b2st.domain.scheduleseat.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.entity.PerformanceStatus;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.scheduleseat.dto.response.ScheduleSeatViewRes;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.global.error.exception.BusinessException;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ScheduleSeatServiceTest {

	@Autowired
	private ScheduleSeatService scheduleSeatService;

	@Autowired
	private com.back.b2st.domain.venue.venue.repository.VenueRepository venueRepository;

	@Autowired
	private PerformanceRepository performanceRepository;

	@Autowired
	private PerformanceScheduleRepository performanceScheduleRepository;

	@Autowired
	private SectionRepository sectionRepository;

	@Autowired
	private SeatRepository seatRepository;

	@Autowired
	private ScheduleSeatRepository scheduleSeatRepository;

	private Long scheduleId;

	@BeforeEach
	void setUp() {
		// 공연장 생성
		Venue venue = venueRepository.save(
			Venue.builder()
				.name("테스트 공연장")
				.build()
		);

		// 공연 생성
		Performance performance = performanceRepository.save(
			Performance.builder()
				.venue(venue)
				.title("테스트 공연")
				.category("콘서트")
				.posterUrl("")
				.description(null)
				.startDate(LocalDateTime.now().plusDays(1))
				.endDate(LocalDateTime.now().plusDays(2))
				.status(PerformanceStatus.ON_SALE)
				.build()
		);

		// 공연 회차 생성
		PerformanceSchedule schedule = performanceScheduleRepository.save(
			PerformanceSchedule.builder()
				.performance(performance)
				.roundNo(1)
				.startAt(LocalDateTime.now().plusDays(1))
				.bookingType(BookingType.FIRST_COME)
				.bookingOpenAt(LocalDateTime.now().minusDays(1))
				.bookingCloseAt(LocalDateTime.now().plusDays(1))
				.build()
		);

		this.scheduleId = schedule.getPerformanceScheduleId();

		// 구역 생성
		Section section = sectionRepository.save(
			Section.builder()
				.venueId(venue.getVenueId())
				.sectionName("A")
				.build()
		);

		// 좌석 5개 + 회차별 좌석 5개 생성 (AVAILABLE)
		for (int seatNumber = 1; seatNumber <= 5; seatNumber++) {

			Seat seat = seatRepository.save(
				Seat.builder()
					.venueId(venue.getVenueId())
					.sectionId(section.getId())
					.sectionName(section.getSectionName())
					.rowLabel("1")
					.seatNumber(seatNumber)
					.build()
			);

			scheduleSeatRepository.save(
				ScheduleSeat.builder()
					.scheduleId(scheduleId)
					.seatId(seat.getId())
					.build() // 기본 상태: AVAILABLE
			);
		}
	}

	@DisplayName("특정 회차의 전체 좌석을 조회한다")
	@Test
	void getSeats_success() {
		// when
		List<ScheduleSeatViewRes> seats =
			scheduleSeatService.getSeats(scheduleId);

		// then
		assertThat(seats).isNotEmpty();
		assertThat(seats).hasSize(5);
	}

	@DisplayName("특정 회차에서 AVAILABLE 상태 좌석만 조회한다")
	@Test
	void getSeatsByStatus_available_only() {
		// when
		List<ScheduleSeatViewRes> seats =
			scheduleSeatService.getSeatsByStatus(scheduleId, SeatStatus.AVAILABLE);

		// then
		assertThat(seats).isNotEmpty();
		assertThat(seats)
			.allMatch(seat -> seat.status() == SeatStatus.AVAILABLE);
	}

	@DisplayName("좌석이 없는 회차를 조회하면 빈 리스트를 반환한다")
	@Test
	void getSeats_empty_result() {
		// given
		PerformanceSchedule emptySchedule = performanceScheduleRepository.save(
			PerformanceSchedule.builder()
				.performance(performanceRepository.findAll().get(0))
				.roundNo(99)
				.startAt(LocalDateTime.now().plusDays(3))
				.bookingType(BookingType.FIRST_COME)
				.bookingOpenAt(LocalDateTime.now())
				.bookingCloseAt(LocalDateTime.now().plusDays(2))
				.build()
		);

		// when
		List<ScheduleSeatViewRes> seats =
			scheduleSeatService.getSeats(emptySchedule.getPerformanceScheduleId());

		// then
		assertThat(seats).isEmpty();
	}

	@DisplayName("존재하지 않는 회차를 조회하면 예외가 발생한다")
	@Test
	void getSeats_schedule_not_found() {
		// given
		Long notExistScheduleId = 9999L;

		// when & then
		assertThatThrownBy(() ->
			scheduleSeatService.getSeats(notExistScheduleId)
		)
			.isInstanceOf(BusinessException.class);
	}

}
