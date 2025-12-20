package com.back.b2st.domain.reservation.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import com.back.b2st.domain.reservation.dto.response.ReservationDetailRes;
import com.back.b2st.domain.reservation.dto.response.ReservationRes;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationStatus;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.scheduleseat.service.ScheduleSeatStateService;
import com.back.b2st.domain.scheduleseat.service.SeatHoldTokenService;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private ScheduleSeatRepository scheduleSeatRepository;

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
	@DisplayName("createReservation(): HOLD 소유권 검증 후 Reservation 저장하고 상세를 반환한다")
	void createReservation_success() {
		// given
		ReservationReq request = mock(ReservationReq.class);
		Reservation reservation = mock(Reservation.class);
		Reservation saved = mock(Reservation.class);
		ReservationDetailRes detail = mock(ReservationDetailRes.class);

		when(request.scheduleId()).thenReturn(SCHEDULE_ID);
		when(request.seatId()).thenReturn(SEAT_ID);

		when(request.toEntity(MEMBER_ID)).thenReturn(reservation);
		when(reservationRepository.save(reservation)).thenReturn(saved);
		when(saved.getId()).thenReturn(RESERVATION_ID);

		when(reservationRepository.findReservationDetail(RESERVATION_ID, MEMBER_ID)).thenReturn(detail);

		// when
		ReservationDetailRes result = reservationService.createReservation(MEMBER_ID, request);

		// then
		assertThat(result).isSameAs(detail);
		verify(seatHoldTokenService).validateOwnership(SCHEDULE_ID, SEAT_ID, MEMBER_ID);
		verify(reservationRepository).save(reservation);
		verify(reservationRepository).findReservationDetail(RESERVATION_ID, MEMBER_ID);
	}

	@Test
	@DisplayName("createReservation(): 상세 조회 결과가 null이면 RESERVATION_NOT_FOUND 예외")
	void createReservation_detailNull_throw() {
		// given
		ReservationReq request = mock(ReservationReq.class);
		Reservation reservation = mock(Reservation.class);
		Reservation saved = mock(Reservation.class);

		when(request.scheduleId()).thenReturn(SCHEDULE_ID);
		when(request.seatId()).thenReturn(SEAT_ID);

		when(request.toEntity(MEMBER_ID)).thenReturn(reservation);
		when(reservationRepository.save(reservation)).thenReturn(saved);
		when(saved.getId()).thenReturn(RESERVATION_ID);

		when(reservationRepository.findReservationDetail(RESERVATION_ID, MEMBER_ID)).thenReturn(null);

		// when & then
		assertThrows(BusinessException.class, () -> reservationService.createReservation(MEMBER_ID, request));
		verify(seatHoldTokenService).validateOwnership(SCHEDULE_ID, SEAT_ID, MEMBER_ID);
	}

	@Test
	@DisplayName("cancelReservation(): 내 예매가 아니면 RESERVATION_FORBIDDEN 예외")
	void cancelReservation_forbidden_throw() {
		// given
		Reservation reservation = mock(Reservation.class);
		when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
		when(reservation.getMemberId()).thenReturn(OTHER_MEMBER_ID);

		// when & then
		assertThrows(BusinessException.class, () -> reservationService.cancelReservation(RESERVATION_ID, MEMBER_ID));

		verify(scheduleSeatStateService, never()).changeToAvailable(any(), any());
	}

	@Test
	@DisplayName("cancelReservation(): 상태가 cancel 불가면 INVALID_RESERVATION_STATUS 예외")
	void cancelReservation_invalidStatus_throw() {
		// given
		Reservation reservation = mock(Reservation.class);
		ReservationStatus status = mock(ReservationStatus.class);

		when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
		when(reservation.getMemberId()).thenReturn(MEMBER_ID);
		when(reservation.getStatus()).thenReturn(status);
		when(status.canCancel()).thenReturn(false);

		// when & then
		assertThrows(BusinessException.class, () -> reservationService.cancelReservation(RESERVATION_ID, MEMBER_ID));

		verify(reservation, never()).cancel(any());
		verify(scheduleSeatStateService, never()).changeToAvailable(any(), any());
	}

	@Test
	@DisplayName("cancelReservation(): 정상 취소되면 reservation.cancel() 호출 후 좌석을 HOLD→AVAILABLE로 변경한다")
	void cancelReservation_success() {
		// given
		Reservation reservation = mock(Reservation.class);
		ReservationStatus status = mock(ReservationStatus.class);

		when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
		when(reservation.getMemberId()).thenReturn(MEMBER_ID);
		when(reservation.getStatus()).thenReturn(status);
		when(status.canCancel()).thenReturn(true);

		when(reservation.getScheduleId()).thenReturn(SCHEDULE_ID);
		when(reservation.getSeatId()).thenReturn(SEAT_ID);

		// when
		assertThatNoException().isThrownBy(() -> reservationService.cancelReservation(RESERVATION_ID, MEMBER_ID));

		// then
		ArgumentCaptor<java.time.LocalDateTime> timeCaptor = ArgumentCaptor.forClass(java.time.LocalDateTime.class);
		verify(reservation).cancel(timeCaptor.capture());
		verify(scheduleSeatStateService).changeToAvailable(SCHEDULE_ID, SEAT_ID);

		assertThat(timeCaptor.getValue()).isNotNull();
	}

	@Test
	@DisplayName("completeReservation(): 이미 COMPLETED면 아무것도 하지 않고 종료")
	void completeReservation_alreadyCompleted_return() {
		// given
		Reservation reservation = mock(Reservation.class);

		when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
		when(reservation.getStatus()).thenReturn(ReservationStatus.COMPLETED);

		// when
		assertThatNoException().isThrownBy(() -> reservationService.completeReservation(RESERVATION_ID));

		// then
		verify(reservation, never()).complete(any());
		verify(scheduleSeatStateService, never()).changeToSold(any(), any());
	}

	@Test
	@DisplayName("completeReservation(): 완료 불가 상태면 INVALID_RESERVATION_STATUS 예외")
	void completeReservation_invalidStatus_throw() {
		// given
		Reservation reservation = mock(Reservation.class);
		ReservationStatus status = mock(ReservationStatus.class);

		when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
		when(reservation.getStatus()).thenReturn(status);
		when(status.canComplete()).thenReturn(false);

		// when & then
		assertThrows(BusinessException.class, () -> reservationService.completeReservation(RESERVATION_ID));

		verify(reservation, never()).complete(any());
		verify(scheduleSeatStateService, never()).changeToSold(any(), any());
	}

	@Test
	@DisplayName("completeReservation(): 정상 완료되면 reservation.complete() 호출 후 좌석을 HOLD→SOLD로 변경한다")
	void completeReservation_success() {
		// given
		Reservation reservation = mock(Reservation.class);
		ReservationStatus status = mock(ReservationStatus.class);

		when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
		when(reservation.getStatus()).thenReturn(status);
		when(status.canComplete()).thenReturn(true);

		when(reservation.getScheduleId()).thenReturn(SCHEDULE_ID);
		when(reservation.getSeatId()).thenReturn(SEAT_ID);

		// when
		assertThatNoException().isThrownBy(() -> reservationService.completeReservation(RESERVATION_ID));

		// then
		ArgumentCaptor<java.time.LocalDateTime> timeCaptor = ArgumentCaptor.forClass(java.time.LocalDateTime.class);
		verify(reservation).complete(timeCaptor.capture());
		verify(scheduleSeatStateService).changeToSold(SCHEDULE_ID, SEAT_ID);

		assertThat(timeCaptor.getValue()).isNotNull();
	}

	@Test
	@DisplayName("expireReservation(): canExpire()가 false면 아무것도 하지 않고 종료")
	void expireReservation_cannotExpire_return() {
		// given
		Reservation reservation = mock(Reservation.class);
		ReservationStatus status = mock(ReservationStatus.class);

		when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
		when(reservation.getStatus()).thenReturn(status);
		when(status.canExpire()).thenReturn(false);

		// when
		assertThatNoException().isThrownBy(() -> reservationService.expireReservation(RESERVATION_ID));

		// then
		verify(reservation, never()).expire();
		verify(scheduleSeatRepository, never()).findByScheduleIdAndSeatId(any(), any());
	}

	@Test
	@DisplayName("expireReservation(): 만료 처리 후 ScheduleSeat가 있으면 release() 호출")
	void expireReservation_success_releaseSeat() {
		// given
		Reservation reservation = mock(Reservation.class);
		ReservationStatus status = mock(ReservationStatus.class);
		ScheduleSeat scheduleSeat = mock(ScheduleSeat.class);

		when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
		when(reservation.getStatus()).thenReturn(status);
		when(status.canExpire()).thenReturn(true);

		when(reservation.getScheduleId()).thenReturn(SCHEDULE_ID);
		when(reservation.getSeatId()).thenReturn(SEAT_ID);

		when(scheduleSeatRepository.findByScheduleIdAndSeatId(SCHEDULE_ID, SEAT_ID))
			.thenReturn(Optional.of(scheduleSeat));

		// when
		assertThatNoException().isThrownBy(() -> reservationService.expireReservation(RESERVATION_ID));

		// then
		verify(reservation).expire();
		verify(scheduleSeat).release();
	}

	@Test
	@DisplayName("expireReservation(): 만료 처리 후 ScheduleSeat가 없으면 release()를 호출하지 않는다")
	void expireReservation_success_noSeat_noop() {
		// given
		Reservation reservation = mock(Reservation.class);
		ReservationStatus status = mock(ReservationStatus.class);

		when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
		when(reservation.getStatus()).thenReturn(status);
		when(status.canExpire()).thenReturn(true);

		when(reservation.getScheduleId()).thenReturn(SCHEDULE_ID);
		when(reservation.getSeatId()).thenReturn(SEAT_ID);

		when(scheduleSeatRepository.findByScheduleIdAndSeatId(SCHEDULE_ID, SEAT_ID))
			.thenReturn(Optional.empty());

		// when
		assertThatNoException().isThrownBy(() -> reservationService.expireReservation(RESERVATION_ID));

		// then
		verify(reservation).expire();
		verify(scheduleSeatRepository).findByScheduleIdAndSeatId(SCHEDULE_ID, SEAT_ID);
	}

	@Test
	@DisplayName("getReservation(): 내 예매가 아니면 RESERVATION_FORBIDDEN 예외")
	void getReservation_forbidden_throw() {
		// given
		Reservation reservation = mock(Reservation.class);

		when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
		when(reservation.getMemberId()).thenReturn(OTHER_MEMBER_ID);

		// when & then
		assertThrows(BusinessException.class, () -> reservationService.getReservation(RESERVATION_ID, MEMBER_ID));
	}

	@Test
	@DisplayName("getReservation(): 내 예매면 ReservationRes 반환")
	void getReservation_success() {
		// given
		Reservation reservation = mock(Reservation.class);

		when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
		when(reservation.getMemberId()).thenReturn(MEMBER_ID);

		when(reservation.getStatus()).thenReturn(ReservationStatus.COMPLETED); // 또는 실제 존재하는 아무 상태

		// when
		ReservationRes result = reservationService.getReservation(RESERVATION_ID, MEMBER_ID);

		// then
		assertThat(result).isNotNull();
	}

	@Test
	@DisplayName("getMyReservations(): repository 결과를 ReservationRes 리스트로 변환해 반환")
	void getMyReservations_success() {
		// given
		Reservation r1 = mock(Reservation.class);
		Reservation r2 = mock(Reservation.class);

		when(r1.getStatus()).thenReturn(ReservationStatus.COMPLETED);
		when(r2.getStatus()).thenReturn(ReservationStatus.CANCELED); // 존재하는 상태로 아무거나 OK

		when(reservationRepository.findAllByMemberId(MEMBER_ID)).thenReturn(List.of(r1, r2));

		// when
		List<ReservationRes> result = reservationService.getMyReservations(MEMBER_ID);

		// then
		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);
	}

	@Test
	@DisplayName("getReservationDetail(): 결과가 null이면 RESERVATION_NOT_FOUND 예외")
	void getReservationDetail_null_throw() {
		// given
		when(reservationRepository.findReservationDetail(RESERVATION_ID, MEMBER_ID)).thenReturn(null);

		// when & then
		assertThrows(BusinessException.class, () -> reservationService.getReservationDetail(RESERVATION_ID, MEMBER_ID));
	}

	@Test
	@DisplayName("getReservationDetail(): 결과가 있으면 그대로 반환")
	void getReservationDetail_success() {
		// given
		ReservationDetailRes detail = mock(ReservationDetailRes.class);
		when(reservationRepository.findReservationDetail(RESERVATION_ID, MEMBER_ID)).thenReturn(detail);

		// when
		ReservationDetailRes result = reservationService.getReservationDetail(RESERVATION_ID, MEMBER_ID);

		// then
		assertThat(result).isSameAs(detail);
	}

	@Test
	@DisplayName("getMyReservationsDetail(): repository 결과를 그대로 반환")
	void getMyReservationsDetail_success() {
		// given
		List<ReservationDetailRes> details = List.of(mock(ReservationDetailRes.class));
		when(reservationRepository.findMyReservationDetails(MEMBER_ID)).thenReturn(details);

		// when
		List<ReservationDetailRes> result = reservationService.getMyReservationsDetail(MEMBER_ID);

		// then
		assertThat(result).isSameAs(details);
	}

	@Test
	@DisplayName("cancelReservation(): reservationId가 없으면 RESERVATION_NOT_FOUND 예외")
	void cancelReservation_notFound_throw() {
		// given
		when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

		// when & then
		assertThrows(BusinessException.class, () -> reservationService.cancelReservation(RESERVATION_ID, MEMBER_ID));
	}

	@Test
	@DisplayName("completeReservation(): reservationId가 없으면 RESERVATION_NOT_FOUND 예외")
	void completeReservation_notFound_throw() {
		// given
		when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

		// when & then
		assertThrows(BusinessException.class, () -> reservationService.completeReservation(RESERVATION_ID));
	}

	@Test
	@DisplayName("expireReservation(): reservationId가 없으면 RESERVATION_NOT_FOUND 예외")
	void expireReservation_notFound_throw() {
		// given
		when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

		// when & then
		assertThrows(BusinessException.class, () -> reservationService.expireReservation(RESERVATION_ID));
	}
}
