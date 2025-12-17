package com.back.b2st.domain.reservation.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.reservation.dto.request.ReservationReq;
import com.back.b2st.domain.reservation.entity.ScheduleSeat;
import com.back.b2st.domain.reservation.repository.ScheduleSeatRepository;
import com.back.b2st.global.error.exception.BusinessException;

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
	void 예매하기_락획득실패시_예외발생() {
		// given
		Long memberId = 1L;
		Long scheduleId = 10L;
		Long seatId = 100L;

		ReservationReq request = new ReservationReq(scheduleId, seatId);

		// when & then
		assertThatThrownBy(() ->
			reservationService.createReservation(memberId, request)
		)
			.isInstanceOf(BusinessException.class)
			.hasMessage("좌석을 찾을 수 없습니다.");
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
			.isInstanceOf(BusinessException.class)
			.hasMessage("이미 판매된 좌석입니다.");
	}

	@Test
	@DisplayName("예매하기 - 이미 HOLD 된 좌석이면 예외 발생")
	void 예매하기_이미_예매된좌석이면_중복예외() {
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
			.isInstanceOf(BusinessException.class)
			.hasMessage("이미 다른 사용자가 선택한 좌석입니다.");
	}

	@Test
	@DisplayName("예매하기 - 정상 예매 성공 시 상세 정보 반환")
	@Disabled
	void 예매하기_성공() {
		// given
		Long memberId = 1L;
		Long scheduleId = 10L;
		Long seatId = 100L;

		// 회차별 좌석이 AVAILABLE 상태로 존재
		scheduleSeatRepository.save(
			ScheduleSeat.builder()
				.scheduleId(scheduleId)
				.seatId(seatId)
				.build()
		);

		ReservationReq request = new ReservationReq(scheduleId, seatId);

		// when
		var result = reservationService.createReservation(memberId, request);

		// then
		assertThat(result).isNotNull();
		assertThat(result.reservationId()).isNotNull();

		assertThat(result.status()).isNotNull();

		assertThat(result.performance()).isNotNull();
		//assertThat(result.performance().performanceId()).isEqualTo(scheduleId);

		assertThat(result.seat()).isNotNull();
		//assertThat(result.seat().seatId()).isEqualTo(seatId);
	}

}
