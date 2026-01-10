package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentMethod;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.payment.repository.PaymentRepository;
import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.entity.PerformanceStatus;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.prereservation.booking.entity.PrereservationBooking;
import com.back.b2st.domain.prereservation.booking.entity.PrereservationBookingStatus;
import com.back.b2st.domain.prereservation.booking.repository.PrereservationBookingRepository;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.repository.TicketRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.domain.venue.venue.repository.VenueRepository;
import com.back.b2st.global.error.exception.BusinessException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PrereservationPaymentFinalizerIntegrationTest {

	@TestConfiguration
	static class TestClockConfig {
		@Bean
		@Primary
		public Clock testClock() {
			return Clock.fixed(
				Instant.parse("2025-01-01T14:00:00Z"),
				ZoneId.of("UTC")
			);
		}
	}

	@Autowired
	private PrereservationPaymentFinalizer prereservationPaymentFinalizer;

	@Autowired
	private PrereservationBookingRepository prereservationBookingRepository;

	@Autowired
	private PerformanceScheduleRepository performanceScheduleRepository;

	@Autowired
	private ScheduleSeatRepository scheduleSeatRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private PerformanceRepository performanceRepository;

	@Autowired
	private VenueRepository venueRepository;

	private static final Long MEMBER_ID = 10L;
	private static final Long SEAT_ID = 1000L;

	private Performance performance;
	private PerformanceSchedule schedule;
	private ScheduleSeat scheduleSeat;

	@BeforeEach
	void setUp() {
		// Clean up
		ticketRepository.deleteAll();
		paymentRepository.deleteAll();
		reservationRepository.deleteAll();
		prereservationBookingRepository.deleteAll();
		scheduleSeatRepository.deleteAll();
		performanceScheduleRepository.deleteAll();
		performanceRepository.deleteAll();
		venueRepository.deleteAll();

		// Create test venue
		Venue venue = venueRepository.save(
			Venue.builder()
				.name("테스트 공연장")
				.build()
		);

		// Create test performance
		performance = performanceRepository.save(
			Performance.builder()
				.venue(venue)
				.title("테스트 신청예매 공연")
				.category("연극")
				.posterKey(null)
				.description("테스트용")
				.startDate(LocalDateTime.now())
				.endDate(LocalDateTime.now().plusDays(30))
				.status(PerformanceStatus.ACTIVE)
				.build()
		);

		// Create test schedule with PRERESERVE booking type
		schedule = performanceScheduleRepository.save(
			PerformanceSchedule.builder()
				.performance(performance)
				.roundNo(1)
				.startAt(LocalDateTime.now().plusDays(1))
				.bookingType(BookingType.PRERESERVE)
				.bookingOpenAt(LocalDateTime.now().minusHours(1))
				.bookingCloseAt(LocalDateTime.now().plusDays(30))
				.build()
		);

		// Create test schedule seat
		scheduleSeat = scheduleSeatRepository.save(
			ScheduleSeat.builder()
				.scheduleId(schedule.getPerformanceScheduleId())
				.seatId(SEAT_ID)
				.build()
		);
	}

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
		Payment payment = paymentRepository.save(
			Payment.builder()
				.orderId("ORDER-TEST-001")
				.memberId(MEMBER_ID)
				.domainType(DomainType.PRERESERVATION)
				.domainId(999L)
				.amount(30000L)
				.method(PaymentMethod.CARD)
				.expiresAt(LocalDateTime.now().plusMinutes(5))
				.build()
		);

		assertThatThrownBy(() -> prereservationPaymentFinalizer.finalizePayment(payment))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException)ex).getErrorCode())
				.isEqualTo(PaymentErrorCode.DOMAIN_NOT_FOUND));
	}

	@Test
	@DisplayName("finalizePayment(): 다른 사용자의 booking이면 UNAUTHORIZED_PAYMENT_ACCESS 예외")
	void finalizePayment_unauthorizedMember_throw() {
		PrereservationBooking booking = prereservationBookingRepository.save(
			PrereservationBooking.builder()
				.memberId(999L)
				.scheduleId(schedule.getPerformanceScheduleId())
				.scheduleSeatId(scheduleSeat.getId())
				.expiresAt(LocalDateTime.now().plusMinutes(5))
				.build()
		);

		Payment payment = paymentRepository.save(
			Payment.builder()
				.orderId("ORDER-TEST-002")
				.memberId(MEMBER_ID)
				.domainType(DomainType.PRERESERVATION)
				.domainId(booking.getId())
				.amount(30000L)
				.method(PaymentMethod.CARD)
				.expiresAt(LocalDateTime.now().plusMinutes(5))
				.build()
		);

		assertThatThrownBy(() -> prereservationPaymentFinalizer.finalizePayment(payment))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException)ex).getErrorCode())
				.isEqualTo(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS));
	}

	@Test
	@DisplayName("finalizePayment(): bookingType이 PRERESERVE가 아니면 예외")
	void finalizePayment_wrongBookingType_throw() {
		// Create schedule with wrong booking type
		PerformanceSchedule wrongSchedule = performanceScheduleRepository.save(
			PerformanceSchedule.builder()
				.performance(performance)
				.roundNo(2)
				.startAt(LocalDateTime.now().plusDays(1))
				.bookingType(BookingType.FIRST_COME)
				.bookingOpenAt(LocalDateTime.now().minusHours(1))
				.bookingCloseAt(LocalDateTime.now().plusDays(30))
				.build()
		);

		ScheduleSeat wrongScheduleSeat = scheduleSeatRepository.save(
			ScheduleSeat.builder()
				.scheduleId(wrongSchedule.getPerformanceScheduleId())
				.seatId(SEAT_ID)
				.build()
		);

		PrereservationBooking booking = prereservationBookingRepository.save(
			PrereservationBooking.builder()
				.memberId(MEMBER_ID)
				.scheduleId(wrongSchedule.getPerformanceScheduleId())
				.scheduleSeatId(wrongScheduleSeat.getId())
				.expiresAt(LocalDateTime.now().plusMinutes(5))
				.build()
		);

		Payment payment = paymentRepository.save(
			Payment.builder()
				.orderId("ORDER-TEST-003")
				.memberId(MEMBER_ID)
				.domainType(DomainType.PRERESERVATION)
				.domainId(booking.getId())
				.amount(30000L)
				.method(PaymentMethod.CARD)
				.expiresAt(LocalDateTime.now().plusMinutes(5))
				.build()
		);

		assertThatThrownBy(() -> prereservationPaymentFinalizer.finalizePayment(payment))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> {
				assertThat(((BusinessException)ex).getErrorCode()).isEqualTo(PaymentErrorCode.DOMAIN_NOT_PAYABLE);
				assertThat(ex).hasMessageContaining("신청 예매 결제 대상이 아닙니다");
			});
	}

	@Test
	@DisplayName("finalizePayment(): 이미 완료된 booking은 idempotent 처리")
	void finalizePayment_alreadyCompleted_idempotent() {
		// Hold the seat first
		scheduleSeat.hold(LocalDateTime.now().plusMinutes(5));
		scheduleSeat.sold();
		scheduleSeatRepository.save(scheduleSeat);

		// Create completed booking
		PrereservationBooking booking = prereservationBookingRepository.save(
			PrereservationBooking.builder()
				.memberId(MEMBER_ID)
				.scheduleId(schedule.getPerformanceScheduleId())
				.scheduleSeatId(scheduleSeat.getId())
				.expiresAt(LocalDateTime.now().plusMinutes(5))
				.build()
		);
		booking.complete(LocalDateTime.now());
		prereservationBookingRepository.save(booking);

		// Create existing reservation
		Reservation existingReservation = reservationRepository.save(
			Reservation.builder()
				.scheduleId(schedule.getPerformanceScheduleId())
				.memberId(MEMBER_ID)
				.expiresAt(LocalDateTime.now().plusMinutes(5))
				.build()
		);
		existingReservation.complete(LocalDateTime.now());
		reservationRepository.save(existingReservation);

		// Create existing ticket
		ticketRepository.save(
			Ticket.builder()
				.reservationId(existingReservation.getId())
				.memberId(MEMBER_ID)
				.seatId(SEAT_ID)
				.build()
		);

		Payment payment = paymentRepository.save(
			Payment.builder()
				.orderId("ORDER-TEST-004")
				.memberId(MEMBER_ID)
				.domainType(DomainType.PRERESERVATION)
				.domainId(booking.getId())
				.amount(30000L)
				.method(PaymentMethod.CARD)
				.expiresAt(LocalDateTime.now().plusMinutes(5))
				.build()
		);

		// Should not throw
		assertThatCode(() -> prereservationPaymentFinalizer.finalizePayment(payment))
			.doesNotThrowAnyException();

		// Verify seat is still SOLD
		ScheduleSeat updatedSeat = scheduleSeatRepository.findById(scheduleSeat.getId()).orElseThrow();
		assertThat(updatedSeat.getStatus()).isEqualTo(SeatStatus.SOLD);

		// Verify booking is still COMPLETED
		PrereservationBooking updatedBooking = prereservationBookingRepository.findById(booking.getId()).orElseThrow();
		assertThat(updatedBooking.getStatus()).isEqualTo(PrereservationBookingStatus.COMPLETED);
	}

	@Test
	@DisplayName("finalizePayment(): 정상 케이스 - booking 완료 및 티켓 생성")
	void finalizePayment_success() {
		// Hold the seat first
		scheduleSeat.hold(LocalDateTime.now().plusMinutes(5));
		scheduleSeatRepository.save(scheduleSeat);

		// Create booking
		PrereservationBooking booking = prereservationBookingRepository.save(
			PrereservationBooking.builder()
				.memberId(MEMBER_ID)
				.scheduleId(schedule.getPerformanceScheduleId())
				.scheduleSeatId(scheduleSeat.getId())
				.expiresAt(LocalDateTime.now().plusMinutes(5))
				.build()
		);

		Payment payment = paymentRepository.save(
			Payment.builder()
				.orderId("ORDER-TEST-005")
				.memberId(MEMBER_ID)
				.domainType(DomainType.PRERESERVATION)
				.domainId(booking.getId())
				.amount(30000L)
				.method(PaymentMethod.CARD)
				.expiresAt(LocalDateTime.now().plusMinutes(5))
				.build()
		);

		// Execute
		assertThatCode(() -> prereservationPaymentFinalizer.finalizePayment(payment))
			.doesNotThrowAnyException();

		// Verify booking is completed
		PrereservationBooking updatedBooking = prereservationBookingRepository.findById(booking.getId()).orElseThrow();
		assertThat(updatedBooking.getStatus()).isEqualTo(PrereservationBookingStatus.COMPLETED);
		assertThat(updatedBooking.getCompletedAt()).isNotNull();

		// Verify seat is sold
		ScheduleSeat updatedSeat = scheduleSeatRepository.findById(scheduleSeat.getId()).orElseThrow();
		assertThat(updatedSeat.getStatus()).isEqualTo(SeatStatus.SOLD);

		// Verify reservation is created
		assertThat(reservationRepository.findAll()).hasSize(1);
		Reservation reservation = reservationRepository.findAll().get(0);
		assertThat(reservation.getScheduleId()).isEqualTo(schedule.getPerformanceScheduleId());
		assertThat(reservation.getMemberId()).isEqualTo(MEMBER_ID);
		assertThat(reservation.getCompletedAt()).isNotNull();

		// Verify ticket is created
		assertThat(ticketRepository.findAll()).hasSize(1);
		Ticket ticket = ticketRepository.findAll().get(0);
		assertThat(ticket.getReservationId()).isEqualTo(reservation.getId());
		assertThat(ticket.getMemberId()).isEqualTo(MEMBER_ID);
		assertThat(ticket.getSeatId()).isEqualTo(SEAT_ID);
	}
}
