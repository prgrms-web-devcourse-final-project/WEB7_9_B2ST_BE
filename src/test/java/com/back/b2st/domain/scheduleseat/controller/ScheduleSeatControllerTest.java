package com.back.b2st.domain.scheduleseat.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.entity.PerformanceStatus;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.domain.venue.venue.repository.VenueRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class ScheduleSeatControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private VenueRepository venueRepository;

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

	@Autowired
	private EntityManager em;

	private Long scheduleId;

	@BeforeEach
	void setUp() {
		// 공연장
		Venue venue = venueRepository.save(
			Venue.builder().name("좌석 조회 테스트 공연장").build()
		);

		// 공연
		Performance performance = performanceRepository.save(
			Performance.builder()
				.venue(venue)
				.title("좌석 조회 테스트 공연")
				.category("콘서트")
				.startDate(LocalDateTime.now().plusDays(1))
				.endDate(LocalDateTime.now().plusDays(2))
				.status(PerformanceStatus.ON_SALE)
				.build()
		);

		// 회차
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

		// 구역
		Section section = sectionRepository.save(
			Section.builder()
				.venueId(venue.getVenueId())
				.sectionName("A")
				.build()
		);

		// 좌석 5개 (AVAILABLE)
		for (int i = 1; i <= 5; i++) {
			Seat seat = seatRepository.save(
				Seat.builder()
					.venueId(venue.getVenueId())
					.sectionId(section.getId())
					.sectionName(section.getSectionName())
					.rowLabel("1")
					.seatNumber(i)
					.build()
			);

			scheduleSeatRepository.save(
				ScheduleSeat.builder()
					.scheduleId(scheduleId)
					.seatId(seat.getId())
					.build()
			);
		}

		em.flush();
		em.clear();
	}

	@DisplayName("회차별 전체 좌석 조회 API")
	@Test
	void getScheduleSeats_success() throws Exception {
		mockMvc.perform(
				get("/api/schedules/{scheduleId}/seats", scheduleId)
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(5));
	}

	@DisplayName("회차별 AVAILABLE 좌석만 조회 API")
	@Test
	void getScheduleSeats_by_status() throws Exception {
		mockMvc.perform(
				get("/api/schedules/{scheduleId}/seats", scheduleId)
					.param("status", SeatStatus.AVAILABLE.name())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data[0].status").value("AVAILABLE"));
	}
}
