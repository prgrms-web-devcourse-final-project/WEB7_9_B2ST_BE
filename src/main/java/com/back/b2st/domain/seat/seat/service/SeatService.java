package com.back.b2st.domain.seat.seat.service;

import org.springframework.stereotype.Service;

import com.back.b2st.domain.seat.seat.dto.request.CreateSeatReq;
import com.back.b2st.domain.seat.seat.dto.response.CreateSeatRes;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.error.SeatErrorCode;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatService {

	private final SeatRepository seatRepository;
	private final SectionRepository sectionRepository;

	public CreateSeatRes createSeatInfo(Long venueId, CreateSeatReq request) {
		// todo : venueId 검증
		Section section = validateSectionId(request);
		// todo : 기등록 검증

		Seat seat = Seat.builder()
			.venueId(venueId)
			.sectionId(section.getId())
			.section(section.getSectionName())
			.rowLabel(request.rowLabel())
			.seatNumber(request.seatNumber())
			.build();

		return new CreateSeatRes(seatRepository.save(seat));
	}

	private Section validateSectionId(CreateSeatReq request) {
		return sectionRepository.findById(request.sectionId())
			.orElseThrow(() -> new BusinessException(SeatErrorCode.SECTION_NOT_FOUND));
	}

}
