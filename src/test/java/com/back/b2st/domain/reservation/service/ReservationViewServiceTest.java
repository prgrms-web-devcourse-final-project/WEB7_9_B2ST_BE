package com.back.b2st.domain.reservation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.payment.dto.response.PaymentConfirmRes;
import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.PaymentStatus;
import com.back.b2st.domain.payment.service.PaymentViewService;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.prereservation.booking.entity.PrereservationBooking;
import com.back.b2st.domain.prereservation.booking.repository.PrereservationBookingRepository;
import com.back.b2st.domain.reservation.dto.response.ReservationDetailRes;
import com.back.b2st.domain.reservation.dto.response.ReservationDetailRes.PerformanceInfo;
import com.back.b2st.domain.reservation.dto.response.ReservationDetailWithPaymentRes;
import com.back.b2st.domain.reservation.dto.response.ReservationRes;
import com.back.b2st.domain.reservation.dto.response.ReservationSeatInfo;
import com.back.b2st.domain.reservation.error.ReservationErrorCode;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.reservation.repository.ReservationSeatRepository;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.repository.TicketRepository;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class ReservationViewServiceTest {

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private ReservationSeatRepository reservationSeatRepository;

	@Mock
	private PrereservationBookingRepository prereservationBookingRepository;

	@Mock
	private ScheduleSeatRepository scheduleSeatRepository;

	@Mock
	private SeatRepository seatRepository;

	@Mock
	private PerformanceScheduleRepository performanceScheduleRepository;

	@Mock
	private TicketRepository ticketRepository;

	@Mock
	private PaymentViewService paymentViewService;

	@InjectMocks
	private ReservationViewService reservationViewService;

	private static final Long RESERVATION_ID = 1L;
	private static final Long MEMBER_ID = 10L;

	@Test
	@DisplayName("getReservationDetail(): 예매 상세 + 좌석 + 결제 정보를 조회한다")
	void getReservationDetail_success() {
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

		when(reservationRepository.findReservationDetail(RESERVATION_ID, MEMBER_ID)).thenReturn(reservationDetail);
		when(reservationSeatRepository.findSeatInfos(RESERVATION_ID)).thenReturn(seats);
		when(paymentViewService.getByReservationId(RESERVATION_ID, MEMBER_ID)).thenReturn(payment);

		ReservationDetailWithPaymentRes result = reservationViewService.getReservationDetail(RESERVATION_ID, MEMBER_ID);

		assertThat(result).isNotNull();
		assertThat(result.reservation()).isEqualTo(reservationDetail);
		assertThat(result.seats()).hasSize(2);
		assertThat(result.payment()).isEqualTo(payment);
	}

	@Test
	@DisplayName("getReservationDetail(): 예매가 없으면 RESERVATION_NOT_FOUND")
	void getReservationDetail_notFound_throw() {
		when(reservationRepository.findReservationDetail(RESERVATION_ID, MEMBER_ID)).thenReturn(null);
		when(ticketRepository.findAllByReservationIdAndMemberId(RESERVATION_ID, MEMBER_ID)).thenReturn(List.of());

		assertThatThrownBy(() -> reservationViewService.getReservationDetail(RESERVATION_ID, MEMBER_ID))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND);

		verifyNoInteractions(reservationSeatRepository);
	}

	@Test
	@DisplayName("getReservationDetail(): 신청예매 티켓이면 prereservationBookingId로 상세를 조회한다")
	void getReservationDetail_prereservationTicket_success() throws Exception {
		Long bookingId = 52L;
		Long scheduleId = 100L;
		Long scheduleSeatId = 200L;
		Long seatId = 1L;

		PrereservationBooking booking = PrereservationBooking.builder()
			.scheduleId(scheduleId)
			.memberId(MEMBER_ID)
			.scheduleSeatId(scheduleSeatId)
			.expiresAt(LocalDateTime.now().plusMinutes(10))
			.build();
		var idField = PrereservationBooking.class.getDeclaredField("id");
		idField.setAccessible(true);
		idField.set(booking, bookingId);

		Ticket ticket = Ticket.builder()
			.reservationId(bookingId)
			.memberId(MEMBER_ID)
			.seatId(seatId)
			.build();

		ScheduleSeat scheduleSeat = ScheduleSeat.builder().scheduleId(scheduleId).seatId(seatId).build();

		Seat seat = Seat.builder()
			.venueId(1L)
			.sectionId(10L)
			.sectionName("A")
			.rowLabel("1")
			.seatNumber(1)
			.build();

		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		var performance = mock(com.back.b2st.domain.performance.entity.Performance.class);
		when(performance.getPerformanceId()).thenReturn(999L);
		when(performance.getTitle()).thenReturn("테스트 공연");
		when(performance.getCategory()).thenReturn("콘서트");
		when(performance.getStartDate()).thenReturn(LocalDateTime.now().plusDays(1));
		when(schedule.getPerformance()).thenReturn(performance);
		when(schedule.getPerformanceScheduleId()).thenReturn(scheduleId);
		when(schedule.getStartAt()).thenReturn(LocalDateTime.now().plusDays(1));

		PaymentConfirmRes payment =
			new PaymentConfirmRes(
				50L,
				"ORDER-PR-1",
				10000L,
				PaymentStatus.DONE,
				LocalDateTime.now()
			);

		when(reservationRepository.findReservationDetail(bookingId, MEMBER_ID)).thenReturn(null);
		when(ticketRepository.findAllByReservationIdAndMemberId(bookingId, MEMBER_ID)).thenReturn(List.of(ticket));
		when(prereservationBookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
		when(scheduleSeatRepository.findById(scheduleSeatId)).thenReturn(Optional.of(scheduleSeat));
		when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));
		when(performanceScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
		when(paymentViewService.getByDomain(DomainType.PRERESERVATION, bookingId, MEMBER_ID)).thenReturn(payment);

		ReservationDetailWithPaymentRes result = reservationViewService.getReservationDetail(bookingId, MEMBER_ID);

		assertThat(result).isNotNull();
		assertThat(result.reservation().reservationId()).isEqualTo(bookingId);
		assertThat(result.seats()).hasSize(1);
		assertThat(result.payment()).isEqualTo(payment);
	}

	@Test
	@DisplayName("getReservationDetail(): 티켓 소유자면 reservation 상세 조회를 허용한다(좌석 매칭)")
	void getReservationDetail_ticketOwner_canViewReservation() {
		Long reservationId = 52L;
		Long seatId = 1L;

		Ticket ticket = Ticket.builder()
			.reservationId(reservationId)
			.memberId(MEMBER_ID)
			.seatId(seatId)
			.build();

		ReservationDetailRes reservationDetail =
			new ReservationDetailRes(
				reservationId,
				"COMPLETED",
				new PerformanceInfo(
					100L,
					200L,
					"테스트 공연",
					"콘서트",
					LocalDateTime.now().plusDays(1),
					LocalDateTime.now().plusDays(1)
				)
			);

		List<ReservationSeatInfo> seats = List.of(new ReservationSeatInfo(seatId, 10L, "A", "1", 1));

		when(reservationRepository.findReservationDetail(reservationId, MEMBER_ID)).thenReturn(null);
		when(ticketRepository.findAllByReservationIdAndMemberId(reservationId, MEMBER_ID)).thenReturn(List.of(ticket));
		when(prereservationBookingRepository.findById(reservationId)).thenReturn(Optional.empty());
		when(reservationRepository.findReservationDetail(reservationId)).thenReturn(reservationDetail);
		when(reservationSeatRepository.findSeatInfos(reservationId)).thenReturn(seats);
		when(paymentViewService.getByReservationId(reservationId, MEMBER_ID)).thenReturn(null);

		ReservationDetailWithPaymentRes result = reservationViewService.getReservationDetail(reservationId, MEMBER_ID);

		assertThat(result).isNotNull();
		assertThat(result.reservation()).isEqualTo(reservationDetail);
		assertThat(result.seats()).hasSize(1);
	}

	@Test
	@DisplayName("getReservationDetail(): reservationId가 충돌해도 ticket 좌석이 매칭되는 도메인으로 분기한다")
	void getReservationDetail_idCollision_routesByTicketSeatMatch() {
		Long id = 52L;
		Long ticketSeatId = 1L;

		Ticket ticket = Ticket.builder()
			.reservationId(id)
			.memberId(MEMBER_ID)
			.seatId(ticketSeatId)
			.build();

		PrereservationBooking booking = PrereservationBooking.builder()
			.scheduleId(100L)
			.memberId(MEMBER_ID)
			.scheduleSeatId(999L)
			.expiresAt(LocalDateTime.now().plusMinutes(10))
			.build();
		ScheduleSeat bookingSeat = ScheduleSeat.builder().scheduleId(100L).seatId(9999L).build();

		ReservationDetailRes reservationDetail =
			new ReservationDetailRes(
				id,
				"COMPLETED",
				new PerformanceInfo(
					100L,
					200L,
					"테스트 공연",
					"콘서트",
					LocalDateTime.now().plusDays(1),
					LocalDateTime.now().plusDays(1)
				)
			);
		List<ReservationSeatInfo> reservationSeats = List.of(new ReservationSeatInfo(ticketSeatId, 10L, "A", "1", 1));

		when(reservationRepository.findReservationDetail(id, MEMBER_ID)).thenReturn(null);
		when(ticketRepository.findAllByReservationIdAndMemberId(id, MEMBER_ID)).thenReturn(List.of(ticket));
		when(prereservationBookingRepository.findById(id)).thenReturn(Optional.of(booking));
		when(scheduleSeatRepository.findById(999L)).thenReturn(Optional.of(bookingSeat));
		when(reservationRepository.findReservationDetail(id)).thenReturn(reservationDetail);
		when(reservationSeatRepository.findSeatInfos(id)).thenReturn(reservationSeats);

		ReservationDetailWithPaymentRes result = reservationViewService.getReservationDetail(id, MEMBER_ID);

		assertThat(result.reservation()).isEqualTo(reservationDetail);
	}

	@Test
	@DisplayName("getMyReservations(): 내 예매 목록을 조회한다")
	void getMyReservations_success() {
		List<ReservationRes> reservations = List.of(mock(ReservationRes.class), mock(ReservationRes.class));
		when(reservationRepository.findMyReservations(MEMBER_ID)).thenReturn(reservations);

		List<ReservationRes> result = reservationViewService.getMyReservations(MEMBER_ID);

		assertThat(result).hasSize(2);
	}
}
