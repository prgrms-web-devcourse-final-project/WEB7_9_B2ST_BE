package com.back.b2st.domain.reservation.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.reservation.entity.ScheduleSeat;
import com.back.b2st.domain.reservation.entity.SeatStatus;
import com.back.b2st.domain.reservation.error.ReservationErrorCode;
import com.back.b2st.domain.reservation.repository.ScheduleSeatRepository;
import com.back.b2st.global.error.exception.BusinessException;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class SeatHoldServiceTest {

	@Autowired
	private SeatHoldService seatHoldService;

	@Autowired
	private ScheduleSeatRepository scheduleSeatRepository;

	private Long scheduleId = 1000L;
	private Long seatId = 50L;

	@BeforeEach
	void setUp() {
		// given 테스트용 좌석 데이터 생성
		ScheduleSeat seat = ScheduleSeat.builder()
			.scheduleId(scheduleId)
			.seatId(seatId)
			.build();

		scheduleSeatRepository.save(seat);
	}

	@Test
	@DisplayName("좌석 HOLD 성공 - AVAILABLE → HOLD")
	void holdSeat_success() {
		// when 좌석 HOLD 서비스 호출
		seatHoldService.holdSeat(scheduleId, seatId);

		// then 해당 좌석 상태가 HOLD 로 변경되었는지 검증
		ScheduleSeat updated = scheduleSeatRepository.findByScheduleIdAndSeatId(scheduleId, seatId)
			.orElseThrow();

		assertThat(updated.getStatus()).isEqualTo(SeatStatus.HOLD);
	}

	@Test
	@DisplayName("이미 SOLD 좌석을 HOLD 시도하면 실패해야 한다")
	void holdSeat_fail_soldSeat() {
		// given 해당 좌석 상태를 SOLD 로 설정
		ScheduleSeat seat = scheduleSeatRepository.findByScheduleIdAndSeatId(scheduleId, seatId)
			.orElseThrow();
		seat.markSold(); // SOLD로 상태 변경

		// when / then BusinessException 발생해야 함
		BusinessException ex = assertThrows(
			BusinessException.class,
			() -> seatHoldService.holdSeat(scheduleId, seatId)
		);

		assertThat(ex.getErrorCode()).isEqualTo(ReservationErrorCode.SEAT_ALREADY_SOLD);
	}

	@Test
	@DisplayName("이미 HOLD 된 좌석을 다시 HOLD 시도하면 실패해야 한다")
	void holdSeat_fail_holdSeat() {
		// given 먼저 HOLD 처리
		seatHoldService.holdSeat(scheduleId, seatId);

		// when / then 같은 좌석 다시 HOLD 시 예외 발생
		BusinessException ex = assertThrows(
			BusinessException.class,
			() -> seatHoldService.holdSeat(scheduleId, seatId)
		);

		assertThat(ex.getErrorCode()).isEqualTo(ReservationErrorCode.SEAT_ALREADY_HOLD);
	}
}
