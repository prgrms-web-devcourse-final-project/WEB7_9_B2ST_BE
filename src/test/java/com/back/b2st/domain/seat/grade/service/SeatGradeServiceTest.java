package com.back.b2st.domain.seat.grade.service;

import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.entity.PerformanceStatus;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.seat.grade.dto.request.CreateSeatGradeReq;
import com.back.b2st.domain.seat.grade.dto.response.SeatGradeInfoRes;
import com.back.b2st.domain.seat.grade.entity.SeatGrade;
import com.back.b2st.domain.seat.grade.error.SeatGradeErrorCode;
import com.back.b2st.domain.seat.grade.repository.SeatGradeRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.domain.venue.venue.repository.VenueRepository;
import com.back.b2st.global.error.exception.BusinessException;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class SeatGradeServiceTest {

	@Autowired
	private SeatGradeService seatGradeService;
	@Autowired
	private SeatGradeRepository seatGradeRepository;
	@Autowired
	private PerformanceRepository performanceRepository;
	@Autowired
	private VenueRepository venueRepository;
	@Autowired
	private SectionRepository sectionRepository;
	@Autowired
	private SeatRepository seatRepository;
	@Autowired
	private PerformanceScheduleRepository performanceScheduleRepository;

	private Performance performance;
	private Section section;
	private Seat seat;
	private PerformanceSchedule performanceSchedule;

	@BeforeEach
	void setUp() {
		// 공연, 좌석
		Venue venue = venueRepository.save(Venue.builder()
			.name("잠실실내체육관")
			.build());

		performance = performanceRepository.save(Performance.builder()
			.venue(venue)
			.title("2024 아이유 콘서트 - HEREH WORLD TOUR")
			.category("콘서트")
			.posterUrl("")
			.description(null)
			.startDate(LocalDateTime.of(2024, 12, 20, 19, 0))
			.endDate(LocalDateTime.of(2024, 12, 22, 21, 0))
			.status(PerformanceStatus.ACTIVE)
			.build());

		// Section 생성 추가
		section = sectionRepository.save(Section.builder()
			.venueId(venue.getVenueId())
			.sectionName("A")
			.build());

		// Seat 생성 추가
		seat = seatRepository.save(Seat.builder()
			.venueId(venue.getVenueId())
			.sectionId(section.getId())
			.sectionName("A")
			.rowLabel("8")
			.seatNumber(7)
			.build());

		performanceSchedule = performanceScheduleRepository.save(
			PerformanceSchedule.builder()
				.performance(performance)
				.startAt(LocalDateTime.of(2024, 12, 20, 19, 0))
				.roundNo(1)
				.bookingType(BookingType.LOTTERY)
				.bookingOpenAt(LocalDateTime.of(2024, 12, 10, 12, 0))
				.bookingCloseAt(LocalDateTime.of(2024, 12, 15, 23, 59))
				.build());
	}

	@Test
	@DisplayName("좌석등급 생성 - 성공")
	void createSeatGradeInfo_success() {
		// given
		CreateSeatGradeReq req = new CreateSeatGradeReq(
			seat.getId(),
			"VIP",
			10000
		);

		// when
		SeatGradeInfoRes saveGrade = seatGradeService.createSeatGradeInfo(performance.getPerformanceId(), req);

		// then
		assertThat(saveGrade.seatGradeId()).isNotNull();

		SeatGrade findGrade = seatGradeRepository.findById(saveGrade.seatGradeId()).orElseThrow();
		assertThat(findGrade.getId()).isEqualTo(saveGrade.seatGradeId());
	}

	@Test
	@DisplayName("좌석등급 생성 - 실패, 공연")
	void createSeatGradeInfo_fail_performance() {
		// given
		Long performanceId = 99L;
		CreateSeatGradeReq req = new CreateSeatGradeReq(
			seat.getId(),
			"VIP",
			10000
		);

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> seatGradeService.createSeatGradeInfo(performanceId, req));

		// then
		assertThat(e.getErrorCode()).isEqualTo(SeatGradeErrorCode.PERFORMANCE_NOT_FOUND);
	}

	@Test
	@DisplayName("좌석등급 생성 - 실패, 좌석")
	void createSeatGradeInfo_fail_seat() {
		// given
		Long performanceId = performance.getPerformanceId();
		Long seatId = 99999L;
		CreateSeatGradeReq req = new CreateSeatGradeReq(
			seatId,
			"VIP",
			10000
		);

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> seatGradeService.createSeatGradeInfo(performanceId, req));

		// then
		assertThat(e.getErrorCode()).isEqualTo(SeatGradeErrorCode.SEAT_NOT_FOUND);
	}

	@Test
	@DisplayName("좌석등급 생성 - 실패, 등급")
	void createSeatGradeInfo_fail_grade() {
		// given
		Long performanceId = performance.getPerformanceId();
		CreateSeatGradeReq req = new CreateSeatGradeReq(
			seat.getId(),
			"TEST",
			10000
		);

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> seatGradeService.createSeatGradeInfo(performanceId, req));

		// then
		assertThat(e.getErrorCode()).isEqualTo(SeatGradeErrorCode.INVALID_GRADE_TYPE);
	}
}