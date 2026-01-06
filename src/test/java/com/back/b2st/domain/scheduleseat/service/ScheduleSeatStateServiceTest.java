package com.back.b2st.domain.scheduleseat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.queue.service.QueueAccessService;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.error.ScheduleSeatErrorCode;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ScheduleSeatStateServiceTest {

	@Mock
	private ScheduleSeatLockService scheduleSeatLockService;

	@Mock
	private SeatHoldTokenService seatHoldTokenService;

	@Mock
	private ScheduleSeatRepository scheduleSeatRepository;

	@Mock
	private PerformanceScheduleRepository performanceScheduleRepository;

	@Mock
	private QueueAccessService queueAccessService;

	@InjectMocks
	private ScheduleSeatStateService scheduleSeatStateService;

	private static final Long MEMBER_ID = 1L;
	private static final Long SCHEDULE_ID = 10L;
	private static final Long SEAT_ID = 100L;
	private static final Long PERFORMANCE_ID = 99L;

	@Test
	@DisplayName("holdSeat(): 락 획득 실패 시 SEAT_LOCK_FAILED")
	void holdSeat_lockFailed_throw() {
		// given
		when(performanceScheduleRepository.findPerformanceIdByScheduleId(SCHEDULE_ID))
			.thenReturn(Optional.of(PERFORMANCE_ID));
		doNothing().when(queueAccessService).assertEnterable(PERFORMANCE_ID, MEMBER_ID);

		when(scheduleSeatLockService.tryLock(SCHEDULE_ID, SEAT_ID, MEMBER_ID))
			.thenReturn(null);

		// when & then
		assertThatThrownBy(() ->
			scheduleSeatStateService.holdSeat(MEMBER_ID, SCHEDULE_ID, SEAT_ID)
		)
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ScheduleSeatErrorCode.SEAT_LOCK_FAILED);

		verify(scheduleSeatRepository, never())
			.findByScheduleIdAndSeatIdWithLock(anyLong(), anyLong());
		verify(seatHoldTokenService, never()).save(anyLong(), anyLong(), anyLong());

		verify(queueAccessService).assertEnterable(PERFORMANCE_ID, MEMBER_ID);
	}

	@Test
	@DisplayName("holdSeat(): 성공 시 queueCheck → lock → hold → token → unlock 순서")
	void holdSeat_success_flow() {
		// given
		String lockValue = "lock-value";
		ScheduleSeat seat = mock(ScheduleSeat.class);

		when(performanceScheduleRepository.findPerformanceIdByScheduleId(SCHEDULE_ID))
			.thenReturn(Optional.of(PERFORMANCE_ID));
		doNothing().when(queueAccessService).assertEnterable(PERFORMANCE_ID, MEMBER_ID);

		when(scheduleSeatLockService.tryLock(SCHEDULE_ID, SEAT_ID, MEMBER_ID))
			.thenReturn(lockValue);
		when(scheduleSeatRepository.findByScheduleIdAndSeatIdWithLock(SCHEDULE_ID, SEAT_ID))
			.thenReturn(Optional.of(seat));
		when(seat.getStatus()).thenReturn(SeatStatus.AVAILABLE);

		// when & then
		assertThatNoException()
			.isThrownBy(() -> scheduleSeatStateService.holdSeat(MEMBER_ID, SCHEDULE_ID, SEAT_ID));

		InOrder inOrder = inOrder(
			performanceScheduleRepository,
			queueAccessService,
			scheduleSeatLockService,
			seat,
			seatHoldTokenService
		);

		inOrder.verify(performanceScheduleRepository)
			.findPerformanceIdByScheduleId(SCHEDULE_ID);
		inOrder.verify(queueAccessService)
			.assertEnterable(PERFORMANCE_ID, MEMBER_ID);
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
	@DisplayName("holdSeat(): HOLD 상태면 SEAT_ALREADY_HOLD")
	void holdSeat_alreadyHold_throw() {
		// given
		String lockValue = "lock-value";
		ScheduleSeat seat = mock(ScheduleSeat.class);

		when(performanceScheduleRepository.findPerformanceIdByScheduleId(SCHEDULE_ID))
			.thenReturn(Optional.of(PERFORMANCE_ID));
		doNothing().when(queueAccessService).assertEnterable(PERFORMANCE_ID, MEMBER_ID);

		when(scheduleSeatLockService.tryLock(SCHEDULE_ID, SEAT_ID, MEMBER_ID))
			.thenReturn(lockValue);
		when(scheduleSeatRepository.findByScheduleIdAndSeatIdWithLock(SCHEDULE_ID, SEAT_ID))
			.thenReturn(Optional.of(seat));
		when(seat.getStatus()).thenReturn(SeatStatus.HOLD);

		// when & then
		assertThatThrownBy(() ->
			scheduleSeatStateService.holdSeat(MEMBER_ID, SCHEDULE_ID, SEAT_ID)
		)
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ScheduleSeatErrorCode.SEAT_ALREADY_HOLD);

		verify(scheduleSeatLockService)
			.unlock(eq(SCHEDULE_ID), eq(SEAT_ID), anyString());
		verify(seatHoldTokenService, never()).save(anyLong(), anyLong(), anyLong());
		verify(queueAccessService).assertEnterable(PERFORMANCE_ID, MEMBER_ID);
	}

	@Test
	@DisplayName("releaseHold(): HOLD → AVAILABLE + token 제거")
	void releaseHold_success() {
		// given
		ScheduleSeat seat = mock(ScheduleSeat.class);

		when(scheduleSeatRepository.findByScheduleIdAndSeatIdWithLock(SCHEDULE_ID, SEAT_ID))
			.thenReturn(Optional.of(seat));
		when(seat.getStatus()).thenReturn(SeatStatus.HOLD);

		// when
		scheduleSeatStateService.releaseHold(SCHEDULE_ID, SEAT_ID);

		// then
		verify(seat).release();
		verify(seatHoldTokenService).remove(SCHEDULE_ID, SEAT_ID);
	}

	@Test
	@DisplayName("confirmHold(): HOLD → SOLD + token 제거")
	void confirmHold_success() {
		// given
		ScheduleSeat seat = mock(ScheduleSeat.class);

		when(scheduleSeatRepository.findByScheduleIdAndSeatIdWithLock(SCHEDULE_ID, SEAT_ID))
			.thenReturn(Optional.of(seat));
		when(seat.getStatus()).thenReturn(SeatStatus.HOLD);

		// when
		scheduleSeatStateService.confirmHold(SCHEDULE_ID, SEAT_ID);

		// then
		verify(seat).sold();
		verify(seatHoldTokenService).remove(SCHEDULE_ID, SEAT_ID);
	}

	@Test
	@DisplayName("releaseExpiredHoldsBatch(): 만료된 HOLD 좌석을 복구하고 token 제거")
	void releaseExpiredHoldsBatch_success() {
		// given
		Object[] row = new Object[] {SCHEDULE_ID, SEAT_ID};

		when(scheduleSeatRepository.findExpiredHoldKeys(eq(SeatStatus.HOLD), any(LocalDateTime.class)))
			.thenReturn(Collections.singletonList(row));

		when(scheduleSeatRepository.releaseExpiredHolds(
			eq(SeatStatus.HOLD),
			eq(SeatStatus.AVAILABLE),
			any(LocalDateTime.class)
		)).thenReturn(1);

		// when
		int updated = scheduleSeatStateService.releaseExpiredHoldsBatch();

		// then
		assertThat(updated).isEqualTo(1);
		verify(seatHoldTokenService).remove(SCHEDULE_ID, SEAT_ID);
	}
}
