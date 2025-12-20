package com.back.b2st.domain.scheduleseat.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.error.ScheduleSeatErrorCode;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class ScheduleSeatStateServiceTest {

	@Mock
	private ScheduleSeatLockService scheduleSeatLockService;

	@Mock
	private SeatHoldTokenService seatHoldTokenService;

	@Mock
	private ScheduleSeatRepository scheduleSeatRepository;

	@InjectMocks
	private ScheduleSeatStateService scheduleSeatStateService;

	private static final Long MEMBER_ID = 1L;
	private static final Long SCHEDULE_ID = 10L;
	private static final Long SEAT_ID = 100L;

	@Test
	@DisplayName("holdSeat(): 락 획득 실패 시 SEAT_LOCK_FAILED 예외를 던지고, 상태변경/토큰저장/언락을 수행하지 않는다")
	void holdSeat_lockFailed_throw() {
		// given
		when(scheduleSeatLockService.tryLock(SCHEDULE_ID, SEAT_ID, MEMBER_ID)).thenReturn(null);

		// when & then
		assertThrows(BusinessException.class, () -> scheduleSeatStateService.holdSeat(MEMBER_ID, SCHEDULE_ID, SEAT_ID));

		verify(scheduleSeatRepository, never()).findByScheduleIdAndSeatId(anyLong(), anyLong());
		verify(seatHoldTokenService, never()).save(anyLong(), anyLong(), anyLong());
		verify(scheduleSeatLockService, never()).unlock(anyLong(), anyLong(), org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	@DisplayName("holdSeat(): 락 획득 성공 시 changeToHold → 토큰 저장 → unlock 순서로 수행한다")
	void holdSeat_success_flowOrder() {
		// given
		String lockValue = "lock-value";
		ScheduleSeat seat = org.mockito.Mockito.mock(ScheduleSeat.class);

		when(scheduleSeatLockService.tryLock(SCHEDULE_ID, SEAT_ID, MEMBER_ID)).thenReturn(lockValue);
		when(scheduleSeatRepository.findByScheduleIdAndSeatId(SCHEDULE_ID, SEAT_ID)).thenReturn(Optional.of(seat));
		when(seat.getStatus()).thenReturn(SeatStatus.AVAILABLE);

		// when
		assertThatNoException().isThrownBy(() -> scheduleSeatStateService.holdSeat(MEMBER_ID, SCHEDULE_ID, SEAT_ID));

		// then (order)
		InOrder inOrder = inOrder(seat, seatHoldTokenService, scheduleSeatLockService);
		inOrder.verify(seat).hold();
		inOrder.verify(seatHoldTokenService).save(SCHEDULE_ID, SEAT_ID, MEMBER_ID);
		inOrder.verify(scheduleSeatLockService).unlock(SCHEDULE_ID, SEAT_ID, lockValue);
	}

	@Test
	@DisplayName("holdSeat(): changeToHold에서 예외 발생해도 finally로 unlock은 반드시 수행되고, 토큰 저장은 수행하지 않는다")
	void holdSeat_changeToHoldThrows_unlockAlways() {
		// given
		String lockValue = "lock-value";
		ScheduleSeatStateService spyService = spy(
			new ScheduleSeatStateService(scheduleSeatLockService, seatHoldTokenService, scheduleSeatRepository));

		when(scheduleSeatLockService.tryLock(SCHEDULE_ID, SEAT_ID, MEMBER_ID)).thenReturn(lockValue);
		doThrow(new BusinessException(ScheduleSeatErrorCode.SEAT_ALREADY_HOLD)).when(spyService)
			.changeToHold(SCHEDULE_ID, SEAT_ID);

		// when & then
		assertThrows(BusinessException.class, () -> spyService.holdSeat(MEMBER_ID, SCHEDULE_ID, SEAT_ID));

		verify(seatHoldTokenService, never()).save(anyLong(), anyLong(), anyLong());
		verify(scheduleSeatLockService).unlock(SCHEDULE_ID, SEAT_ID, lockValue);
	}

	@Test
	@DisplayName("changeToHold(): 좌석이 없으면 SEAT_NOT_FOUND 예외")
	void changeToHold_notFound_throw() {
		// given
		when(scheduleSeatRepository.findByScheduleIdAndSeatId(SCHEDULE_ID, SEAT_ID)).thenReturn(Optional.empty());

		// when & then
		assertThrows(BusinessException.class, () -> scheduleSeatStateService.changeToHold(SCHEDULE_ID, SEAT_ID));
	}

	@Test
	@DisplayName("changeToHold(): SOLD면 SEAT_ALREADY_SOLD 예외")
	void changeToHold_sold_throw() {
		// given
		ScheduleSeat seat = org.mockito.Mockito.mock(ScheduleSeat.class);
		when(scheduleSeatRepository.findByScheduleIdAndSeatId(SCHEDULE_ID, SEAT_ID)).thenReturn(Optional.of(seat));
		when(seat.getStatus()).thenReturn(SeatStatus.SOLD);

		// when & then
		assertThrows(BusinessException.class, () -> scheduleSeatStateService.changeToHold(SCHEDULE_ID, SEAT_ID));
		verify(seat, never()).hold();
	}

	@Test
	@DisplayName("changeToHold(): 이미 HOLD면 SEAT_ALREADY_HOLD 예외")
	void changeToHold_alreadyHold_throw() {
		// given
		ScheduleSeat seat = org.mockito.Mockito.mock(ScheduleSeat.class);
		when(scheduleSeatRepository.findByScheduleIdAndSeatId(SCHEDULE_ID, SEAT_ID)).thenReturn(Optional.of(seat));
		when(seat.getStatus()).thenReturn(SeatStatus.HOLD);

		// when & then
		assertThrows(BusinessException.class, () -> scheduleSeatStateService.changeToHold(SCHEDULE_ID, SEAT_ID));
		verify(seat, never()).hold();
	}

	@Test
	@DisplayName("changeToHold(): AVAILABLE이면 hold() 호출")
	void changeToHold_available_holdCalled() {
		// given
		ScheduleSeat seat = org.mockito.Mockito.mock(ScheduleSeat.class);
		when(scheduleSeatRepository.findByScheduleIdAndSeatId(SCHEDULE_ID, SEAT_ID)).thenReturn(Optional.of(seat));
		when(seat.getStatus()).thenReturn(SeatStatus.AVAILABLE);

		// when
		assertThatNoException().isThrownBy(() -> scheduleSeatStateService.changeToHold(SCHEDULE_ID, SEAT_ID));

		// then
		verify(seat).hold();
	}

	@Test
	@DisplayName("changeToAvailable(): HOLD가 아니면 아무 것도 하지 않는다(예외 없음)")
	void changeToAvailable_notHold_noop() {
		// given
		ScheduleSeat seat = org.mockito.Mockito.mock(ScheduleSeat.class);
		when(scheduleSeatRepository.findByScheduleIdAndSeatId(SCHEDULE_ID, SEAT_ID)).thenReturn(Optional.of(seat));
		when(seat.getStatus()).thenReturn(SeatStatus.AVAILABLE);

		// when
		assertThatNoException().isThrownBy(() -> scheduleSeatStateService.changeToAvailable(SCHEDULE_ID, SEAT_ID));

		// then
		verify(seat, never()).release();
	}

	@Test
	@DisplayName("changeToAvailable(): HOLD면 release() 호출")
	void changeToAvailable_hold_releaseCalled() {
		// given
		ScheduleSeat seat = org.mockito.Mockito.mock(ScheduleSeat.class);
		when(scheduleSeatRepository.findByScheduleIdAndSeatId(SCHEDULE_ID, SEAT_ID)).thenReturn(Optional.of(seat));
		when(seat.getStatus()).thenReturn(SeatStatus.HOLD);

		// when
		assertThatNoException().isThrownBy(() -> scheduleSeatStateService.changeToAvailable(SCHEDULE_ID, SEAT_ID));

		// then
		verify(seat).release();
	}

	@Test
	@DisplayName("changeToSold(): HOLD가 아니면 SEAT_NOT_HOLD 예외")
	void changeToSold_notHold_throw() {
		// given
		ScheduleSeat seat = org.mockito.Mockito.mock(ScheduleSeat.class);
		when(scheduleSeatRepository.findByScheduleIdAndSeatId(SCHEDULE_ID, SEAT_ID)).thenReturn(Optional.of(seat));
		when(seat.getStatus()).thenReturn(SeatStatus.AVAILABLE);

		// when & then
		assertThrows(BusinessException.class, () -> scheduleSeatStateService.changeToSold(SCHEDULE_ID, SEAT_ID));
		verify(seat, never()).sold();
	}

	@Test
	@DisplayName("changeToSold(): HOLD면 sold() 호출")
	void changeToSold_hold_soldCalled() {
		// given
		ScheduleSeat seat = org.mockito.Mockito.mock(ScheduleSeat.class);
		when(scheduleSeatRepository.findByScheduleIdAndSeatId(SCHEDULE_ID, SEAT_ID)).thenReturn(Optional.of(seat));
		when(seat.getStatus()).thenReturn(SeatStatus.HOLD);

		// when
		assertThatNoException().isThrownBy(() -> scheduleSeatStateService.changeToSold(SCHEDULE_ID, SEAT_ID));

		// then
		verify(seat).sold();
	}
}
