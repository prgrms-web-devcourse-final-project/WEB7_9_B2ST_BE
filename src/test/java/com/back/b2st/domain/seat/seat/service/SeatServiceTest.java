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

import com.back.b2st.domain.seat.seat.dto.response.DetailSeatInfo;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.error.SeatErrorCode;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.error.SectionErrorCode;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
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

	private Section section1A;
	private Seat seat;

	@BeforeEach
	void setUp() {
		section1A = Section.builder()
			.venueId(1L)
			.sectionName("A")
			.build();

		sectionRepository.save(section1A);

		seat = Seat.builder()
			.venueId(1L)
			.sectionId(section1A.getId())
			.sectionName(section1A.getSectionName())
			.rowLabel("1")
			.seatNumber(7)
			.build();

		seat = seatRepository.save(seat);
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
		DetailSeatInfo findSeat = seatService.getSeatInfoBySeatId(seatId);

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
		List<DetailSeatInfo> findSeat = seatService.getSeatInfoBySectionId(sectionId);

		// then
		// assertThat(findSeat.sectionName()).isEqualTo(sectionName);
		// assertThat(findSeat.rowLabel()).isEqualTo(rowLabel);
		// assertThat(findSeat.seatNumber()).isEqualTo(seatNumber);
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
}