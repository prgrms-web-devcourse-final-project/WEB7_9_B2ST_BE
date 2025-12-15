package com.back.b2st.domain.reservation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.back.b2st.domain.reservation.dto.request.ReservationRequest;
import com.back.b2st.domain.reservation.dto.response.ReservationResponse;
import com.back.b2st.domain.reservation.entity.ScheduleSeat;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.reservation.repository.ScheduleSeatRepository;

@SpringBootTest
@ActiveProfiles("test")
class ReservationServiceV1Test {

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private ScheduleSeatRepository scheduleSeatRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@MockitoBean
	private SeatLockService seatLockService;

	private final Long performanceId = 1001L;
	private final Long seatId = 55L;
	private final Long memberId = 999L;  // 가짜 회원 ID

	@BeforeEach
	void setUp() {
		given(seatLockService.tryLock(any(), any(), any()))
			.willReturn("LOCK_VALUE");

		reservationRepository.deleteAll();
		scheduleSeatRepository.deleteAll();

		// AVAILABLE 좌석 세팅
		ScheduleSeat seat = ScheduleSeat.builder()
			.scheduleId(performanceId)
			.seatId(seatId)
			.build();

		scheduleSeatRepository.save(seat);
	}

	@Test
	@DisplayName("예매 생성 성공 - 좌석 HOLD + Reservation 저장")
	void createReservationSuccess() {
		// given
		ReservationRequest request = new ReservationRequest(performanceId, seatId);

		// when
		ReservationResponse response = reservationService.createReservation(memberId, request);

		// then
		assertThat(response).isNotNull();
		assertThat(response.reservationId()).isNotNull();
		assertThat(response.memberId()).isEqualTo(memberId);
		assertThat(response.performanceId()).isEqualTo(performanceId);
		assertThat(response.seatId()).isEqualTo(seatId);

		// 좌석 상태가 HOLD로 변경되었는지 검증
		ScheduleSeat updatedSeat =
			scheduleSeatRepository.findByScheduleIdAndSeatId(performanceId, seatId)
				.orElseThrow();

		assertThat(updatedSeat.getStatus().name()).isEqualTo("HOLD");
	}
}
