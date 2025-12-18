package com.back.b2st.domain.seat.grade.service;

import org.springframework.stereotype.Service;

import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.seat.grade.dto.request.CreateSeatGradeReq;
import com.back.b2st.domain.seat.grade.dto.response.SeatGradeInfoRes;
import com.back.b2st.domain.seat.grade.entity.SeatGrade;
import com.back.b2st.domain.seat.grade.entity.SeatGradeType;
import com.back.b2st.domain.seat.grade.error.SeatGradeErrorCode;
import com.back.b2st.domain.seat.grade.repository.SeatGradeRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatGradeService {

	private final SeatGradeRepository seatGradeRepository;
	private final PerformanceRepository performanceRepository;

	// 생성 - 단건?
	public SeatGradeInfoRes createSeatGradeInfo(Long performanceId, CreateSeatGradeReq request) {
		validatePerformance(performanceId);
		validateSeat(request.seatId());
		SeatGradeType grade = SeatGradeType.fromString(request.grade());

		SeatGrade seatGrade = SeatGrade.builder()
			.performanceId(performanceId)
			.seatId(request.seatId())
			.grade(grade)
			.price(request.price())
			.build();

		return SeatGradeInfoRes.from(seatGradeRepository.save(seatGrade));
	}

	private void validateSeat(Long seatId) {
		if (!seatGradeRepository.existsById(seatId)) {
			throw new BusinessException(SeatGradeErrorCode.SEAT_NOT_FOUND);
		}
	}

	private void validatePerformance(Long performanceId) {
		if (!performanceRepository.existsById(performanceId)) {
			throw new BusinessException(SeatGradeErrorCode.PERFORMANCE_NOT_FOUND);
		}
	}

}