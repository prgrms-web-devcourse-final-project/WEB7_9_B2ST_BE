package com.back.b2st.domain.reservation.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.reservation.dto.request.ReservationReq;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReservationServiceTest {

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private ScheduleSeatRepository scheduleSeatRepository;

	@Test
	@DisplayName("예매하기 - 좌석이 존재하지 않으면 예외 발생")
	void 예매하기_좌석이없으면_예외발생() {
		// given
		Long memberId = 1L;
		Long scheduleId = 10L;
		Long seatId = 100L;

		ReservationReq request = new ReservationReq(scheduleId, seatId);

		// when & then
		assertThatThrownBy(() ->
			reservationService.createReservation(memberId, request)
		)
			.isInstanceOf(Throwable.class);
	}

	@Test
	@DisplayName("예매하기 - SOLD 좌석이면 예외 발생")
	void 예매하기_SOLD좌석이면_예외발생() {
		// given
		Long memberId = 1L;
		Long scheduleId = 10L;
		Long seatId = 100L;

		ScheduleSeat seat = scheduleSeatRepository.save(
			ScheduleSeat.builder()
				.scheduleId(scheduleId)
				.seatId(seatId)
				.build()
		);
		seat.sold();

		ReservationReq request = new ReservationReq(scheduleId, seatId);

		// when & then
		assertThatThrownBy(() ->
			reservationService.createReservation(memberId, request)
		)
			.isInstanceOf(Throwable.class);
	}

	@Test
	@DisplayName("예매하기 - 이미 HOLD 된 좌석이면 예외 발생")
	void 예매하기_HOLD좌석이면_예외발생() {
		// given
		Long memberId = 1L;
		Long scheduleId = 10L;
		Long seatId = 100L;

		ScheduleSeat seat = scheduleSeatRepository.save(
			ScheduleSeat.builder()
				.scheduleId(scheduleId)
				.seatId(seatId)
				.build()
		);
		seat.hold();

		ReservationReq request = new ReservationReq(scheduleId, seatId);

		// when & then
		assertThatThrownBy(() ->
			reservationService.createReservation(memberId, request)
		)
			.isInstanceOf(Throwable.class);
	}
}
