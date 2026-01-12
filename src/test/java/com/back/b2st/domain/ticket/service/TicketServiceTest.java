package com.back.b2st.domain.ticket.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.entity.PerformanceStatus;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.prereservation.booking.entity.PrereservationBooking;
import com.back.b2st.domain.prereservation.booking.repository.PrereservationBookingRepository;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationSeat;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.reservation.repository.ReservationSeatRepository;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.ticket.dto.response.TicketRes;
import com.back.b2st.domain.ticket.entity.AcquisitionType;
import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.entity.TicketStatus;
import com.back.b2st.domain.ticket.error.TicketErrorCode;
import com.back.b2st.domain.ticket.repository.TicketRepository;
import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeType;
import com.back.b2st.domain.trade.repository.TradeRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.domain.venue.venue.repository.VenueRepository;
import com.back.b2st.global.error.exception.BusinessException;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class TicketServiceTest {

	@Autowired
	private TicketRepository ticketRepository;
	@Autowired
	private TicketService ticketService;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private SectionRepository sectionRepository;
	@Autowired
	private SeatRepository seatRepository;
	@Autowired
	private ReservationRepository reservationRepository;
	@Autowired
	private ReservationSeatRepository reservationSeatRepository;
	@Autowired
	private VenueRepository venueRepository;
	@Autowired
	private PerformanceRepository performanceRepository;
	@Autowired
	private PerformanceScheduleRepository performanceScheduleRepository;
	@Autowired
	private ScheduleSeatRepository scheduleSeatRepository;
	@Autowired
	private PrereservationBookingRepository prereservationBookingRepository;
	@Autowired
	private TradeRepository tradeRepository;
	@Autowired
	private EntityManager em;

	private Ticket ticket;
	private Long rId, mId, sId, scheduleId;
	private Venue venue;
	private Section section;

	@BeforeEach
	void setUp() {
		// Create test member
		Member testMember = Member.builder()
			.email("ticket@test.com")
			.password("password")
			.name("Test User")
			.birth(LocalDate.of(1990, 1, 1))
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build();
		Member savedMember = memberRepository.save(testMember);

		// Create test venue
		venue = Venue.builder()
			.name("Test Venue")
			.build();
		venue = venueRepository.save(venue);

		// Create test performance
		Performance performance = Performance.builder()
			.venue(venue)
			.title("Test Performance")
			.category("Test")
			.startDate(java.time.LocalDateTime.now())
			.endDate(java.time.LocalDateTime.now().plusDays(1))
			.status(PerformanceStatus.ACTIVE)
			.build();
		Performance savedPerformance = performanceRepository.save(performance);

		// Create test performance schedule
		PerformanceSchedule schedule = PerformanceSchedule.builder()
			.performance(savedPerformance)
			.roundNo(1)
			.startAt(java.time.LocalDateTime.now())
			.bookingType(BookingType.FIRST_COME)
			.bookingOpenAt(java.time.LocalDateTime.now().minusDays(1))
			.bookingCloseAt(java.time.LocalDateTime.now().plusDays(1))
			.build();
		PerformanceSchedule savedSchedule = performanceScheduleRepository.save(schedule);

		// Create test section
		section = Section.builder()
			.venueId(venue.getVenueId())
			.sectionName("A")
			.build();
		section = sectionRepository.save(section);

		// Create test seat
		Seat seat = Seat.builder()
			.venueId(venue.getVenueId())
			.sectionId(section.getId())
			.sectionName(section.getSectionName())
			.rowLabel("1")
			.seatNumber(1)
			.build();
		Seat savedSeat = seatRepository.save(seat);

		// Create ScheduleSeat for the test seat
		ScheduleSeat scheduleSeat = ScheduleSeat.builder()
			.scheduleId(savedSchedule.getPerformanceScheduleId())
			.seatId(savedSeat.getId())
			.build();
		ScheduleSeat savedScheduleSeat = scheduleSeatRepository.save(scheduleSeat);

		// Create test reservation
		Reservation reservation = Reservation.builder()
			.scheduleId(savedSchedule.getPerformanceScheduleId())
			.memberId(savedMember.getId())
			.expiresAt(LocalDateTime.now().plusMinutes(5))
			.build();
		Reservation savedReservation = reservationRepository.save(reservation);

		// Create ReservationSeat to link reservation with scheduleSeat
		ReservationSeat reservationSeat = ReservationSeat.builder()
			.reservationId(savedReservation.getId())
			.scheduleSeatId(savedScheduleSeat.getId())
			.build();
		reservationSeatRepository.save(reservationSeat);

		em.flush();
		em.clear();

		rId = savedReservation.getId();
		mId = savedMember.getId();
		sId = savedSeat.getId();
		scheduleId = savedSchedule.getPerformanceScheduleId();

		ticket = ticketService.createTicket(rId, mId, sId);
	}

	/**
	 * Helper method: 테스트용 좌석 생성 및 예약에 연결
	 * @param seatNumber 좌석 번호
	 * @return 생성된 Seat의 ID
	 */
	private Long createAndLinkSeat(int seatNumber) {
		// Create seat
		Seat seat = Seat.builder()
			.venueId(venue.getVenueId())
			.sectionId(section.getId())
			.sectionName(section.getSectionName())
			.rowLabel("1")
			.seatNumber(seatNumber)
			.build();
		Seat savedSeat = seatRepository.save(seat);

		// Create ScheduleSeat
		ScheduleSeat scheduleSeat = ScheduleSeat.builder()
			.scheduleId(scheduleId)
			.seatId(savedSeat.getId())
			.build();
		ScheduleSeat savedScheduleSeat = scheduleSeatRepository.save(scheduleSeat);

		// Link to reservation
		ReservationSeat reservationSeat = ReservationSeat.builder()
			.reservationId(rId)
			.scheduleSeatId(savedScheduleSeat.getId())
			.build();
		reservationSeatRepository.save(reservationSeat);

		return savedSeat.getId();
	}

	@Test
	@DisplayName("티켓생성_성공")
	void createTicket_success() {
		// given
		Ticket ticket = Ticket.builder()
			.reservationId(99L)
			.memberId(99L)
			.seatId(99L)
			.build();

		// when
		Ticket saveTicket = ticketRepository.save(ticket);

		// then
		assertThat(saveTicket.getId()).isNotNull();

		// DB 검증
		Ticket findTicket = ticketRepository.findById(saveTicket.getId()).orElseThrow();
		assertThat(findTicket).isEqualTo(saveTicket);
	}

	@Test
	@DisplayName("티켓취소_성공")
	void cancelTicket_success() {
		// when
		Long rId = ticket.getReservationId();
		Long mId = ticket.getMemberId();
		Long sId = ticket.getSeatId();

		// when
		ticketService.cancelTicket(rId, mId, sId);

		// then
		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELED);

		// DB 검증
		Ticket findTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(findTicket.getStatus()).isEqualTo(TicketStatus.CANCELED);
	}

	@Test
	@DisplayName("티켓취소_실패_취소티켓")
	void cancelTicket_fail_already() {
		// given
		ticketService.cancelTicket(rId, mId, sId);

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> ticketService.cancelTicket(rId, mId, sId));

		// then
		assertThat(e.getErrorCode()).isEqualTo(TicketErrorCode.ALREADY_CANCEL_TICKET);
	}

	@Test
	@DisplayName("티켓취소_실패_없는티켓")
	void cancelTicket_fail_noTicket() {
		// given
		rId = 99L;
		mId = 99L;
		sId = 99L;

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> ticketService.cancelTicket(rId, mId, sId));

		// then
		assertThat(e.getErrorCode()).isEqualTo(TicketErrorCode.TICKET_NOT_FOUND);
	}

	@Test
	@DisplayName("티켓취소_실패_사용티켓")
	void cancelTicket_fail_used() {
		// given
		ticketService.useTicket(rId, mId, sId);

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> ticketService.cancelTicket(rId, mId, sId));

		// then
		assertThat(e.getErrorCode()).isEqualTo(TicketErrorCode.TICKET_NOT_CANCELABLE);
	}

	@Test
	@DisplayName("티켓취소_실패_교환티켓")
	void cancelTicket_fail_exchanged() {
		// given
		ticketService.exchangeTicket(rId, mId, sId);

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> ticketService.cancelTicket(rId, mId, sId));

		// then
		assertThat(e.getErrorCode()).isEqualTo(TicketErrorCode.TICKET_NOT_CANCELABLE);
	}

	@Test
	@DisplayName("티켓취소_실패_양도티켓")
	void cancelTicket_fail_transferred() {
		// given
		ticketService.transferTicket(rId, mId, sId);

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> ticketService.cancelTicket(rId, mId, sId));

		// then
		assertThat(e.getErrorCode()).isEqualTo(TicketErrorCode.TICKET_NOT_CANCELABLE);
	}

	@Test
	@DisplayName("내 티켓 조회: reservationId가 신청예매 bookingId여도 공연 정보를 정상 조회한다")
	void getMyTickets_prereservationBookingId_mappedToSchedule() {
		// given: 신청예매(bookingId를 ticket.reservationId로 쓰던 레거시 데이터) 생성
		PerformanceSchedule prSchedule = PerformanceSchedule.builder()
			.performance(performanceRepository.findAll().getFirst())
			.roundNo(2)
			.startAt(LocalDateTime.now().plusDays(2))
			.bookingType(BookingType.PRERESERVE)
			.bookingOpenAt(LocalDateTime.now().minusDays(1))
			.bookingCloseAt(LocalDateTime.now().plusDays(30))
			.build();
		prSchedule = performanceScheduleRepository.save(prSchedule);

		ScheduleSeat scheduleSeat = scheduleSeatRepository.save(
			ScheduleSeat.builder()
				.scheduleId(prSchedule.getPerformanceScheduleId())
				.seatId(sId)
				.build()
		);

		PrereservationBooking booking = prereservationBookingRepository.save(
			PrereservationBooking.builder()
				.scheduleId(prSchedule.getPerformanceScheduleId())
				.memberId(mId)
				.scheduleSeatId(scheduleSeat.getId())
				.expiresAt(LocalDateTime.now().plusMinutes(10))
				.build()
		);

		ticketRepository.save(
			Ticket.builder()
				.reservationId(booking.getId()) // 레거시: bookingId를 reservationId처럼 저장
				.memberId(mId)
				.seatId(sId)
				.build()
		);

		em.flush();
		em.clear();

		// when
		List<TicketRes> myTickets = ticketService.getMyTickets(mId);

		// then
		TicketRes prTicket = myTickets.stream()
			.filter(t -> t.getReservationId().equals(booking.getId()))
			.findFirst()
			.orElseThrow();

		assertThat(prTicket.getPerformanceId()).isEqualTo(prSchedule.getPerformance().getPerformanceId());
	}

	@Test
	@DisplayName("티켓취소_실패_만료티켓")
	void cancelTicket_fail_expired() {
		// given
		ticketService.expireTicket(rId, mId, sId);

		// when
		BusinessException e = assertThrows(BusinessException.class,
			() -> ticketService.cancelTicket(rId, mId, sId));

		// then
		assertThat(e.getErrorCode()).isEqualTo(TicketErrorCode.TICKET_NOT_CANCELABLE);
	}

	@Test
	@DisplayName("티켓사용_성공")
	void changeTicket_success_used() {
		// when
		Long rId = ticket.getReservationId();
		Long mId = ticket.getMemberId();
		Long sId = ticket.getSeatId();

		// when
		ticketService.useTicket(rId, mId, sId);

		// then
		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);

		// DB 검증
		Ticket findTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(findTicket.getStatus()).isEqualTo(TicketStatus.USED);
	}

	@Test
	@DisplayName("티켓교환_성공")
	void changeTicket_success_exchanged() {
		// when
		Long rId = ticket.getReservationId();
		Long mId = ticket.getMemberId();
		Long sId = ticket.getSeatId();

		// when
		ticketService.exchangeTicket(rId, mId, sId);

		// then
		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.EXCHANGED);

		// DB 검증
		Ticket findTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(findTicket.getStatus()).isEqualTo(TicketStatus.EXCHANGED);
	}

	@Test
	@DisplayName("티켓양도_성공")
	void changeTicket_success_transferred() {
		// when
		Long rId = ticket.getReservationId();
		Long mId = ticket.getMemberId();
		Long sId = ticket.getSeatId();

		// when
		ticketService.transferTicket(rId, mId, sId);

		// then
		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.TRANSFERRED);

		// DB 검증
		Ticket findTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(findTicket.getStatus()).isEqualTo(TicketStatus.TRANSFERRED);
	}

	@Test
	@DisplayName("티켓만료_성공")
	void changeTicket_success_expired() {
		// when
		Long rId = ticket.getReservationId();
		Long mId = ticket.getMemberId();
		Long sId = ticket.getSeatId();

		// when
		ticketService.expireTicket(rId, mId, sId);

		// then
		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.EXPIRED);

		// DB 검증
		Ticket findTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(findTicket.getStatus()).isEqualTo(TicketStatus.EXPIRED);
	}

	@Test
	@DisplayName("내티켓조회_성공")
	void getMyTicket_success() {
		// given - using the member created in setUp
		Long memberId = mId;

		// when
		List<TicketRes> tickets = ticketService.getMyTickets(memberId);

		// then
		assertThat(tickets).isNotNull();
		assertThat(tickets.size()).isGreaterThanOrEqualTo(1);

		// 티켓 정보 검증
		TicketRes firstTicket = tickets.get(0);
		assertThat(firstTicket.getTicketId()).isNotNull();
		assertThat(firstTicket.getReservationId()).isNotNull();
		assertThat(firstTicket.getSeatId()).isNotNull();
		assertThat(firstTicket.getStatus()).isNotNull();
		assertThat(firstTicket.getSectionName()).isNotNull();
		assertThat(firstTicket.getRowLabel()).isNotNull();
		assertThat(firstTicket.getSeatNumber()).isNotNull();
		assertThat(firstTicket.getPerformanceId()).isNotNull();
	}

	@Test
	@DisplayName("내티켓조회_성공_빈목록")
	void getMyTicket_success_empty() {
		// given
		Long memberId = 9999L; // 존재하지 않는 회원 ID

		// when
		List<TicketRes> tickets = ticketService.getMyTickets(memberId);

		// then
		assertThat(tickets).isNotNull();
		assertThat(tickets).isEmpty();
	}

	@Test
	@DisplayName("티켓복구_성공")
	void restoreTicket_success() {
		// given: TRANSFERRED 상태의 티켓
		ticketService.transferTicket(rId, mId, sId);
		em.flush();
		em.clear();

		// when
		Ticket restoredTicket = ticketService.restoreTicket(ticket.getId());

		// then
		assertThat(restoredTicket.getStatus()).isEqualTo(TicketStatus.ISSUED);

		// DB 검증
		Ticket findTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(findTicket.getStatus()).isEqualTo(TicketStatus.ISSUED);
	}

	@Test
	@DisplayName("티켓복구_실패_TRANSFERRED가_아닌_티켓")
	void restoreTicket_fail_notTransferred() {
		// given: ISSUED 상태의 티켓 (TRANSFERRED가 아님)
		em.flush();
		em.clear();

		// when & then
		BusinessException e = assertThrows(BusinessException.class,
			() -> ticketService.restoreTicket(ticket.getId()));

		assertThat(e.getErrorCode()).isEqualTo(TicketErrorCode.TICKET_NOT_TRANSFERABLE);
	}

	@Test
	@DisplayName("티켓생성_멱등성_중복시_기존티켓_반환")
	void createTicket_idempotency_returnExisting() {
		// given: 이미 존재하는 티켓
		Long existingReservationId = ticket.getReservationId();
		Long existingMemberId = ticket.getMemberId();
		Long existingSeatId = ticket.getSeatId();

		// when: 같은 정보로 티켓 재생성 시도
		Ticket result = ticketService.createTicket(existingReservationId, existingMemberId, existingSeatId);

		// then: 새 티켓이 아닌 기존 티켓이 반환됨
		assertThat(result.getId()).isEqualTo(ticket.getId());

		// DB 검증: 중복 티켓이 생성되지 않음
		List<Ticket> allTickets = ticketRepository.findAll();
		long count = allTickets.stream()
			.filter(t -> t.getReservationId().equals(existingReservationId)
				&& t.getMemberId().equals(existingMemberId)
				&& t.getSeatId().equals(existingSeatId))
			.count();
		assertThat(count).isEqualTo(1L);
	}

	@Test
	@DisplayName("내티켓조회_좌석이_없어도_전체_조회가_실패하지_않고_유효한_티켓만_반환")
	void getMyTickets_missingSeat_shouldSkipInvalidTickets() {
		// given
		Ticket invalidTicket = Ticket.builder()
			.reservationId(rId)
			.memberId(mId)
			.seatId(999_999L)
			.build();
		ticketRepository.save(invalidTicket);
		em.flush();
		em.clear();

		// when
		List<TicketRes> tickets = ticketService.getMyTickets(mId);

		// then
		assertThat(tickets).isNotEmpty();
		assertThat(tickets)
			.extracting(TicketRes::getTicketId)
			.contains(ticket.getId())
			.doesNotContain(invalidTicket.getId());
	}

	@Test
	@DisplayName("내티켓조회_예매로받은티켓_RESERVATION")
	void getMyTickets_reservationTicket_shouldReturnReservationType() {
		// given: 예매로 받은 티켓 (기존 setUp에서 생성됨)
		Long memberId = mId;

		// when: 내 티켓 조회
		List<TicketRes> tickets = ticketService.getMyTickets(memberId);

		// then: acquisitionType이 RESERVATION
		assertThat(tickets).isNotEmpty();
		TicketRes ticketRes = tickets.stream()
			.filter(t -> t.getTicketId().equals(ticket.getId()))
			.findFirst()
			.orElseThrow();
		assertThat(ticketRes.getAcquisitionType()).isEqualTo(AcquisitionType.RESERVATION);
	}

	@Test
	@DisplayName("내티켓조회_양도로받은티켓_TRANSFER")
	void getMyTickets_transferTicket_shouldReturnTransferType() {
		// given: 양도 시나리오 설정
		// 1. 판매자(다른 사용자) 생성
		Member seller = Member.builder()
			.email("seller@test.com")
			.password("password")
			.name("Seller")
			.birth(LocalDate.of(1990, 1, 1))
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build();
		Member savedSeller = memberRepository.save(seller);

		// 2. 추가 좌석 생성 및 예약에 연결
		Long transferSeatId = createAndLinkSeat(10);

		// 3. 판매자의 티켓 생성
		Ticket sellerTicket = ticketService.createTicket(rId, savedSeller.getId(), transferSeatId);

		// 4. 양도 거래 생성 및 완료
		Trade trade = Trade.builder()
			.memberId(savedSeller.getId())
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(sellerTicket.getId())
			.type(TradeType.TRANSFER)
			.price(30000)
			.totalCount(1)
			.section("A")
			.row("1")
			.seatNumber("10")
			.build();
		Trade savedTrade = tradeRepository.save(trade);
		savedTrade.completeTransfer(mId, LocalDateTime.now());
		em.flush();

		// 5. 구매자(나)에게 새 티켓 생성
		Ticket myTicket = ticketService.createTicket(rId, mId, transferSeatId);
		em.flush();
		em.clear();

		// when: 내 티켓 조회
		List<TicketRes> tickets = ticketService.getMyTickets(mId);

		// then: 양도로 받은 티켓의 acquisitionType이 TRANSFER
		TicketRes transferredTicket = tickets.stream()
			.filter(t -> t.getTicketId().equals(myTicket.getId()))
			.findFirst()
			.orElseThrow();
		assertThat(transferredTicket.getAcquisitionType()).isEqualTo(AcquisitionType.TRANSFER);
	}

	@Test
	@DisplayName("내티켓조회_교환으로받은티켓_EXCHANGE")
	void getMyTickets_exchangeTicket_shouldReturnExchangeType() {
		// given: 교환 시나리오 설정
		// 1. 교환 상대방 생성
		Member partner = Member.builder()
			.email("partner@test.com")
			.password("password")
			.name("Partner")
			.birth(LocalDate.of(1990, 1, 1))
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build();
		Member savedPartner = memberRepository.save(partner);

		// 2. 추가 좌석 생성 및 예약에 연결
		Long exchangeSeatId = createAndLinkSeat(11);

		// 3. 상대방의 티켓 생성
		Ticket partnerTicket = ticketService.createTicket(rId, savedPartner.getId(), exchangeSeatId);

		// 4. 교환 거래 생성 및 완료
		Trade trade = Trade.builder()
			.memberId(savedPartner.getId())
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(partnerTicket.getId())
			.type(TradeType.EXCHANGE)
			.price(null)
			.totalCount(1)
			.section("A")
			.row("1")
			.seatNumber("11")
			.build();
		Trade savedTrade = tradeRepository.save(trade);
		savedTrade.completeTransfer(mId, LocalDateTime.now());
		em.flush();

		// 5. 나에게 새 티켓 생성 (교환으로 받은 티켓)
		Ticket myExchangedTicket = ticketService.createTicket(rId, mId, exchangeSeatId);
		em.flush();
		em.clear();

		// when: 내 티켓 조회
		List<TicketRes> tickets = ticketService.getMyTickets(mId);

		// then: 교환으로 받은 티켓의 acquisitionType이 EXCHANGE
		TicketRes exchangedTicket = tickets.stream()
			.filter(t -> t.getTicketId().equals(myExchangedTicket.getId()))
			.findFirst()
			.orElseThrow();
		assertThat(exchangedTicket.getAcquisitionType()).isEqualTo(AcquisitionType.EXCHANGE);
	}

	@Test
	@DisplayName("내티켓조회_혼합티켓_각각올바른타입반환")
	void getMyTickets_mixedTickets_shouldReturnCorrectTypes() {
		// given: 예매 + 양도 + 교환 티켓이 모두 있는 상황
		// 1. 예매 티켓 (기존)
		Ticket reservationTicket = ticket;

		// 2. 양도 티켓 설정
		Member seller = memberRepository.save(Member.builder()
			.email("seller2@test.com")
			.password("password")
			.name("Seller2")
			.birth(LocalDate.of(1990, 1, 1))
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build());

		Long transferSeatId = createAndLinkSeat(5);
		Ticket sellerTicket = ticketService.createTicket(rId, seller.getId(), transferSeatId);
		Trade transferTrade = tradeRepository.save(Trade.builder()
			.memberId(seller.getId())
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(sellerTicket.getId())
			.type(TradeType.TRANSFER)
			.price(30000)
			.totalCount(1)
			.section("A")
			.row("2")
			.seatNumber("5")
			.build());
		transferTrade.completeTransfer(mId, LocalDateTime.now());

		Ticket transferredTicket = ticketService.createTicket(rId, mId, transferSeatId);

		// 3. 교환 티켓 설정
		Member partner = memberRepository.save(Member.builder()
			.email("partner2@test.com")
			.password("password")
			.name("Partner2")
			.birth(LocalDate.of(1990, 1, 1))
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build());

		Long exchangeSeatId = createAndLinkSeat(8);
		Ticket partnerTicket = ticketService.createTicket(rId, partner.getId(), exchangeSeatId);
		Trade exchangeTrade = tradeRepository.save(Trade.builder()
			.memberId(partner.getId())
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(partnerTicket.getId())
			.type(TradeType.EXCHANGE)
			.price(null)
			.totalCount(1)
			.section("A")
			.row("3")
			.seatNumber("8")
			.build());
		exchangeTrade.completeTransfer(mId, LocalDateTime.now());

		Ticket exchangedTicket = ticketService.createTicket(rId, mId, exchangeSeatId);

		em.flush();
		em.clear();

		// when: 내 티켓 조회
		List<TicketRes> tickets = ticketService.getMyTickets(mId);

		// then: 각 티켓의 타입이 올바름
		assertThat(tickets).hasSize(3);

		TicketRes reservation = tickets.stream()
			.filter(t -> t.getTicketId().equals(reservationTicket.getId()))
			.findFirst().orElseThrow();
		assertThat(reservation.getAcquisitionType()).isEqualTo(AcquisitionType.RESERVATION);

		TicketRes transfer = tickets.stream()
			.filter(t -> t.getTicketId().equals(transferredTicket.getId()))
			.findFirst().orElseThrow();
		assertThat(transfer.getAcquisitionType()).isEqualTo(AcquisitionType.TRANSFER);

		TicketRes exchange = tickets.stream()
			.filter(t -> t.getTicketId().equals(exchangedTicket.getId()))
			.findFirst().orElseThrow();
		assertThat(exchange.getAcquisitionType()).isEqualTo(AcquisitionType.EXCHANGE);
	}
}
