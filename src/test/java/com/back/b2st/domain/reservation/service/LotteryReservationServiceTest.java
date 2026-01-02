package com.back.b2st.domain.reservation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.reservation.dto.response.LotteryReservationCreatedRes;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationSeat;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.reservation.repository.ReservationSeatRepository;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class LotteryReservationServiceTest {

	@Mock
	ReservationRepository reservationRepository;

	@Mock
	ReservationSeatRepository reservationSeatRepository;

	@Mock
	ScheduleSeatRepository scheduleSeatRepository;

	@InjectMocks
	LotteryReservationService lotteryReservationService;

	@Test
	@DisplayName("createCompletedReservation: 예매 생성 후 즉시 완료 처리된다")
	void createCompletedReservation_success() {

		// given
		Long memberId = 1L;
		Long scheduleId = 10L;

		when(reservationRepository.save(any(Reservation.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		// when
		LotteryReservationCreatedRes res =
			lotteryReservationService.createCompletedReservation(memberId, scheduleId);

		// then
		ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
		verify(reservationRepository).save(captor.capture());

		Reservation saved = captor.getValue();

		assertThat(saved.getMemberId()).isEqualTo(memberId);
		assertThat(saved.getScheduleId()).isEqualTo(scheduleId);
		assertThat(saved.getExpiresAt()).isNotNull();
		assertThat(res).isNotNull();
	}

	@Test
	@DisplayName("confirmAssignedSeats: 좌석 확정 성공")
	void confirmAssignedSeats_success() {

		// given
		Long reservationId = 100L;
		Long scheduleId = 200L;
		List<Long> scheduleSeatIds = List.of(1L, 2L, 3L);

		when(scheduleSeatRepository.updateStatusToSoldByScheduleSeatIds(
			eq(scheduleId),
			eq(scheduleSeatIds),
			eq(SeatStatus.AVAILABLE),
			eq(SeatStatus.SOLD)
		)).thenReturn(scheduleSeatIds.size());

		// when
		lotteryReservationService.confirmAssignedSeats(reservationId, scheduleId, scheduleSeatIds);

		// then
		verify(scheduleSeatRepository).updateStatusToSoldByScheduleSeatIds(
			scheduleId, scheduleSeatIds, SeatStatus.AVAILABLE, SeatStatus.SOLD
		);

		ArgumentCaptor<ReservationSeat> captor = ArgumentCaptor.forClass(ReservationSeat.class);
		verify(reservationSeatRepository, times(3)).save(captor.capture());

		List<ReservationSeat> savedSeats = captor.getAllValues();

		assertThat(savedSeats).hasSize(3);
		assertThat(savedSeats)
			.extracting(ReservationSeat::getReservationId)
			.containsOnly(reservationId);
		assertThat(savedSeats)
			.extracting(ReservationSeat::getScheduleSeatId)
			.containsExactlyInAnyOrderElementsOf(scheduleSeatIds);
	}

	@Test
	@DisplayName("confirmAssignedSeats: 이미 판매된 좌석이 있으면 예외 발생")
	void confirmAssignedSeats_fail() {

		// given
		Long reservationId = 100L;
		Long scheduleId = 200L;
		List<Long> scheduleSeatIds = List.of(1L, 2L, 3L);

		when(scheduleSeatRepository.updateStatusToSoldByScheduleSeatIds(
			anyLong(), anyList(), any(), any()
		)).thenReturn(2);

		// when
		BusinessException ex = catchThrowableOfType(
			() -> lotteryReservationService.confirmAssignedSeats(reservationId, scheduleId, scheduleSeatIds),
			BusinessException.class
		);

		// then
		assertThat(ex).isNotNull();
		verify(reservationSeatRepository, never()).save(any());
	}
}
