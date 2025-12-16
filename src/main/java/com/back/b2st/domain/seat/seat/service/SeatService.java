package com.back.b2st.domain.seat.seat.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.back.b2st.domain.seat.seat.dto.request.CreateSeatReq;
import com.back.b2st.domain.seat.seat.dto.response.CreateSeatRes;
import com.back.b2st.domain.seat.seat.dto.response.SeatInfoRes;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.error.SeatErrorCode;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.service.SectionService;
import com.back.b2st.domain.venue.venue.repository.VenueRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatService {

	private final SeatRepository seatRepository;
	private final SectionService sectionService;
	private final VenueRepository venueRepository;

	// 생성
	public CreateSeatRes createSeatInfo(Long venueId, CreateSeatReq request) {
		validateVenueId(venueId);
		Section section = validateSectionId(request.sectionId());
		validateSeatInfo(venueId, request.sectionId(), section.getSectionName(), request.rowLabel(),
			request.seatNumber());

		Seat seat = Seat.builder()
			.venueId(venueId)
			.sectionId(section.getId())
			.sectionName(section.getSectionName())
			.rowLabel(request.rowLabel())
			.seatNumber(request.seatNumber())
			.build();

		return CreateSeatRes.from(seatRepository.save(seat));
	}

	// 기등록 검증
	private void validateSeatInfo(Long venueId, Long sectionId, String sectionName, String rowLabel,
		Integer seatNumber) {
		if (seatRepository.existsByVenueIdAndSectionIdAndSectionNameAndRowLabelAndSeatNumber(venueId, sectionId,
			sectionName, rowLabel, seatNumber)) {
			throw new BusinessException(SeatErrorCode.ALREADY_CREATE_SEAT);
		}
	}

	// 구역 id 검증
	private Section validateSectionId(Long sectionId) {
		return sectionService.getSection(sectionId);
	}

	// 조회 - 좌석 id
	public SeatInfoRes getSeatInfoBySeatId(Long seatId) {
		Seat seat = seatRepository.findById(seatId)
			.orElseThrow(() -> new BusinessException(SeatErrorCode.SEAT_NOT_FOUND));
		return SeatInfoRes.from(seat);
	}

	// 조회 - 구역 id
	public List<SeatInfoRes> getSeatInfoBySectionId(Long sectionId) {
		validateSectionId(sectionId);
		List<Seat> seats = seatRepository.findBySectionId(sectionId);

		return seats.stream().map(SeatInfoRes::from).toList();
	}

	// 조회 - 공연장 id
	// todo seatInfo 만들어서 필요한 정보만 받아오도록 쿼리 작성
	public List<SeatInfoRes> getSeatInfoByVenueId(Long venudId) {
		validateVenueId(venudId);
		List<Seat> seats = seatRepository.findByVenueId(venudId);

		return seats.stream().map(SeatInfoRes::from).toList();
	}

	private void validateVenueId(Long venudId) {
		if (!(venueRepository.existsById(venudId))) {
			throw new BusinessException(SeatErrorCode.VENUE_NOT_FOUND);
		}
	}
}
