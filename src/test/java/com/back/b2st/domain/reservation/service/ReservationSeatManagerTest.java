package com.back.b2st.domain.reservation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.reservation.dto.response.SeatReservationResult;
import com.back.b2st.domain.reservation.entity.ReservationSeat;
import com.back.b2st.domain.reservation.repository.ReservationSeatRepository;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.scheduleseat.service.ScheduleSeatService;
import com.back.b2st.domain.scheduleseat.service.ScheduleSeatStateService;

@ExtendWith(MockitoExtension.class)
class ReservationSeatManagerTest {

	@Mock
	private ReservationSeatRepository reservationSeatRepository;

	@Mock
	private ScheduleSeatRepository scheduleSeatRepository;

	@Mock
	private ScheduleSeatService scheduleSeatService;

	@Mock
	private ScheduleSeatStateService scheduleSeatStateService;

	@InjectMocks
	private ReservationSeatManager reservationSeatManager;

	private static final Long SCHEDULE_ID = 1L;
	private static final Long MEMBER_ID = 10L;
	private static final Long RESERVATION_ID = 100L;

	private static final Long SEAT_ID_1 = 1000L;
	private static final Long SEAT_ID_2 = 2000L;

	private static final Long SCHEDULE_SEAT_ID_1 = 11L;
	private static final Long SCHEDULE_SEAT_ID_2 = 22L;

	@Test
	@DisplayName("prepareSeatReservation(): scheduleSeatId 목록과 가장 빠른 HOLD 만료 시각 반환")
	void prepareSeatReservation_success() {
		// given
		LocalDateTime expires1 = LocalDateTime.now().plusMinutes(10);
		LocalDateTime expires2 = LocalDateTime.now().plusMinutes(5);

		ScheduleSeat seat1 = mock(ScheduleSeat.class);
		ScheduleSeat seat2 = mock(ScheduleSeat.class);

		when(seat1.getId()).thenReturn(SCHEDULE_SEAT_ID_1);
		when(seat1.getHoldExpiredAt()).thenReturn(expires1);

		when(seat2.getId()).thenReturn(SCHEDULE_SEAT_ID_2);
		when(seat2.getHoldExpiredAt()).thenReturn(expires2);

		when(scheduleSeatService.validateAndGetAttachableSeat(SCHEDULE_ID, SEAT_ID_1, MEMBER_ID))
			.thenReturn(seat1);
		when(scheduleSeatService.validateAndGetAttachableSeat(SCHEDULE_ID, SEAT_ID_2, MEMBER_ID))
			.thenReturn(seat2);

		// when
		SeatReservationResult result =
			reservationSeatManager.prepareSeatReservation(
				SCHEDULE_ID,
				List.of(SEAT_ID_1, SEAT_ID_2),
				MEMBER_ID
			);

		// then
		assertThat(result.scheduleSeatIds())
			.containsExactlyInAnyOrder(SCHEDULE_SEAT_ID_1, SCHEDULE_SEAT_ID_2);

		assertThat(result.expiresAt()).isEqualTo(expires2);
	}

	@Test
	@DisplayName("attachSeats(): 예매 좌석을 저장한다")
	void attachSeats_success() {
		// when
		reservationSeatManager.attachSeats(
			RESERVATION_ID,
			List.of(SCHEDULE_SEAT_ID_1, SCHEDULE_SEAT_ID_2)
		);

		// then
		verify(reservationSeatRepository, times(2))
			.save(any(ReservationSeat.class));
	}

	@Test
	@DisplayName("releaseAllSeats(): 예매 좌석 전부 HOLD 해제 위임")
	void releaseAllSeats_success() {
		// given
		ScheduleSeat scheduleSeat1 = mock(ScheduleSeat.class);
		ScheduleSeat scheduleSeat2 = mock(ScheduleSeat.class);

		when(scheduleSeat1.getScheduleId()).thenReturn(SCHEDULE_ID);
		when(scheduleSeat1.getSeatId()).thenReturn(SEAT_ID_1);

		when(scheduleSeat2.getScheduleId()).thenReturn(SCHEDULE_ID);
		when(scheduleSeat2.getSeatId()).thenReturn(SEAT_ID_2);

		ReservationSeat rs1 = ReservationSeat.builder()
			.reservationId(RESERVATION_ID)
			.scheduleSeatId(SCHEDULE_SEAT_ID_1)
			.build();

		ReservationSeat rs2 = ReservationSeat.builder()
			.reservationId(RESERVATION_ID)
			.scheduleSeatId(SCHEDULE_SEAT_ID_2)
			.build();

		when(reservationSeatRepository.findByReservationId(RESERVATION_ID))
			.thenReturn(List.of(rs1, rs2));

		when(scheduleSeatRepository.findById(SCHEDULE_SEAT_ID_1))
			.thenReturn(Optional.of(scheduleSeat1));
		when(scheduleSeatRepository.findById(SCHEDULE_SEAT_ID_2))
			.thenReturn(Optional.of(scheduleSeat2));

		// when
		reservationSeatManager.releaseAllSeats(RESERVATION_ID);

		// then
		verify(scheduleSeatStateService)
			.releaseHold(SCHEDULE_ID, SEAT_ID_1);
		verify(scheduleSeatStateService)
			.releaseHold(SCHEDULE_ID, SEAT_ID_2);
	}

	@Test
	@DisplayName("confirmAllSeats(): 예매 좌석 전부 SOLD 처리 위임")
	void confirmAllSeats_success() {
		// given
		ScheduleSeat scheduleSeat1 = mock(ScheduleSeat.class);
		ScheduleSeat scheduleSeat2 = mock(ScheduleSeat.class);

		when(scheduleSeat1.getScheduleId()).thenReturn(SCHEDULE_ID);
		when(scheduleSeat1.getSeatId()).thenReturn(SEAT_ID_1);

		when(scheduleSeat2.getScheduleId()).thenReturn(SCHEDULE_ID);
		when(scheduleSeat2.getSeatId()).thenReturn(SEAT_ID_2);

		ReservationSeat rs1 = ReservationSeat.builder()
			.reservationId(RESERVATION_ID)
			.scheduleSeatId(SCHEDULE_SEAT_ID_1)
			.build();

		ReservationSeat rs2 = ReservationSeat.builder()
			.reservationId(RESERVATION_ID)
			.scheduleSeatId(SCHEDULE_SEAT_ID_2)
			.build();

		when(reservationSeatRepository.findByReservationId(RESERVATION_ID))
			.thenReturn(List.of(rs1, rs2));

		when(scheduleSeatRepository.findById(SCHEDULE_SEAT_ID_1))
			.thenReturn(Optional.of(scheduleSeat1));
		when(scheduleSeatRepository.findById(SCHEDULE_SEAT_ID_2))
			.thenReturn(Optional.of(scheduleSeat2));

		// when
		reservationSeatManager.confirmAllSeats(RESERVATION_ID);

		// then
		verify(scheduleSeatStateService)
			.confirmHold(SCHEDULE_ID, SEAT_ID_1);
		verify(scheduleSeatStateService)
			.confirmHold(SCHEDULE_ID, SEAT_ID_2);
	}
}
