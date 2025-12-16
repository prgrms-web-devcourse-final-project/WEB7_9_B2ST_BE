package com.back.b2st.domain.reservation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.reservation.dto.request.ReservationReq;
import com.back.b2st.domain.reservation.dto.response.ReservationRes;
import com.back.b2st.domain.reservation.repository.ReservationRepository;

@ExtendWith(MockitoExtension.class)
class ReservationServiceV3Test {

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private SeatHoldTokenService seatHoldTokenService;

	@Mock
	private ScheduleSeatService scheduleSeatService;

	@InjectMocks
	private ReservationService reservationService;

	@Test
	void 예매_생성_성공() {
		// given
		Long memberId = 1L;
		ReservationReq request = new ReservationReq(1001L, 55L);

		willDoNothing().given(seatHoldTokenService)
			.validateOwnership(any(), any(), any());

		willDoNothing().given(scheduleSeatService)
			.getHoldSeatOrThrow(any(), any());

		given(reservationRepository.existsByScheduleIdAndSeatId(any(), any()))
			.willReturn(false);

		given(reservationRepository.save(any()))
			.willAnswer(invocation -> invocation.getArgument(0));

		// when
		ReservationRes result =
			reservationService.createReservation(memberId, request);

		// then
		assertThat(result).isNotNull();
		assertThat(result.scheduleId()).isEqualTo(1001L);
		assertThat(result.seatId()).isEqualTo(55L);
		assertThat(result.memberId()).isEqualTo(memberId);
	}
	
}
