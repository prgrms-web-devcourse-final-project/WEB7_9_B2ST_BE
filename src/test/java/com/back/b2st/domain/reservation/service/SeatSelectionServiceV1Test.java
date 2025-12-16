package com.back.b2st.domain.reservation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.reservation.entity.ScheduleSeat;
import com.back.b2st.domain.reservation.entity.SeatStatus;
import com.back.b2st.domain.reservation.repository.ScheduleSeatRepository;
import com.back.b2st.global.error.exception.BusinessException;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Disabled
class SeatSelectionServiceV1Test {

	@Autowired
	private SeatSelectionService seatSelectionService;

	@Autowired
	private ScheduleSeatRepository scheduleSeatRepository;

	@MockitoBean
	private SeatLockService seatLockService;

	private Long scheduleId = 1001L;
	private Long seatId = 77L;
	private Long memberId = 100L;

	@BeforeEach
	void setUp() {
		ScheduleSeat seat = ScheduleSeat.builder()
			.scheduleId(scheduleId)
			.seatId(seatId)
			.build();
		scheduleSeatRepository.save(seat);
	}

	@Test
	void 락_획득_성공시_HOLD_성공() {
		// given
		given(seatLockService.tryLock(any(), any(), any()))
			.willReturn("LOCK_VALUE");

		// when
		seatSelectionService.selectSeat(memberId, scheduleId, seatId);

		// then
		ScheduleSeat seat =
			scheduleSeatRepository.findByScheduleIdAndSeatId(scheduleId, seatId)
				.orElseThrow();

		assertThat(seat.getStatus()).isEqualTo(SeatStatus.HOLD);
	}

	@Test
	void 락_획득_실패시_좌석선택_실패() {
		// given
		given(seatLockService.tryLock(any(), any(), any()))
			.willReturn(null);

		// when & then
		assertThatThrownBy(() ->
			seatSelectionService.selectSeat(memberId, scheduleId, seatId)
		)
			.isInstanceOf(BusinessException.class);
	}
}
