package com.back.b2st.domain.seat.seat.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.back.b2st.domain.seat.seat.dto.request.CreateSeatReq;
import com.back.b2st.domain.seat.seat.dto.response.CreateSeatRes;
import com.back.b2st.domain.seat.seat.dto.response.DetailSeatInfo;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.error.SeatErrorCode;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.service.SectionService;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatService {

	private final SeatRepository seatRepository;
	private final SectionService sectionService;

	public CreateSeatRes createSeatInfo(Long venueId, CreateSeatReq request) {
		// todo : venueId 검증
		Section section = validateSectionId(request.sectionId());
		// todo : 기등록 검증

		Seat seat = Seat.builder()
			.venueId(venueId)
			.sectionId(section.getId())
			.sectionName(section.getSectionName())
			.rowLabel(request.rowLabel())
			.seatNumber(request.seatNumber())
			.build();

		return new CreateSeatRes(seatRepository.save(seat));
	}

	private Section validateSectionId(Long sectionId) {
		return sectionService.getSection(sectionId);
	}

	public DetailSeatInfo getSeatInfoBySeatId(Long seatId) {
		Seat seat = seatRepository.findById(seatId)
			.orElseThrow(() -> new BusinessException(SeatErrorCode.SEAT_NOT_FOUND));
		return new DetailSeatInfo(seat);
	}

	public List<DetailSeatInfo> getSeatInfoBySectionId(Long sectionId) {
		validateSectionId(sectionId);
		List<Seat> seats = seatRepository.findBySectionId(sectionId);

		return seats.stream()
			.map(seat -> new DetailSeatInfo(
				seat.getSectionName(),
				seat.getRowLabel(),
				seat.getSeatNumber()
			)).toList();
	}
}
