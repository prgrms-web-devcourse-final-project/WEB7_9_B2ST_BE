package com.back.b2st.domain.reservation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.back.b2st.domain.payment.dto.response.PaymentConfirmRes;
import com.back.b2st.domain.payment.service.PaymentViewService;
import com.back.b2st.domain.reservation.dto.response.AdminReservationSummaryRes;
import com.back.b2st.domain.reservation.dto.response.ReservationDetailRes;
import com.back.b2st.domain.reservation.dto.response.ReservationDetailWithPaymentRes;
import com.back.b2st.domain.reservation.dto.response.ReservationSeatInfo;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationStatus;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.reservation.repository.ReservationSeatRepository;
import com.back.b2st.domain.scheduleseat.service.ScheduleSeatStateService;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class AdminReservationServiceTest {

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private ReservationSeatRepository reservationSeatRepository;

	@Mock
	private PaymentViewService paymentViewService;

	@Mock
	private ScheduleSeatStateService scheduleSeatStateService;

	@InjectMocks
	private AdminReservationService adminReservationService;

	@Test
	@DisplayName("관리자 예매 상태별 조회 - Page로 매핑해서 반환한다")
	void getReservationsByStatus_success() {
		// given
		ReservationStatus status = ReservationStatus.COMPLETED;
		Long scheduleId = 1L;
		Long memberId = 2L;
		Pageable pageable = PageRequest.of(0, 20);

		Reservation r1 = mock(Reservation.class);
		given(r1.getId()).willReturn(10L);
		given(r1.getScheduleId()).willReturn(scheduleId);
		given(r1.getMemberId()).willReturn(memberId);
		given(r1.getStatus()).willReturn(status);
		given(r1.getCreatedAt()).willReturn(LocalDateTime.now().minusMinutes(1));
		given(r1.getExpiresAt()).willReturn(LocalDateTime.now().plusMinutes(4));

		Reservation r2 = mock(Reservation.class);
		given(r2.getId()).willReturn(11L);
		given(r2.getScheduleId()).willReturn(scheduleId);
		given(r2.getMemberId()).willReturn(memberId);
		given(r2.getStatus()).willReturn(status);
		given(r2.getCreatedAt()).willReturn(LocalDateTime.now().minusMinutes(2));
		given(r2.getExpiresAt()).willReturn(LocalDateTime.now().plusMinutes(3));

		Page<Reservation> reservationPage = new PageImpl<>(List.of(r1, r2), pageable, 2);

		given(reservationRepository.findByStatusWithOptionalFilters(status, scheduleId, memberId, pageable))
			.willReturn(reservationPage);

		given(reservationSeatRepository.countByReservationId(10L)).willReturn(1);
		given(reservationSeatRepository.countByReservationId(11L)).willReturn(2);

		// when
		Page<AdminReservationSummaryRes> result =
			adminReservationService.getReservationsByStatus(status, scheduleId, memberId, pageable);

		// then
		// given-when-then
		// then: page metadata 유지 + content 매핑 확인
		org.assertj.core.api.Assertions.assertThat(result.getTotalElements()).isEqualTo(2);
		org.assertj.core.api.Assertions.assertThat(result.getContent()).hasSize(2);
		org.assertj.core.api.Assertions.assertThat(result.getContent().get(0).seatCount()).isEqualTo(1);
		org.assertj.core.api.Assertions.assertThat(result.getContent().get(1).seatCount()).isEqualTo(2);

		then(reservationRepository).should().findByStatusWithOptionalFilters(status, scheduleId, memberId, pageable);
		then(reservationSeatRepository).should().countByReservationId(10L);
		then(reservationSeatRepository).should().countByReservationId(11L);
	}

	@Test
	@DisplayName("관리자 예매 상세 조회 - ReservationDetailWithPaymentRes를 반환한다")
	void getReservationDetail_success() {
		// given
		Long reservationId = 1L;
		Long ownerMemberId = 2L;

		Reservation reservationEntity = mock(Reservation.class);
		given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservationEntity));
		given(reservationEntity.getMemberId()).willReturn(ownerMemberId);

		ReservationDetailRes detailRes = mock(ReservationDetailRes.class);
		given(reservationRepository.findReservationDetail(reservationId, ownerMemberId)).willReturn(detailRes);

		List<ReservationSeatInfo> seats = List.of(mock(ReservationSeatInfo.class), mock(ReservationSeatInfo.class));
		given(reservationSeatRepository.findSeatInfos(reservationId)).willReturn(seats);

		PaymentConfirmRes payment = mock(PaymentConfirmRes.class);
		given(paymentViewService.getByReservationId(reservationId, ownerMemberId)).willReturn(payment);

		// when
		ReservationDetailWithPaymentRes result = adminReservationService.getReservationDetail(reservationId);

		// then
		org.assertj.core.api.Assertions.assertThat(result).isNotNull();
		org.assertj.core.api.Assertions.assertThat(result.reservation()).isSameAs(detailRes);
		org.assertj.core.api.Assertions.assertThat(result.seats()).isSameAs(seats);
		org.assertj.core.api.Assertions.assertThat(result.payment()).isSameAs(payment);

		then(reservationRepository).should().findById(reservationId);
		then(reservationRepository).should().findReservationDetail(reservationId, ownerMemberId);
		then(reservationSeatRepository).should().findSeatInfos(reservationId);
		then(paymentViewService).should().getByReservationId(reservationId, ownerMemberId);
	}

	@Test
	@DisplayName("관리자 예매 상세 조회 - 예매가 없으면 BusinessException")
	void getReservationDetail_notFound() {
		// given
		Long reservationId = 999L;
		given(reservationRepository.findById(reservationId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> adminReservationService.getReservationDetail(reservationId))
			.isInstanceOf(BusinessException.class);

		then(reservationRepository).should().findById(reservationId);
		then(reservationRepository).should(never()).findReservationDetail(anyLong(), anyLong());
		then(reservationSeatRepository).shouldHaveNoInteractions();
		then(paymentViewService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("관리자 예매 강제 취소 - 취소 처리 후 좌석들을 forceToAvailable 호출한다")
	void forceCancel_success() {
		// given
		Long reservationId = 1L;
		Long scheduleId = 10L;

		Reservation reservation = mock(Reservation.class);
		given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
		given(reservation.getStatus()).willReturn(ReservationStatus.COMPLETED);
		given(reservation.getScheduleId()).willReturn(scheduleId);

		ReservationSeatInfo s1 = mock(ReservationSeatInfo.class);
		ReservationSeatInfo s2 = mock(ReservationSeatInfo.class);
		given(s1.seatId()).willReturn(101L);
		given(s2.seatId()).willReturn(102L);

		given(reservationSeatRepository.findSeatInfos(reservationId)).willReturn(List.of(s1, s2));

		// when
		adminReservationService.forceCancel(reservationId);

		// then
		then(reservationRepository).should().findById(reservationId);
		then(reservation).should().cancel(any(LocalDateTime.class));
		then(reservationSeatRepository).should().findSeatInfos(reservationId);

		then(scheduleSeatStateService).should().forceToAvailable(scheduleId, 101L);
		then(scheduleSeatStateService).should().forceToAvailable(scheduleId, 102L);
	}

	@Test
	@DisplayName("관리자 예매 강제 취소 - 이미 CANCELED면 아무 것도 하지 않고 종료한다")
	void forceCancel_alreadyCanceled_noop() {
		// given
		Long reservationId = 1L;

		Reservation reservation = mock(Reservation.class);
		given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
		given(reservation.getStatus()).willReturn(ReservationStatus.CANCELED);

		// when
		adminReservationService.forceCancel(reservationId);

		// then
		then(reservation).should(never()).cancel(any(LocalDateTime.class));
		then(reservationSeatRepository).should(never()).findSeatInfos(anyLong());
		then(scheduleSeatStateService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("관리자 예매 강제 취소 - 예매가 없으면 BusinessException")
	void forceCancel_notFound() {
		// given
		Long reservationId = 999L;
		given(reservationRepository.findById(reservationId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> adminReservationService.forceCancel(reservationId))
			.isInstanceOf(BusinessException.class);

		then(reservationRepository).should().findById(reservationId);
		then(reservationSeatRepository).shouldHaveNoInteractions();
		then(scheduleSeatStateService).shouldHaveNoInteractions();
	}
}
