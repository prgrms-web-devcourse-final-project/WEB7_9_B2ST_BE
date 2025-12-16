package com.back.b2st.domain.seat.seat.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.seat.seat.dto.response.SeatInfoRes;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.error.SeatErrorCode;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.error.SectionErrorCode;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.domain.venue.venue.repository.VenueRepository;
import com.back.b2st.global.error.exception.BusinessException;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class SeatServiceTest {

	@Autowired
	private SeatService seatService;
	@Autowired
	private SeatRepository seatRepository;
	@Autowired
	private SectionRepository sectionRepository;
	@Autowired
	private VenueRepository venueRepository;

	private Section section1A;
	private Seat seat;
	private Venue venue;

	@BeforeEach
	void setUp() {
		venue = venueRepository.save(
			Venue.builder()
				.name("잠실실내체육관")
				.build());

		section1A = sectionRepository.save(
			Section.builder()
				.venueId(venue.getVenueId())
				.sectionName("A")
				.build()
		);

		seat = seatRepository.save(
			Seat.builder()
				.venueId(venue.getVenueId())
				.sectionId(section1A.getId())
				.sectionName("A")
				.rowLabel("1")
				.seatNumber(1)
				.build()
		);
	}

	@Test
	@DisplayName("좌석조회 - 성공, 좌석ID")
	void getSeatInfoBySeatId_success() {
		// given
		Long seatId = seat.getId();
		String sectionName = section1A.getSectionName();
		String rowLabel = seat.getRowLabel();
		int seatNumber = seat.getSeatNumber();

		// when
		SeatInfoRes findSeat = seatService.getSeatInfoBySeatId(seatId);

		// then
		assertThat(findSeat.sectionName()).isEqualTo(sectionName);
		assertThat(findSeat.rowLabel()).isEqualTo(rowLabel);
		assertThat(findSeat.seatNumber()).isEqualTo(seatNumber);
	}

	@Test
	@DisplayName("좌석조회 - 실패, 좌석ID")
	void getSeatInfoBySeatId_fail() {
		// given
		Long seatId = 99L;

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> seatService.getSeatInfoBySeatId(seatId));

		// then
		assertThat(e.getErrorCode()).isEqualTo(SeatErrorCode.SEAT_NOT_FOUND);
	}

	@Test
	@DisplayName("좌석조회 - 성공, 구역ID")
	void getSeatInfoBySectionId_success() {
		// given
		Long sectionId = section1A.getId();
		String sectionName = section1A.getSectionName();
		String rowLabel = seat.getRowLabel();
		int seatNumber = seat.getSeatNumber();

		// when
		List<SeatInfoRes> seats = seatService.getSeatInfoBySectionId(sectionId);

		// then
		assertThat(seats).isNotEmpty();

		SeatInfoRes findSeat = seats.get(0);
		assertThat(findSeat.sectionName()).isEqualTo(sectionName);
		assertThat(findSeat.rowLabel()).isEqualTo(rowLabel);
		assertThat(findSeat.seatNumber()).isEqualTo(seatNumber);
	}

	@Test
	@DisplayName("좌석조회 - 실패, 구역ID")
	void getSeatInfoBySectionId_fail() {
		// given
		Long sectionId = 99L;

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> seatService.getSeatInfoBySectionId(sectionId));

		// then
		assertThat(e.getErrorCode()).isEqualTo(SectionErrorCode.SECTION_NOT_FOUND);
	}

	@Test
	@DisplayName("좌석조회 - 성공, 공연장")
	void getSeatInfoByVenueId_success() {
		// given
		String sectionName = "A";
		String rowLabel = seat.getRowLabel();
		int seatNumber = seat.getSeatNumber();

		// when
		List<SeatInfoRes> seats = seatService.getSeatInfoByVenueId(venue.getVenueId());

		// then
		assertThat(seats).isNotEmpty();

		SeatInfoRes findSeat = seats.get(0);
		assertThat(findSeat.sectionName()).isEqualTo(sectionName);
		assertThat(findSeat.rowLabel()).isEqualTo(rowLabel);
		assertThat(findSeat.seatNumber()).isEqualTo(seatNumber);
	}

	@Test
	@DisplayName("좌석조회 - 실패, 공연장")
	void getSeatInfoByVenueId_fail() {
		// given
		Long venudId = 999L;

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> seatService.getSeatInfoByVenueId(venudId));

		// then
		assertThat(e.getErrorCode()).isEqualTo(SeatErrorCode.VENUE_NOT_FOUND);
	}
}