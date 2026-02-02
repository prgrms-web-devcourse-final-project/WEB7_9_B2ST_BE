package com.back.b2st.domain.reservation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.reservation.dto.request.ReservationReq;
import com.back.b2st.domain.reservation.dto.response.ReservationCreateRes;
import com.back.b2st.domain.reservation.dto.response.SeatReservationResult;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationStatus;
import com.back.b2st.domain.reservation.error.ReservationErrorCode;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.ticket.service.TicketService;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private ReservationSeatManager reservationSeatManager;

	@Mock
	private TicketService ticketService;

	@InjectMocks
	private ReservationService reservationService;

	private static final Long MEMBER_ID = 1L;
	private static final Long OTHER_MEMBER_ID = 999L;
	private static final Long RESERVATION_ID = 10L;
	private static final Long SCHEDULE_ID = 100L;
	private static final Long SEAT_ID = 200L;
	private static final Long SCHEDULE_SEAT_ID = 300L;

	@Test
	@DisplayName("createReservation(): 정상 생성 시 Reservation 저장 + 좌석 귀속")
	void createReservation_success() {
		// given
		ReservationReq request = mock(ReservationReq.class);
		LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

		when(request.scheduleId()).thenReturn(SCHEDULE_ID);
		when(request.seatIds()).thenReturn(List.of(SEAT_ID));

		when(reservationSeatManager.prepareSeatReservation(
			SCHEDULE_ID,
			List.of(SEAT_ID),
			MEMBER_ID
		)).thenReturn(
			new SeatReservationResult(List.of(SCHEDULE_SEAT_ID), expiresAt)
		);

		when(reservationRepository.existsCompletedByScheduleSeat(SCHEDULE_ID, SCHEDULE_SEAT_ID))
			.thenReturn(false);
		when(reservationRepository.existsActivePendingByScheduleSeat(
			eq(SCHEDULE_ID),
			eq(SCHEDULE_SEAT_ID),
			any(LocalDateTime.class)
		)).thenReturn(false);

		Reservation reservation = Reservation.builder()
			.memberId(MEMBER_ID)
			.scheduleId(SCHEDULE_ID)
			.expiresAt(expiresAt)
			.build();

		when(request.toEntity(MEMBER_ID, expiresAt)).thenReturn(reservation);
		when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

		// when
		ReservationCreateRes result =
			reservationService.createReservation(MEMBER_ID, request);

		// then
		assertThat(result).isNotNull();

		verify(reservationRepository).save(reservation);

		verify(reservationSeatManager).attachSeats(
			any(), // Long (null 허용)
			eq(List.of(SCHEDULE_SEAT_ID))
		);
	}

	@Test
	@DisplayName("createReservation(): 좌석이 1개가 아니면 INVALID_SEAT_COUNT")
	void createReservation_invalidSeatCount_throw() {
		// given
		ReservationReq request = mock(ReservationReq.class);
		when(request.seatIds()).thenReturn(List.of(1L, 2L));

		// when & then
		assertThatThrownBy(() ->
			reservationService.createReservation(MEMBER_ID, request)
		)
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ReservationErrorCode.INVALID_SEAT_COUNT);

		verifyNoInteractions(reservationRepository);
		verifyNoInteractions(reservationSeatManager);
	}

	@Test
	@DisplayName("failReservation(): PENDING → FAILED + 좌석 해제")
	void failReservation_success() {
		// given
		Reservation reservation = mock(Reservation.class);

		when(reservationRepository.findByIdWithLock(RESERVATION_ID))
			.thenReturn(Optional.of(reservation));
		when(reservation.getStatus()).thenReturn(ReservationStatus.PENDING);

		// when
		reservationService.failReservation(RESERVATION_ID);

		// then
		verify(reservation).fail();
		verify(reservationSeatManager).releaseAllSeats(RESERVATION_ID);
	}

	@Test
	@DisplayName("cancelReservation(): 정상 취소 시 티켓 취소 + 예매 상태 변경 + 좌석 강제 해제")
	void cancelReservation_success() {
		// given
		Reservation reservation = mock(Reservation.class);

		when(reservationRepository.findByIdWithLock(RESERVATION_ID))
			.thenReturn(Optional.of(reservation));
		when(reservation.getMemberId()).thenReturn(MEMBER_ID);
		when(reservation.getStatus()).thenReturn(ReservationStatus.PENDING); // canCancel() = true

		// when
		reservationService.cancelReservation(RESERVATION_ID, MEMBER_ID);

		// then
		ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);

		// 1) 티켓 취소 호출
		verify(ticketService).cancelTicketsByReservation(RESERVATION_ID, MEMBER_ID);

		// 2) 예매 취소(시간 인자 포함)
		verify(reservation).cancel(captor.capture());

		// 3) 좌석 강제 해제 (서비스 코드와 메서드명 일치)
		verify(reservationSeatManager).releaseForceAllSeats(RESERVATION_ID);
	}

	@Test
	@DisplayName("cancelReservation(): 본인 예매 아니면 FORBIDDEN")
	void cancelReservation_forbidden_throw() {
		// given
		Reservation reservation = mock(Reservation.class);

		when(reservationRepository.findByIdWithLock(RESERVATION_ID))
			.thenReturn(Optional.of(reservation));
		when(reservation.getMemberId()).thenReturn(OTHER_MEMBER_ID);

		// when & then
		assertThatThrownBy(() ->
			reservationService.cancelReservation(RESERVATION_ID, MEMBER_ID)
		)
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ReservationErrorCode.RESERVATION_FORBIDDEN);

		verify(reservationSeatManager, never()).releaseAllSeats(anyLong());
	}

	@Test
	@DisplayName("expireReservation(): 만료 조건 충족 시 EXPIRED + 좌석 해제")
	void expireReservation_success() {
		// given
		Reservation reservation = mock(Reservation.class);

		when(reservationRepository.findByIdWithLock(RESERVATION_ID))
			.thenReturn(Optional.of(reservation));
		when(reservation.getStatus()).thenReturn(ReservationStatus.PENDING);
		when(reservation.getExpiresAt())
			.thenReturn(LocalDateTime.now().minusSeconds(1));

		// when
		reservationService.expireReservation(RESERVATION_ID);

		// then
		verify(reservation).expire();
		verify(reservationSeatManager).releaseAllSeats(RESERVATION_ID);
	}

	@Test
	@DisplayName("expireReservation(): 아직 만료되지 않으면 아무 작업도 하지 않는다")
	void expireReservation_notExpired_noop() {
		// given
		Reservation reservation = mock(Reservation.class);

		when(reservationRepository.findByIdWithLock(RESERVATION_ID))
			.thenReturn(Optional.of(reservation));
		when(reservation.getStatus()).thenReturn(ReservationStatus.PENDING);
		when(reservation.getExpiresAt())
			.thenReturn(LocalDateTime.now().plusMinutes(1));

		// when
		reservationService.expireReservation(RESERVATION_ID);

		// then
		verify(reservation, never()).expire();
		verify(reservationSeatManager, never()).releaseAllSeats(anyLong());
	}

	@Test
	@DisplayName("completeReservation(): 예매 완료 시 좌석 확정 + 티켓 생성 보장")
	void completeReservation_success() {
		// given
		Reservation reservation = mock(Reservation.class);

		when(reservationRepository.findByIdWithLock(RESERVATION_ID))
			.thenReturn(Optional.of(reservation));
		when(reservation.getStatus()).thenReturn(ReservationStatus.PENDING);

		// when
		reservationService.completeReservation(RESERVATION_ID);

		// then
		verify(reservation).complete(any(LocalDateTime.class));
		verify(reservationSeatManager).confirmAllSeats(RESERVATION_ID);
		verify(ticketService).ensureTicketsForReservation(RESERVATION_ID);
	}

	@Test
	@DisplayName("completeReservation(): 이미 완료된 예매면 티켓만 보정하고 종료")
	void completeReservation_alreadyCompleted_ensuresTicketsOnly() {
		// given
		Reservation reservation = mock(Reservation.class);

		when(reservationRepository.findByIdWithLock(RESERVATION_ID))
			.thenReturn(Optional.of(reservation));
		when(reservation.getStatus()).thenReturn(ReservationStatus.COMPLETED);

		// when
		reservationService.completeReservation(RESERVATION_ID);

		// then
		verify(ticketService).ensureTicketsForReservation(RESERVATION_ID);
		verify(reservationSeatManager, never()).confirmAllSeats(anyLong());
		verify(reservation, never()).complete(any(LocalDateTime.class));
	}
}
