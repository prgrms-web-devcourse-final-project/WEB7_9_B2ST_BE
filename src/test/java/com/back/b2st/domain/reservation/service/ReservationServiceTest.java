package com.back.b2st.domain.reservation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.reservation.dto.request.ReservationReq;
import com.back.b2st.domain.reservation.dto.response.ReservationCreateRes;
import com.back.b2st.domain.reservation.dto.response.ReservationDetailRes;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationStatus;
import com.back.b2st.domain.reservation.error.ReservationErrorCode;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.scheduleseat.service.ScheduleSeatStateService;
import com.back.b2st.domain.scheduleseat.service.SeatHoldTokenService;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private SeatHoldTokenService seatHoldTokenService;

	@Mock
	private ScheduleSeatStateService scheduleSeatStateService;

	@InjectMocks
	private ReservationService reservationService;

	private static final Long MEMBER_ID = 1L;
	private static final Long OTHER_MEMBER_ID = 999L;
	private static final Long RESERVATION_ID = 10L;
	private static final Long SCHEDULE_ID = 100L;
	private static final Long SEAT_ID = 200L;

	@Test
	@DisplayName("createReservation(): HOLD 검증 후 Reservation 저장 및 DTO 반환")
	void createReservation_success() {
		// given
		ReservationReq request = mock(ReservationReq.class);
		LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

		when(request.scheduleId()).thenReturn(SCHEDULE_ID);
		when(request.seatId()).thenReturn(SEAT_ID);

		when(reservationRepository.existsByScheduleIdAndSeatIdAndStatusIn(
			eq(SCHEDULE_ID),
			eq(SEAT_ID),
			anyList()
		)).thenReturn(false);

		when(scheduleSeatStateService.getHoldExpiredAtOrThrow(SCHEDULE_ID, SEAT_ID))
			.thenReturn(expiresAt);

		Reservation reservation = Reservation.builder()
			.memberId(MEMBER_ID)
			.scheduleId(SCHEDULE_ID)
			.seatId(SEAT_ID)
			.expiresAt(expiresAt)
			.build();

		when(request.toEntity(MEMBER_ID, expiresAt)).thenReturn(reservation);
		when(reservationRepository.save(reservation)).thenReturn(reservation);

		// when
		ReservationCreateRes result =
			reservationService.createReservation(MEMBER_ID, request);

		// then
		assertThat(result).isNotNull();

		verify(seatHoldTokenService).validateOwnership(SCHEDULE_ID, SEAT_ID, MEMBER_ID);
		verify(scheduleSeatStateService).validateHoldState(SCHEDULE_ID, SEAT_ID);
		verify(scheduleSeatStateService).getHoldExpiredAtOrThrow(SCHEDULE_ID, SEAT_ID);
		verify(reservationRepository).save(reservation);
	}

	@Test
	@DisplayName("createReservation(): 활성 상태 중복 예매면 예외")
	void createReservation_duplicate_throw() {
		// given
		ReservationReq request = mock(ReservationReq.class);

		when(request.scheduleId()).thenReturn(SCHEDULE_ID);
		when(request.seatId()).thenReturn(SEAT_ID);

		when(reservationRepository.existsByScheduleIdAndSeatIdAndStatusIn(
			eq(SCHEDULE_ID),
			eq(SEAT_ID),
			anyList()
		)).thenReturn(true);

		// when & then
		assertThatThrownBy(() -> reservationService.createReservation(MEMBER_ID, request))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ReservationErrorCode.RESERVATION_ALREADY_EXISTS);
	}

	@Test
	@DisplayName("cancelReservation(): 정상 취소 시 좌석 복구 및 토큰 제거")
	void cancelReservation_success() {
		// given
		Reservation reservation = mock(Reservation.class);
		ReservationStatus status = mock(ReservationStatus.class);

		when(reservationRepository.findById(RESERVATION_ID))
			.thenReturn(Optional.of(reservation));
		when(reservation.getMemberId()).thenReturn(MEMBER_ID);
		when(reservation.getStatus()).thenReturn(status);
		when(status.canCancel()).thenReturn(true);
		when(reservation.getScheduleId()).thenReturn(SCHEDULE_ID);
		when(reservation.getSeatId()).thenReturn(SEAT_ID);

		// when
		reservationService.cancelReservation(RESERVATION_ID, MEMBER_ID);

		// then
		ArgumentCaptor<LocalDateTime> timeCaptor =
			ArgumentCaptor.forClass(LocalDateTime.class);

		InOrder inOrder = inOrder(reservation, scheduleSeatStateService, seatHoldTokenService);
		inOrder.verify(reservation).cancel(timeCaptor.capture());
		inOrder.verify(scheduleSeatStateService).changeToAvailable(SCHEDULE_ID, SEAT_ID);
		inOrder.verify(seatHoldTokenService).remove(SCHEDULE_ID, SEAT_ID);

		assertThat(timeCaptor.getValue()).isNotNull();
	}

	@Test
	@DisplayName("completeReservation(): 정상 완료 시 SOLD 전환 및 토큰 제거")
	void completeReservation_success() {
		// given
		Reservation reservation = mock(Reservation.class);
		ReservationStatus status = mock(ReservationStatus.class);

		when(reservationRepository.findById(RESERVATION_ID))
			.thenReturn(Optional.of(reservation));
		when(reservation.getStatus()).thenReturn(status);
		when(status.canComplete()).thenReturn(true);
		when(reservation.getScheduleId()).thenReturn(SCHEDULE_ID);
		when(reservation.getSeatId()).thenReturn(SEAT_ID);

		// when
		reservationService.completeReservation(RESERVATION_ID);

		// then
		InOrder inOrder = inOrder(reservation, scheduleSeatStateService, seatHoldTokenService);
		inOrder.verify(reservation).complete(any(LocalDateTime.class));
		inOrder.verify(scheduleSeatStateService).changeToSold(SCHEDULE_ID, SEAT_ID);
		inOrder.verify(seatHoldTokenService).remove(SCHEDULE_ID, SEAT_ID);
	}

	@Test
	@DisplayName("expireReservation(): 만료 가능 시 AVAILABLE 복구")
	void expireReservation_success() {
		// given
		Reservation reservation = mock(Reservation.class);
		ReservationStatus status = mock(ReservationStatus.class);

		when(reservationRepository.findById(RESERVATION_ID))
			.thenReturn(Optional.of(reservation));
		when(reservation.getStatus()).thenReturn(status);
		when(status.canExpire()).thenReturn(true);
		when(reservation.getScheduleId()).thenReturn(SCHEDULE_ID);
		when(reservation.getSeatId()).thenReturn(SEAT_ID);

		// when
		reservationService.expireReservation(RESERVATION_ID);

		// then
		InOrder inOrder = inOrder(reservation, scheduleSeatStateService, seatHoldTokenService);
		inOrder.verify(reservation).expire();
		inOrder.verify(scheduleSeatStateService).changeToAvailable(SCHEDULE_ID, SEAT_ID);
		inOrder.verify(seatHoldTokenService).remove(SCHEDULE_ID, SEAT_ID);
	}

	@Test
	@DisplayName("getReservationDetail(): 없으면 NOT_FOUND 예외")
	void getReservationDetail_notFound() {
		when(reservationRepository.findReservationDetail(RESERVATION_ID, MEMBER_ID))
			.thenReturn(null);

		assertThatThrownBy(() ->
			reservationService.getReservationDetail(RESERVATION_ID, MEMBER_ID)
		).isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("getReservationDetail(): 있으면 그대로 반환")
	void getReservationDetail_success() {
		ReservationDetailRes detail = mock(ReservationDetailRes.class);
		when(reservationRepository.findReservationDetail(RESERVATION_ID, MEMBER_ID))
			.thenReturn(detail);

		ReservationDetailRes result =
			reservationService.getReservationDetail(RESERVATION_ID, MEMBER_ID);

		assertThat(result).isSameAs(detail);
	}
}
