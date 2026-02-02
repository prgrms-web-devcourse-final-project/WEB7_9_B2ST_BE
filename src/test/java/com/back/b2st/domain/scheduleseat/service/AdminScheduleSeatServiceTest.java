package com.back.b2st.domain.scheduleseat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.back.b2st.domain.scheduleseat.dto.response.ScheduleSeatViewRes;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AdminScheduleSeatServiceTest {

	@Mock
	private ScheduleSeatService scheduleSeatService;

	@Mock
	private ScheduleSeatStateService seatStateService;

	@InjectMocks
	private AdminScheduleSeatService adminScheduleSeatService;

	@Test
	void getSeats_returnsSeatList() {
		// given
		Long scheduleId = 1L;
		List<ScheduleSeatViewRes> expected = List.of(mock(ScheduleSeatViewRes.class));

		when(scheduleSeatService.getSeats(scheduleId)).thenReturn(expected);

		// when
		List<ScheduleSeatViewRes> result = adminScheduleSeatService.getSeats(scheduleId);

		// then
		assertThat(result).isSameAs(expected);
		verify(scheduleSeatService, times(1)).getSeats(scheduleId);
		verifyNoInteractions(seatStateService);
	}

	@Test
	void getSeatsByStatus_returnsFilteredSeatList() {
		// given
		Long scheduleId = 1L;
		SeatStatus status = SeatStatus.HOLD;
		List<ScheduleSeatViewRes> expected = List.of(mock(ScheduleSeatViewRes.class), mock(ScheduleSeatViewRes.class));

		when(scheduleSeatService.getSeatsByStatus(scheduleId, status)).thenReturn(expected);

		// when
		List<ScheduleSeatViewRes> result = adminScheduleSeatService.getSeatsByStatus(scheduleId, status);

		// then
		assertThat(result).isSameAs(expected);
		verify(scheduleSeatService, times(1)).getSeatsByStatus(scheduleId, status);
		verifyNoInteractions(seatStateService);
	}

	@Test
	void releaseHold_delegatesToSeatStateService() {
		// given
		Long scheduleId = 1L;
		Long seatId = 10L;

		// when
		adminScheduleSeatService.releaseHold(scheduleId, seatId);

		// then
		verify(seatStateService, times(1)).releaseHold(scheduleId, seatId);
		verifyNoInteractions(scheduleSeatService);
	}
}
