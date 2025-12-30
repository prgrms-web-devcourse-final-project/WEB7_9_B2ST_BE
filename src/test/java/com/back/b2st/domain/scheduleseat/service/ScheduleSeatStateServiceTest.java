package com.back.b2st.domain.scheduleseat.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.prereservation.service.PrereservationService;
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
	private PrereservationService prereservationService;

	@Mock
	private ScheduleSeatRepository scheduleSeatRepository;

	@InjectMocks
	private ScheduleSeatStateService scheduleSeatStateService;

	private static final Long MEMBER_ID = 1L;
	private static final Long SCHEDULE_ID = 10L;
	private static final Long SEAT_ID = 100L;

	@Test
	@DisplayName("holdSeat(): prereservation 검증 실패 시 락을 시도하지 않는다")
	void holdSeat_prereservationFailed_noLock() {
		doThrow(new BusinessException(ScheduleSeatErrorCode.SEAT_NOT_FOUND))
			.when(prereservationService)
			.validateSeatHoldAllowed(MEMBER_ID, SCHEDULE_ID, SEAT_ID);

		assertThrows(
			BusinessException.class,
			() -> scheduleSeatStateService.holdSeat(MEMBER_ID, SCHEDULE_ID, SEAT_ID)
		);

		verify(scheduleSeatLockService, never()).tryLock(anyLong(), anyLong(), anyLong());
		verify(scheduleSeatRepository, never())
			.findByScheduleIdAndSeatIdWithLock(anyLong(), anyLong());
		verify(seatHoldTokenService, never()).save(anyLong(), anyLong(), anyLong());
		verify(scheduleSeatLockService, never()).unlock(anyLong(), anyLong(), anyString());
	}

	@Test
	@DisplayName("holdSeat(): 락 획득 실패 시 SEAT_LOCK_FAILED 예외")
	void holdSeat_lockFailed_throw() {
		when(scheduleSeatLockService.tryLock(SCHEDULE_ID, SEAT_ID, MEMBER_ID))
			.thenReturn(null);

		assertThrows(
			BusinessException.class,
			() -> scheduleSeatStateService.holdSeat(MEMBER_ID, SCHEDULE_ID, SEAT_ID)
		);

		verify(scheduleSeatRepository, never())
			.findByScheduleIdAndSeatIdWithLock(anyLong(), anyLong());
		verify(seatHoldTokenService, never()).save(anyLong(), anyLong(), anyLong());
		verify(scheduleSeatLockService, never()).unlock(anyLong(), anyLong(), anyString());
	}

	@Test
	@DisplayName("holdSeat(): 성공 시 validate → lock → hold → token → unlock 순서")
	void holdSeat_success_flowOrder() {
		String lockValue = "lock-value";
		ScheduleSeat seat = mock(ScheduleSeat.class);

		when(scheduleSeatLockService.tryLock(SCHEDULE_ID, SEAT_ID, MEMBER_ID))
			.thenReturn(lockValue);
		when(scheduleSeatRepository.findByScheduleIdAndSeatIdWithLock(SCHEDULE_ID, SEAT_ID))
			.thenReturn(Optional.of(seat));
		when(seat.getStatus()).thenReturn(SeatStatus.AVAILABLE);

		assertThatNoException()
			.isThrownBy(() -> scheduleSeatStateService.holdSeat(MEMBER_ID, SCHEDULE_ID, SEAT_ID));

		InOrder inOrder = inOrder(
			prereservationService,
			scheduleSeatLockService,
			seat,
			seatHoldTokenService
		);

		inOrder.verify(prereservationService)
			.validateSeatHoldAllowed(MEMBER_ID, SCHEDULE_ID, SEAT_ID);
		inOrder.verify(scheduleSeatLockService)
			.tryLock(SCHEDULE_ID, SEAT_ID, MEMBER_ID);
		inOrder.verify(seat)
			.hold(any(LocalDateTime.class));
		inOrder.verify(seatHoldTokenService)
			.save(SCHEDULE_ID, SEAT_ID, MEMBER_ID);
		inOrder.verify(scheduleSeatLockService)
			.unlock(SCHEDULE_ID, SEAT_ID, lockValue);
	}

	@Test
	@DisplayName("holdSeat(): changeToHold에서 예외 발생해도 unlock은 반드시 수행된다")
	void holdSeat_changeToHoldThrows_unlockAlways() {
		String lockValue = "lock-value";

		ScheduleSeatStateService spyService =
			spy(new ScheduleSeatStateService(
				scheduleSeatLockService,
				seatHoldTokenService,
				prereservationService,
				scheduleSeatRepository
			));

		when(scheduleSeatLockService.tryLock(SCHEDULE_ID, SEAT_ID, MEMBER_ID))
			.thenReturn(lockValue);

		doThrow(new BusinessException(ScheduleSeatErrorCode.SEAT_ALREADY_HOLD))
			.when(spyService)
			.changeToHold(SCHEDULE_ID, SEAT_ID);

		assertThrows(
			BusinessException.class,
			() -> spyService.holdSeat(MEMBER_ID, SCHEDULE_ID, SEAT_ID)
		);

		verify(seatHoldTokenService, never()).save(anyLong(), anyLong(), anyLong());
		verify(scheduleSeatLockService)
			.unlock(SCHEDULE_ID, SEAT_ID, lockValue);
	}
}
