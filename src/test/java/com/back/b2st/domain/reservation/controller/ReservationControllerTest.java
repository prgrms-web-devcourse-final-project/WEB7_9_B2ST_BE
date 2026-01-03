package com.back.b2st.domain.reservation.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.entity.PerformanceStatus;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.domain.venue.venue.repository.VenueRepository;
import com.back.b2st.security.UserPrincipal;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private PerformanceRepository performanceRepository;

	@Autowired
	private PerformanceScheduleRepository performanceScheduleRepository;

	@Autowired
	private VenueRepository venueRepository;

	@Autowired
	private SeatRepository seatRepository;

	private Authentication memberAuth;
	private Long memberId;

	@BeforeEach
	void setup() {
		reservationRepository.deleteAll();
		seatRepository.deleteAll();
		performanceScheduleRepository.deleteAll();
		performanceRepository.deleteAll();
		venueRepository.deleteAll();

		memberId = 1L;

		UserPrincipal member = UserPrincipal.builder()
			.id(memberId)
			.email("member@test.com")
			.role("ROLE_MEMBER")
			.build();

		memberAuth = new UsernamePasswordAuthenticationToken(member, null, null);
	}

	@Test
	@DisplayName("내 예매 목록 조회 성공")
	void getMyReservationsDetail_success() throws Exception {
		// given
		Venue venue = venueRepository.save(
			Venue.builder()
				.name("테스트 공연장")
				.build()
		);

		Performance performance = performanceRepository.save(
			Performance.builder()
				.venue(venue)
				.title("테스트 공연")
				.category("콘서트")
				.startDate(LocalDateTime.now())
				.endDate(LocalDateTime.now().plusDays(1))
				.status(PerformanceStatus.ACTIVE)
				.build()
		);

		PerformanceSchedule schedule = performanceScheduleRepository.save(
			PerformanceSchedule.builder()
				.performance(performance)
				.roundNo(1)
				.startAt(LocalDateTime.now())
				.bookingOpenAt(LocalDateTime.now().minusDays(1))
				.bookingCloseAt(LocalDateTime.now().plusDays(1))
				.bookingType(BookingType.FIRST_COME)
				.build()
		);

		Seat seat1 = seatRepository.save(
			Seat.builder()
				.venueId(venue.getVenueId())
				.sectionId(1L)
				.sectionName("A")
				.rowLabel("1열")
				.seatNumber(1)
				.build()
		);

		Seat seat2 = seatRepository.save(
			Seat.builder()
				.venueId(venue.getVenueId())
				.sectionId(1L)
				.sectionName("A")
				.rowLabel("1열")
				.seatNumber(2)
				.build()
		);

		reservationRepository.save(
			Reservation.builder()
				.memberId(memberId)
				.scheduleId(schedule.getPerformanceScheduleId())
				.expiresAt(LocalDateTime.now().plusMinutes(5))
				.build()
		);

		reservationRepository.save(
			Reservation.builder()
				.memberId(memberId)
				.scheduleId(schedule.getPerformanceScheduleId())
				.expiresAt(LocalDateTime.now().plusMinutes(5))
				.build()
		);

		// when & then
		mockMvc.perform(get("/api/reservations/me")
				.with(authentication(memberAuth))
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(2));
	}
}
