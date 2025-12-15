package com.back.b2st.domain.reservation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.reservation.dto.request.ReservationReq;
import com.back.b2st.domain.reservation.dto.response.ReservationRes;
import com.back.b2st.domain.reservation.entity.ScheduleSeat;
import com.back.b2st.domain.reservation.entity.SeatStatus;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.reservation.repository.ScheduleSeatRepository;
import com.back.b2st.global.error.exception.BusinessException;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ReservationServiceV2Test {

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private ScheduleSeatRepository scheduleSeatRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@MockitoBean
	private SeatLockService seatLockService;

	private Long scheduleId = 1001L;
	private Long seatId = 55L;
	private Long memberId = 999L;

	@BeforeEach
	void setUp() {
		given(seatLockService.tryLock(any(), any(), any()))
			.willReturn("LOCK_VALUE");

		// 좌석 초기 데이터 저장
		ScheduleSeat seat = ScheduleSeat.builder()
			.scheduleId(scheduleId)
			.seatId(seatId)
			.build();
		scheduleSeatRepository.save(seat);
	}

	@Test
	void 예매_성공() {
		// given
		ReservationReq request = new ReservationReq(scheduleId, seatId);

		// when
		ReservationRes response = reservationService.createReservation(memberId, request);

		// then
		assertThat(response.reservationId()).isNotNull();
		assertThat(response.memberId()).isEqualTo(memberId);
		assertThat(response.seatId()).isEqualTo(seatId);

		ScheduleSeat seat = scheduleSeatRepository.findByScheduleIdAndSeatId(scheduleId, seatId).get();
		assertThat(seat.getStatus()).isEqualTo(SeatStatus.HOLD);
	}

	@Test
	void 이미_HOLD된_좌석이면_예매_실패() {
		// given - 좌석을 미리 HOLD 상태로 바꿈
		ScheduleSeat seat = scheduleSeatRepository.findByScheduleIdAndSeatId(scheduleId, seatId).get();
		seat.hold();

		ReservationReq request = new ReservationReq(scheduleId, seatId);

		// when & then
		assertThatThrownBy(() -> reservationService.createReservation(memberId, request))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("이미 다른 사용자가 선택한 좌석입니다.");
	}

	@Test
	void SOLD_좌석이면_예매_실패() {
		// given
		ScheduleSeat seat = scheduleSeatRepository.findByScheduleIdAndSeatId(scheduleId, seatId).get();
		seat.markSold();  // SOLD 상태로 변경

		ReservationReq request = new ReservationReq(scheduleId, seatId);

		// when & then
		assertThatThrownBy(() -> reservationService.createReservation(memberId, request))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("이미 판매된 좌석입니다.");
	}
}
