package com.back.b2st.domain.reservation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.reservation.dto.request.ReservationReq;
import com.back.b2st.domain.reservation.dto.response.ReservationDetailRes;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationStatus;
import com.back.b2st.domain.reservation.error.ReservationErrorCode;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.scheduleseat.service.SeatHoldTokenService;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private SeatHoldTokenService seatHoldTokenService;

	@InjectMocks
	private ReservationService reservationService;

	@Test
	@DisplayName("예매 생성 - HOLD 소유권이 있으면 성공")
	void 예매생성_HOLD소유권있음_성공() {
		// given
		Long memberId = 1L;
		Long reservationId = 1L;
		Long scheduleId = 10L;
		Long seatId = 100L;

		ReservationReq request = new ReservationReq(scheduleId, seatId);

		// 1. HOLD 소유권 검증 통과
		willDoNothing()
			.given(seatHoldTokenService)
			.validateOwnership(scheduleId, seatId, memberId);

		// 2. save는 그대로 반환 (id는 DB에서 생성된다고 가정)
		given(reservationRepository.save(any(Reservation.class)))
			.willAnswer(invocation -> invocation.getArgument(0));

		// 3. findReservationDetail mock (DTO 직접 생성)
		ReservationDetailRes detailRes =
			new ReservationDetailRes(
				reservationId,
				ReservationStatus.CREATED.name(),
				new ReservationDetailRes.PerformanceInfo(
					1L,               // performanceId
					scheduleId,        // performanceScheduleId
					"테스트 공연",        // title
					"콘서트",             // category
					LocalDateTime.now(),
					LocalDateTime.now()
				),
				new ReservationDetailRes.SeatInfo(
					seatId,
					1L,                // sectionId
					"A",                // sectionName
					"A",                // rowLabel
					1                   // seatNumber
				)
			);

		given(reservationRepository.findReservationDetail(any(), eq(memberId)))
			.willReturn(detailRes);

		// when
		ReservationDetailRes result =
			reservationService.createReservation(memberId, request);

		// then
		assertThat(result).isNotNull();
		assertThat(result.reservationId()).isEqualTo(reservationId);
		assertThat(result.status()).isEqualTo(ReservationStatus.CREATED.name());
		assertThat(result.seat().seatId()).isEqualTo(seatId);
		assertThat(result.performance().performanceScheduleId()).isEqualTo(scheduleId);
	}

	@Test
	@DisplayName("예매 생성 - HOLD가 없으면 예외 발생")
	void 예매생성_HOLD없음_예외() {
		// given
		Long memberId = 1L;
		ReservationReq request = new ReservationReq(10L, 100L);

		willThrow(new BusinessException(ReservationErrorCode.SEAT_HOLD_EXPIRED))
			.given(seatHoldTokenService)
			.validateOwnership(any(), any(), any());

		// when & then
		assertThatThrownBy(() ->
			reservationService.createReservation(memberId, request)
		)
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(
				ReservationErrorCode.SEAT_HOLD_EXPIRED.getMessage()
			);
	}

	@Test
	@DisplayName("예매 생성 - 다른 사용자의 HOLD면 예외 발생")
	void 예매생성_HOLD소유자불일치_예외() {
		// given
		Long memberId = 1L;
		ReservationReq request = new ReservationReq(10L, 100L);

		willThrow(new BusinessException(ReservationErrorCode.SEAT_HOLD_FORBIDDEN))
			.given(seatHoldTokenService)
			.validateOwnership(any(), any(), eq(memberId));

		// when & then
		assertThatThrownBy(() ->
			reservationService.createReservation(memberId, request)
		)
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(
				ReservationErrorCode.SEAT_HOLD_FORBIDDEN.getMessage()
			);
	}
}
