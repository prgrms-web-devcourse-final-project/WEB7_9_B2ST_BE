package com.back.b2st.domain.reservation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.payment.dto.response.PaymentConfirmRes;
import com.back.b2st.domain.payment.entity.PaymentStatus;
import com.back.b2st.domain.payment.service.PaymentViewService;
import com.back.b2st.domain.reservation.dto.response.ReservationDetailRes;
import com.back.b2st.domain.reservation.dto.response.ReservationDetailRes.PerformanceInfo;
import com.back.b2st.domain.reservation.dto.response.ReservationDetailWithPaymentRes;
import com.back.b2st.domain.reservation.dto.response.ReservationRes;
import com.back.b2st.domain.reservation.dto.response.ReservationSeatInfo;
import com.back.b2st.domain.reservation.error.ReservationErrorCode;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.reservation.repository.ReservationSeatRepository;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class ReservationViewServiceTest {

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private ReservationSeatRepository reservationSeatRepository;

	@Mock
	private PaymentViewService paymentViewService;

	@InjectMocks
	private ReservationViewService reservationViewService;

	private static final Long RESERVATION_ID = 1L;
	private static final Long MEMBER_ID = 10L;

	@Test
	@DisplayName("getReservationDetail(): 예매 상세 + 좌석 + 결제 정보를 조회한다")
	void getReservationDetail_success() {
		// given
		ReservationDetailRes reservationDetail =
			new ReservationDetailRes(
				RESERVATION_ID,
				"PENDING",
				new PerformanceInfo(
					100L,
					200L,
					"테스트 공연",
					"콘서트",
					LocalDateTime.now().plusDays(1),
					LocalDateTime.now().plusDays(1).withHour(19)
				)
			);

		List<ReservationSeatInfo> seats = List.of(
			new ReservationSeatInfo(1L, 10L, "A", "1", 1),
			new ReservationSeatInfo(2L, 10L, "A", "1", 2)
		);

		PaymentConfirmRes payment =
			new PaymentConfirmRes(
				50L,
				"ORDER-123",
				10000L,
				PaymentStatus.DONE,
				LocalDateTime.now()
			);

		when(reservationRepository.findReservationDetail(RESERVATION_ID, MEMBER_ID))
			.thenReturn(reservationDetail);
		when(reservationSeatRepository.findSeatInfos(RESERVATION_ID))
			.thenReturn(seats);
		when(paymentViewService.getByReservationId(RESERVATION_ID, MEMBER_ID))
			.thenReturn(payment);

		// when
		ReservationDetailWithPaymentRes result =
			reservationViewService.getReservationDetail(RESERVATION_ID, MEMBER_ID);

		// then
		assertThat(result).isNotNull();
		assertThat(result.reservation()).isEqualTo(reservationDetail);
		assertThat(result.seats()).hasSize(2);
		assertThat(result.payment()).isEqualTo(payment);
	}

	@Test
	@DisplayName("getReservationDetail(): 예매가 없으면 RESERVATION_NOT_FOUND")
	void getReservationDetail_notFound_throw() {
		// given
		when(reservationRepository.findReservationDetail(RESERVATION_ID, MEMBER_ID))
			.thenReturn(null);

		// when & then
		assertThatThrownBy(() ->
			reservationViewService.getReservationDetail(RESERVATION_ID, MEMBER_ID)
		)
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND);

		verifyNoInteractions(reservationSeatRepository);
		verifyNoInteractions(paymentViewService);
	}

	@Test
	@DisplayName("getMyReservations(): 내 예매 목록을 조회한다")
	void getMyReservations_success() {
		// given
		List<ReservationRes> reservations = List.of(
			mock(ReservationRes.class),
			mock(ReservationRes.class)
		);

		when(reservationRepository.findMyReservations(MEMBER_ID))
			.thenReturn(reservations);

		// when
		List<ReservationRes> result =
			reservationViewService.getMyReservations(MEMBER_ID);

		// then
		assertThat(result).hasSize(2);
	}
}
