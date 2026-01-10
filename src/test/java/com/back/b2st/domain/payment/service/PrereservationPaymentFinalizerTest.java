package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.prereservation.booking.entity.PrereservationBooking;
import com.back.b2st.domain.prereservation.booking.entity.PrereservationBookingStatus;
import com.back.b2st.domain.prereservation.booking.repository.PrereservationBookingRepository;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.service.TicketService;
import com.back.b2st.global.error.exception.BusinessException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;

@ExtendWith(MockitoExtension.class)
class PrereservationPaymentFinalizerTest {

	@Mock
	private EntityManager entityManager;

	@Mock
	private PerformanceScheduleRepository performanceScheduleRepository;

	@Mock
	private PrereservationBookingRepository prereservationBookingRepository;

	@Mock
	private TicketService ticketService;

	@Mock
	private Clock clock;

	@InjectMocks
	private PrereservationPaymentFinalizer prereservationPaymentFinalizer;

	private static final Long BOOKING_ID = 1L;
	private static final Long MEMBER_ID = 10L;
	private static final Long SCHEDULE_ID = 100L;
	private static final Long SEAT_ID = 1000L;
	private static final Long SCHEDULE_SEAT_ID = 999L;

	@Test
	@DisplayName("supports(): DomainType.PRERESERVATION 지원")
	void supports_prereservation_true() {
		assertThat(prereservationPaymentFinalizer.supports(DomainType.PRERESERVATION)).isTrue();
	}

	@Test
	@DisplayName("supports(): 다른 도메인 타입은 미지원")
	void supports_others_false() {
		assertThat(prereservationPaymentFinalizer.supports(DomainType.RESERVATION)).isFalse();
		assertThat(prereservationPaymentFinalizer.supports(DomainType.LOTTERY)).isFalse();
		assertThat(prereservationPaymentFinalizer.supports(DomainType.TRADE)).isFalse();
	}

	@Test
	@DisplayName("finalizePayment(): booking이 없으면 DOMAIN_NOT_FOUND 예외")
	void finalizePayment_bookingNotFound_throw() {
		Payment payment = mock(Payment.class);
		given(payment.getDomainId()).willReturn(BOOKING_ID);
		given(prereservationBookingRepository.findByIdWithLock(BOOKING_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> prereservationPaymentFinalizer.finalizePayment(payment))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException)ex).getErrorCode())
				.isEqualTo(PaymentErrorCode.DOMAIN_NOT_FOUND));
	}

	@Test
	@DisplayName("finalizePayment(): 다른 사용자의 booking이면 UNAUTHORIZED_PAYMENT_ACCESS 예외")
	void finalizePayment_unauthorizedMember_throw() {
		Payment payment = mock(Payment.class);
		PrereservationBooking booking = mock(PrereservationBooking.class);

		given(payment.getDomainId()).willReturn(BOOKING_ID);
		given(payment.getMemberId()).willReturn(MEMBER_ID);
		given(booking.getMemberId()).willReturn(999L);
		given(prereservationBookingRepository.findByIdWithLock(BOOKING_ID)).willReturn(Optional.of(booking));

		assertThatThrownBy(() -> prereservationPaymentFinalizer.finalizePayment(payment))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException)ex).getErrorCode())
				.isEqualTo(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS));
	}

	@Test
	@DisplayName("finalizePayment(): bookingType이 PRERESERVE가 아니면 예외")
	void finalizePayment_wrongBookingType_throw() {
		Payment payment = mock(Payment.class);
		PrereservationBooking booking = mock(PrereservationBooking.class);
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);

		given(payment.getDomainId()).willReturn(BOOKING_ID);
		given(payment.getMemberId()).willReturn(MEMBER_ID);
		given(booking.getMemberId()).willReturn(MEMBER_ID);
		given(booking.getStatus()).willReturn(PrereservationBookingStatus.CREATED);
		given(booking.getScheduleId()).willReturn(SCHEDULE_ID);
		given(prereservationBookingRepository.findByIdWithLock(BOOKING_ID)).willReturn(Optional.of(booking));

		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(schedule.getBookingType()).willReturn(BookingType.FIRST_COME);

		assertThatThrownBy(() -> prereservationPaymentFinalizer.finalizePayment(payment))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> {
				assertThat(((BusinessException)ex).getErrorCode()).isEqualTo(PaymentErrorCode.DOMAIN_NOT_PAYABLE);
				assertThat(ex).hasMessageContaining("신청 예매 결제 대상이 아닙니다");
			});
	}

	// NOTE: 복잡한 Reservation 생성 로직 테스트는 PrereservationPaymentFinalizerIntegrationTest에서 수행
	// EntityManager mocking이 복잡하여 통합 테스트로 전환
}

