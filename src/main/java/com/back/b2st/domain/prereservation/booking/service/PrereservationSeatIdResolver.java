package com.back.b2st.domain.prereservation.booking.service;

import org.springframework.stereotype.Component;

import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;

import lombok.RequiredArgsConstructor;

/**
 * 프론트 연동에서 seatId 대신 scheduleSeatId가 넘어오는 경우를 흡수하기 위한 resolver.
 * - 정상 케이스: seatId 그대로 반환
 * - 오입력 케이스: scheduleSeatId → seatId로 변환
 */
@Component
@RequiredArgsConstructor
public class PrereservationSeatIdResolver {

	private final ScheduleSeatRepository scheduleSeatRepository;

	public Long resolveSeatId(Long scheduleId, Long seatIdOrScheduleSeatId) {
		// 정상 케이스(= seatId)면 scheduleId+seatId 매핑이 존재한다.
		if (scheduleSeatRepository.findByScheduleIdAndSeatId(scheduleId, seatIdOrScheduleSeatId).isPresent()) {
			return seatIdOrScheduleSeatId;
		}

		// 오입력 케이스(= scheduleSeatId)면 PK로 조회해서 seatId로 변환한다.
		return scheduleSeatRepository.findById(seatIdOrScheduleSeatId)
			.filter(scheduleSeat -> scheduleId.equals(scheduleSeat.getScheduleId()))
			.map(ScheduleSeat::getSeatId)
			.orElse(seatIdOrScheduleSeatId);
	}
}

