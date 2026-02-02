package com.back.b2st.domain.lottery.draw.service;

import static com.back.b2st.support.TestFixture.*;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.lottery.entry.entity.LotteryEntry;
import com.back.b2st.domain.lottery.entry.entity.LotteryStatus;
import com.back.b2st.domain.lottery.entry.repository.LotteryEntryRepository;
import com.back.b2st.domain.lottery.result.dto.LotteryReservationInfo;
import com.back.b2st.domain.lottery.result.entity.LotteryResult;
import com.back.b2st.domain.lottery.result.repository.LotteryResultRepository;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentMethod;
import com.back.b2st.domain.payment.repository.PaymentRepository;
import com.back.b2st.domain.payment.service.PaymentFinalizeService;
import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationSeat;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.reservation.repository.ReservationSeatRepository;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.seat.grade.entity.SeatGrade;
import com.back.b2st.domain.seat.grade.entity.SeatGradeType;
import com.back.b2st.domain.seat.grade.repository.SeatGradeRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.repository.TicketRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.domain.venue.venue.repository.VenueRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SeatAllocationServiceTest {

	@Autowired
	private SeatAllocationService seatAllocationService;
	@Autowired
	private DrawService drawService;
	@Autowired
	private PaymentFinalizeService paymentFinalizeService;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private VenueRepository venueRepository;
	@Autowired
	private PerformanceRepository performanceRepository;
	@Autowired
	private PerformanceScheduleRepository performanceScheduleRepository;
	@Autowired
	private SectionRepository sectionRepository;
	@Autowired
	private SeatRepository seatRepository;
	@Autowired
	private SeatGradeRepository seatGradeRepository;
	@Autowired
	private LotteryEntryRepository lotteryEntryRepository;
	@Autowired
	private LotteryResultRepository lotteryResultRepository;
	@Autowired
	private PaymentRepository paymentRepository;
	@Autowired
	private ScheduleSeatRepository scheduleSeatRepository;
	@Autowired
	private ReservationRepository reservationRepository;
	@Autowired
	private PerformanceDrawService performanceDrawService;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private TicketRepository ticketRepository;
	@Autowired
	private ReservationSeatRepository reservationSeatRepository;

	List<Member> members;
	Venue venue;
	Performance performance;
	List<PerformanceSchedule> schedules;
	List<Section> sections;
	List<Seat> seats;
	PerformanceSchedule schedule;

	@BeforeEach
	void setUp() {
		members = createMembers(10, memberRepository, passwordEncoder);

		venue = createVenue("잠실실내체육관", venueRepository);
		performance = createPerformance(venue, performanceRepository);
		schedules = createSchedules(performance, 1, BookingType.LOTTERY, performanceScheduleRepository);
		schedule = schedules.getFirst();

		sections = createSections(venue.getVenueId(), sectionRepository, "A", "B", "C");
		seats = createSeats(venue.getVenueId(), sections, 3, 5, seatRepository);
		createSeatGrades(performance, seats, SeatGradeType.STANDARD, 10000, seatGradeRepository);
		createSeatGrades(performance, seats, SeatGradeType.VIP, 30000, seatGradeRepository);
		createScheduleSeats(schedule.getPerformanceScheduleId(), seats, scheduleSeatRepository);

		System.out.println("============================== init data OK ==============================");
	}

	/**
	 * Payment 엔티티를 사용한 완전한 결제 프로세스
	 * 1. 추첨 실행 (당첨자 선정)
	 * 2. Reservation 생성
	 * 3. Payment 생성 및 완료 처리
	 * 4. LotteryPaymentFinalizer를 통한 결제 확정 (LotteryResult.confirmPayment())
	 */
	private void executeDrawAndPayWithPaymentEntity() {
		// 1. 추첨 실행
		entityManager.flush();
		drawService.executeDraws();

		entityManager.flush();
		entityManager.clear();

		// 2. 당첨된 LotteryResult 조회
		List<LotteryResult> winningResults = lotteryResultRepository.findAll().stream()
			.filter(result -> !result.isPaid()) // 아직 미결제 상태
			.toList();

		System.out.println("=== 당첨자 수: " + winningResults.size() + " ===");

		assertThat(winningResults)
			.as("추첨 실행 후 당첨자가 존재해야 함")
			.isNotEmpty();

		// 3. 각 당첨자에 대해 Payment 생성 및 결제 처리
		for (LotteryResult result : winningResults) {
			LotteryEntry entry = lotteryEntryRepository.findById(result.getLotteryEntryId()).orElseThrow();

			// 예매 생성
			Reservation reservation = Reservation.builder()
				.memberId(result.getMemberId())
				.scheduleId(entry.getScheduleId())
				.expiresAt(LocalDateTime.now().plusMinutes(5))
				.build();
			reservationRepository.save(reservation);

			// Payment 생성
			String orderId = "ORDER-LOTTERY-" + result.getId();
			Payment payment = Payment.builder()
				.orderId(orderId)
				.memberId(result.getMemberId())
				.domainType(DomainType.LOTTERY)
				.domainId(result.getId()) // LotteryResult ID 사용
				.amount(calculateAmount(entry))
				.method(PaymentMethod.CARD)
				.expiresAt(LocalDateTime.now().plusHours(1))
				.build();

			paymentRepository.save(payment);

			// 결제 완료 처리
			payment.complete(LocalDateTime.now());
			paymentRepository.save(payment);

			System.out.println("결제 완료: Order ID=" + orderId
				+ ", Status=" + payment.getStatus()
				+ ", Amount=" + payment.getAmount());

			// 4. LotteryPaymentFinalizer를 통한 결제 확정
			// → LotteryResult.confirmPayment() 호출됨
			paymentFinalizeService.finalizeByOrderId(orderId);

			System.out.println("결제 확정 완료: Result ID=" + result.getId());
		}

		entityManager.flush();
		entityManager.clear();

		System.out.println("=== 전체 결제 프로세스 완료 ===");

		// 검증: 모든 LotteryResult가 paid 상태인지 확인
		List<LotteryResult> paidResults = lotteryResultRepository.findAll().stream()
			.filter(LotteryResult::isPaid)
			.toList();

		assertThat(paidResults).hasSize(winningResults.size());
		System.out.println("=== 결제 확정 검증 완료: " + paidResults.size() + "건 ===");
	}

	/**
	 * 추첨 신청 금액 계산
	 */
	private Long calculateAmount(LotteryEntry entry) {
		SeatGrade seatGrade = seatGradeRepository.findAll().stream()
			.filter(sg -> sg.getPerformanceId().equals(entry.getPerformanceId()))
			.filter(sg -> sg.getGrade() == entry.getGrade())
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("좌석 등급을 찾을 수 없습니다."));

		return (long)entry.getQuantity() * seatGrade.getPrice();
	}

	@Test
	@DisplayName("좌석 배정 성공")
	void allocateSeats_Success() {
		// given
		createLotteryEntry(members.subList(0, 1), performance, schedule,
			SeatGradeType.STANDARD, 3, lotteryEntryRepository);
		executeDrawAndPayWithPaymentEntity();

		List<LotteryReservationInfo> infos = seatAllocationService.findReservationInfos(
			schedule.getPerformanceScheduleId());
		assertThat(infos).hasSize(1);

		LotteryReservationInfo info = infos.get(0);

		// when
		List<ScheduleSeat> allocatedSeats = seatAllocationService.allocateSeatsForLottery(info);
		entityManager.flush();
		entityManager.clear();

		// then - 좌석 배정 검증
		assertThat(allocatedSeats).hasSize(3);

		// DB 검증 - SOLD 상태 확인
		List<ScheduleSeat> seatsFromDB = scheduleSeatRepository.findAll().stream()
			.filter(seat -> seat.getStatus() == SeatStatus.SOLD)
			.toList();

		assertThat(seatsFromDB).hasSize(3);
		assertThat(seatsFromDB).allMatch(seat ->
			seat.getScheduleId().equals(info.scheduleId()));

		System.out.println("=== 배정된 좌석 ===");
		seatsFromDB.forEach(seat ->
			System.out.println("SeatId: " + seat.getSeatId() + ", Status: " + seat.getStatus()));

		// 티켓 생성 검증
		List<Ticket> ticketsFromDB = ticketRepository.findByReservationId(info.reservationId());

		assertThat(ticketsFromDB)
			.as("예약 ID로 생성된 티켓이 존재해야 함")
			.hasSize(3);

		assertThat(ticketsFromDB)
			.allMatch(ticket -> ticket.getMemberId().equals(info.memberId()))
			.allMatch(ticket -> ticket.getReservationId().equals(info.reservationId()));

		// 티켓의 좌석 ID가 배정된 좌석과 일치하는지 확인
		List<Long> ticketSeatIds = ticketsFromDB.stream()
			.map(Ticket::getSeatId)
			.toList();

		List<Long> allocatedSeatIds = allocatedSeats.stream()
			.map(ScheduleSeat::getSeatId)
			.toList();

		assertThat(ticketSeatIds)
			.as("티켓의 좌석 ID가 배정된 좌석 ID와 일치해야 함")
			.containsExactlyInAnyOrderElementsOf(allocatedSeatIds);

		System.out.println("=== 생성된 티켓 ===");
		ticketsFromDB.forEach(ticket ->
			System.out.println("Ticket ID: " + ticket.getId()
				+ ", Seat ID: " + ticket.getSeatId()
				+ ", Member ID: " + ticket.getMemberId())
		);
	}

	@Test
	@DisplayName("좌석 배정 - 여러 사용자 티켓 생성 검증")
	void allocateSeatsForLottery_MultipleUsers_TicketCreation() {
		// given - 3명에게 각 2장씩
		createLotteryEntry(members.subList(0, 3), performance, schedule,
			SeatGradeType.STANDARD, 2, lotteryEntryRepository);
		executeDrawAndPayWithPaymentEntity();

		List<LotteryReservationInfo> infos = seatAllocationService.findReservationInfos(
			schedule.getPerformanceScheduleId());
		assertThat(infos).hasSize(3);

		// when - 각 사용자에게 좌석 배정
		infos.forEach(info -> seatAllocationService.allocateSeatsForLottery(info));

		entityManager.flush();
		entityManager.clear();

		// then - 전체 티켓 수 검증
		List<Ticket> allTicketsFromDB = ticketRepository.findAll();
		assertThat(allTicketsFromDB).hasSize(6); // 3명 × 2장

		// 각 사용자별 티켓 검증
		for (LotteryReservationInfo info : infos) {
			List<Ticket> userTickets = ticketRepository.findByReservationId(info.reservationId());

			assertThat(userTickets)
				.as("사용자별 티켓이 정확히 생성되어야 함")
				.hasSize(2)
				.allMatch(ticket -> ticket.getMemberId().equals(info.memberId()))
				.allMatch(ticket -> ticket.getReservationId().equals(info.reservationId()));

			System.out.println("=== Member ID: " + info.memberId() + " 티켓 ===");
			userTickets.forEach(ticket ->
				System.out.println("  Ticket ID: " + ticket.getId()
					+ ", Seat ID: " + ticket.getSeatId())
			);
		}

		// 티켓의 좌석 ID 중복 검증
		List<Long> allTicketSeatIds = allTicketsFromDB.stream()
			.map(Ticket::getSeatId)
			.toList();

		assertThat(allTicketSeatIds)
			.as("모든 티켓의 좌석 ID는 중복되지 않아야 함")
			.doesNotHaveDuplicates();
	}

	@Test
	@DisplayName("좌석 배정 - 티켓과 좌석 상태 일관성 검증")
	void allocateSeatsForLottery_TicketAndSeatConsistency() {
		// given
		createLotteryEntry(members.subList(0, 1), performance, schedule,
			SeatGradeType.STANDARD, 3, lotteryEntryRepository);
		executeDrawAndPayWithPaymentEntity();

		List<LotteryReservationInfo> infos = seatAllocationService.findReservationInfos(
			schedule.getPerformanceScheduleId());
		LotteryReservationInfo info = infos.get(0);

		// when
		seatAllocationService.allocateSeatsForLottery(info);
		entityManager.flush();
		entityManager.clear();

		// then - DB에서 SOLD 좌석 조회
		List<ScheduleSeat> soldSeatsFromDB = scheduleSeatRepository.findAll().stream()
			.filter(seat -> seat.getStatus() == SeatStatus.SOLD)
			.toList();

		// DB에서 생성된 티켓 조회
		List<Ticket> ticketsFromDB = ticketRepository.findByReservationId(info.reservationId());

		// 수량 일치 검증
		assertThat(soldSeatsFromDB).hasSize(3);
		assertThat(ticketsFromDB).hasSize(3);

		// 티켓의 좌석 ID가 모두 SOLD 상태인지 확인
		List<Long> soldSeatIds = soldSeatsFromDB.stream()
			.map(ScheduleSeat::getSeatId)
			.toList();

		List<Long> ticketSeatIds = ticketsFromDB.stream()
			.map(Ticket::getSeatId)
			.toList();

		assertThat(ticketSeatIds)
			.as("티켓의 모든 좌석이 SOLD 상태여야 함")
			.allMatch(soldSeatIds::contains);

		// ReservationSeat 매핑도 존재하는지 확인
		List<ReservationSeat> reservationSeats = reservationSeatRepository
			.findByReservationId(info.reservationId());

		assertThat(reservationSeats)
			.as("예약-좌석 매핑이 생성되어야 함")
			.hasSize(3);

		System.out.println("=== 일관성 검증 완료 ===");
		System.out.println("SOLD 좌석 수: " + soldSeatsFromDB.size());
		System.out.println("생성된 티켓 수: " + ticketsFromDB.size());
		System.out.println("예약-좌석 매핑 수: " + reservationSeats.size());
	}

	@Test
	@DisplayName("좌석 배정 실패 - 좌석 부족")
	void allocateSeatsForLottery_InsufficientSeats1() {
		// given - 당첨은 되도록 적게 신청
		createLotteryEntry(members.subList(0, 1), performance, schedule,
			SeatGradeType.STANDARD, 3, lotteryEntryRepository);
		executeDrawAndPayWithPaymentEntity();

		List<LotteryReservationInfo> infos = seatAllocationService.findReservationInfos(
			schedule.getPerformanceScheduleId());
		assertThat(infos).isNotEmpty();
		LotteryReservationInfo info = infos.get(0);

		// 좌석을 모두 SOLD로 만들어서 배정 불가능하게 만들기
		List<ScheduleSeat> allSeats = scheduleSeatRepository.findAll();
		allSeats.forEach(ScheduleSeat::sold);
		scheduleSeatRepository.saveAll(allSeats);
		entityManager.flush();
		entityManager.clear();

		// when & then
		assertThatThrownBy(() -> seatAllocationService.allocateSeatsForLottery(info))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("좌석 부족");

		// DB 상태 확인 - 모든 좌석이 SOLD 상태여야 함
		List<ScheduleSeat> allSeatsFromDB = scheduleSeatRepository.findAll();
		long soldCount = allSeatsFromDB.stream()
			.filter(seat -> seat.getStatus() == SeatStatus.SOLD)
			.count();

		assertThat(soldCount).isEqualTo(allSeats.size());
	}

	@Test
	@DisplayName("좌석 배정 - 정확한 수량만 배정")
	void allocateSeatsForLottery_ExactQuantity() {
		// given
		createLotteryEntry(members.subList(0, 1), performance, schedule,
			SeatGradeType.STANDARD, 3, lotteryEntryRepository);
		executeDrawAndPayWithPaymentEntity();

		entityManager.clear();
		int availableBefore = scheduleSeatRepository
			.findAvailableSeatsByGrade(schedule.getPerformanceScheduleId(), SeatGradeType.STANDARD)
			.size();

		List<LotteryReservationInfo> infos = seatAllocationService.findReservationInfos(
			schedule.getPerformanceScheduleId());
		LotteryReservationInfo info = infos.get(0);

		// when
		List<ScheduleSeat> allocatedSeats = seatAllocationService.allocateSeatsForLottery(info);
		entityManager.flush();
		entityManager.clear();

		// then - DB에서 재조회하여 검증
		int availableAfter = scheduleSeatRepository
			.findAvailableSeatsByGrade(schedule.getPerformanceScheduleId(), SeatGradeType.STANDARD)
			.size();

		assertThat(allocatedSeats).hasSize(3);
		assertThat(availableBefore - availableAfter).isEqualTo(3);

		// SOLD 상태 개수 확인
		List<ScheduleSeat> allSeatsFromDB = scheduleSeatRepository.findAll();
		long soldCount = allSeatsFromDB.stream()
			.filter(seat -> seat.getStatus() == SeatStatus.SOLD)
			.count();

		assertThat(soldCount).isEqualTo(3);
	}

	@Test
	@DisplayName("좌석 배정 - 중복 없음 (여러 사용자)")
	void allocateSeatsForLottery_NoDuplicates() {
		// given - 3명에게 각 2장씩
		createLotteryEntry(members.subList(0, 3), performance, schedule,
			SeatGradeType.STANDARD, 2, lotteryEntryRepository);
		executeDrawAndPayWithPaymentEntity();

		List<LotteryReservationInfo> infos = seatAllocationService.findReservationInfos(
			schedule.getPerformanceScheduleId());
		assertThat(infos).hasSize(3);

		// when - 각 사용자에게 좌석 배정
		List<ScheduleSeat> allAllocated = infos.stream()
			.flatMap(info -> seatAllocationService.allocateSeatsForLottery(info).stream())
			.toList();

		entityManager.flush();
		entityManager.clear();

		// then - DB에서 SOLD 상태인 좌석 조회
		List<ScheduleSeat> soldSeatsFromDB = scheduleSeatRepository.findAll().stream()
			.filter(seat -> seat.getStatus() == SeatStatus.SOLD)
			.toList();

		List<Long> soldSeatIdsFromDB = soldSeatsFromDB.stream()
			.map(ScheduleSeat::getSeatId)
			.toList();

		List<Long> allocatedSeatIds = allAllocated.stream()
			.map(ScheduleSeat::getSeatId)
			.toList();

		assertThat(soldSeatIdsFromDB).hasSize(6); // 3명 × 2장
		assertThat(soldSeatIdsFromDB).doesNotHaveDuplicates();
		assertThat(soldSeatIdsFromDB).containsExactlyInAnyOrderElementsOf(allocatedSeatIds);
	}

	@Test
	@DisplayName("좌석 배정 실패 - 해당 등급 좌석 없음")
	void allocateSeatsForLottery_NoSeatsForGrade() {
		// given
		createLotteryEntry(members.subList(0, 1), performance, schedule,
			SeatGradeType.ROYAL, 2, lotteryEntryRepository);
		entityManager.flush();
		entityManager.clear();

		drawService.executeDraws();

		entityManager.flush();
		entityManager.clear();

		// when & then - DB에서 검증
		List<LotteryReservationInfo> infos = seatAllocationService.findReservationInfos(
			schedule.getPerformanceScheduleId());
		assertThat(infos).isEmpty();

		// DB에서 당첨자 확인
		List<LotteryEntry> winEntriesFromDB = lotteryEntryRepository.findAll().stream()
			.filter(entry -> entry.getStatus() == LotteryStatus.WIN)
			.toList();

		assertThat(winEntriesFromDB).isEmpty();
	}

	@Test
	@DisplayName("좌석 배정 - AVAILABLE 좌석만 선택")
	void allocateSeatsForLottery_OnlyAvailableSeats() {
		// given - 일부 좌석을 미리 SOLD 처리
		List<ScheduleSeat> allSeats = scheduleSeatRepository.findAll();
		List<Long> preSoldSeatIds = allSeats.stream()
			.limit(5)
			.peek(ScheduleSeat::sold)
			.map(ScheduleSeat::getSeatId)
			.toList();

		scheduleSeatRepository.saveAll(allSeats);
		entityManager.flush();
		entityManager.clear();

		// DB에서 SOLD 상태 확인
		long soldCountBefore = scheduleSeatRepository.findAll().stream()
			.filter(seat -> seat.getStatus() == SeatStatus.SOLD)
			.count();
		assertThat(soldCountBefore).isEqualTo(5);

		createLotteryEntry(members.subList(0, 1), performance, schedule,
			SeatGradeType.STANDARD, 3, lotteryEntryRepository);
		// executeDrawAndPay();
		executeDrawAndPayWithPaymentEntity();

		List<LotteryReservationInfo> infos = seatAllocationService.findReservationInfos(
			schedule.getPerformanceScheduleId());
		LotteryReservationInfo info = infos.get(0);

		// when
		List<ScheduleSeat> allocatedSeats = seatAllocationService.allocateSeatsForLottery(info);
		entityManager.flush();
		entityManager.clear();

		// then - DB에서 검증
		assertThat(allocatedSeats).hasSize(3);

		// DB에서 모든 SOLD 좌석 조회
		List<ScheduleSeat> allSoldSeatsFromDB = scheduleSeatRepository.findAll().stream()
			.filter(seat -> seat.getStatus() == SeatStatus.SOLD)
			.toList();

		assertThat(allSoldSeatsFromDB).hasSize(8); // 5(기존) + 3(새로 배정)

		// 새로 배정된 좌석이 기존 SOLD 좌석이 아닌지 확인
		List<Long> newAllocatedSeatIds = allocatedSeats.stream()
			.map(ScheduleSeat::getSeatId)
			.toList();

		assertThat(newAllocatedSeatIds).doesNotContainAnyElementsOf(preSoldSeatIds);
	}

	@Test
	@DisplayName("좌석 배정 성공 - 배치")
	void allocateSeats_Success_execute() {
		// given
		// schedules = createSchedulesSeatAllocation(performance, 1, performanceScheduleRepository);
		schedule = schedules.getFirst();

		// createScheduleSeats(schedule.getPerformanceScheduleId(), seats, scheduleSeatRepository);

		createLotteryEntry(members.subList(0, 1), performance, schedule,
			SeatGradeType.STANDARD, 3, lotteryEntryRepository);

		executeDrawAndPayWithPaymentEntity();

		performanceScheduleRepository.findAll().forEach(ps -> {
			System.out.println(
				"scheduleId=" + ps.getPerformanceScheduleId()
					+ ", startAt=" + ps.getStartAt()
					+ ", seatAllocated=" + ps.isSeatAllocated()
			);
		});

		// when
		drawService.executeAllocation();
		entityManager.flush();
		entityManager.clear();

		// DB 검증 - SOLD 상태 확인
		List<ScheduleSeat> seatsFromDB = scheduleSeatRepository.findAll().stream()
			.filter(seat -> seat.getScheduleId().equals(schedule.getPerformanceScheduleId()))
			.filter(seat -> seat.getStatus() == SeatStatus.SOLD)
			.toList();

		assertThat(seatsFromDB).hasSize(3);
		assertThat(seatsFromDB).allMatch(seat ->
			seat.getScheduleId().equals(schedule.getPerformanceScheduleId()));

		// then - 중복 실행 방지 검증 (다시 AVAILABLE 없어야 함)
		List<ScheduleSeat> soldSeats = scheduleSeatRepository.findAll().stream()
			.filter(seat -> seat.getScheduleId().equals(schedule.getPerformanceScheduleId()))
			.filter(seat -> seat.getStatus() == SeatStatus.SOLD)
			.toList();

		assertThat(soldSeats).hasSize(3);

		System.out.println("=== 배정된 좌석 ===");
		seatsFromDB.forEach(seat ->
			System.out.println("SeatId: " + seat.getSeatId() + ", Status: " + seat.getStatus()));
	}

	@Test
	@DisplayName("좌석 배정 실패 - 티켓 미생성 확인")
	void allocateSeatsForLottery_InsufficientSeats_NoTicketCreation() {
		// given - 좌석 부족 상황
		createLotteryEntry(members.subList(0, 1), performance, schedule,
			SeatGradeType.STANDARD, 3, lotteryEntryRepository);
		executeDrawAndPayWithPaymentEntity();

		List<LotteryReservationInfo> infos = seatAllocationService.findReservationInfos(
			schedule.getPerformanceScheduleId());
		LotteryReservationInfo info = infos.get(0);

		// 모든 좌석을 SOLD로 만들기
		List<ScheduleSeat> allSeats = scheduleSeatRepository.findAll();
		allSeats.forEach(ScheduleSeat::sold);
		scheduleSeatRepository.saveAll(allSeats);
		entityManager.flush();
		entityManager.clear();

		// when & then - 배정 실패 확인
		assertThatThrownBy(() -> seatAllocationService.allocateSeatsForLottery(info))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("좌석 부족");

		entityManager.flush();
		entityManager.clear();

		// 티켓이 생성되지 않았는지 확인
		List<Ticket> ticketsFromDB = ticketRepository.findByReservationId(info.reservationId());

		assertThat(ticketsFromDB)
			.as("좌석 배정 실패 시 티켓이 생성되지 않아야 함")
			.isEmpty();

		// ReservationSeat 매핑도 없어야 함
		List<ReservationSeat> reservationSeats = reservationSeatRepository
			.findByReservationId(info.reservationId());

		assertThat(reservationSeats)
			.as("좌석 배정 실패 시 예약-좌석 매핑이 생성되지 않아야 함")
			.isEmpty();

		System.out.println("=== 배정 실패 시 티켓 미생성 검증 완료 ===");
	}

	@Test
	@DisplayName("좌석 배정 - 배치 실행 후 티켓 생성 검증")
	void allocateSeats_BatchExecution_TicketCreation() {
		// given
		// schedules = createSchedulesSeatAllocation(performance, 1, performanceScheduleRepository);
		schedule = schedules.getFirst();

		// createScheduleSeats(schedule.getPerformanceScheduleId(), seats, scheduleSeatRepository);

		createLotteryEntry(members.subList(0, 1), performance, schedule,
			SeatGradeType.STANDARD, 3, lotteryEntryRepository);

		executeDrawAndPayWithPaymentEntity();

		List<LotteryReservationInfo> infos = seatAllocationService.findReservationInfos(
			schedule.getPerformanceScheduleId());
		assertThat(infos).hasSize(1);
		LotteryReservationInfo info = infos.get(0);

		// when - 배치 실행
		drawService.executeAllocation();
		entityManager.flush();
		entityManager.clear();

		// then - 좌석 상태 검증
		List<ScheduleSeat> soldSeatsFromDB = scheduleSeatRepository.findAll().stream()
			.filter(seat -> seat.getScheduleId().equals(schedule.getPerformanceScheduleId()))
			.filter(seat -> seat.getStatus() == SeatStatus.SOLD)
			.toList();

		assertThat(soldSeatsFromDB).hasSize(3);

		// 티켓 생성 검증
		List<Ticket> ticketsFromDB = ticketRepository.findByReservationId(info.reservationId());

		assertThat(ticketsFromDB)
			.as("배치 실행 후 티켓이 생성되어야 함")
			.hasSize(3)
			.allMatch(ticket -> ticket.getMemberId().equals(info.memberId()))
			.allMatch(ticket -> ticket.getReservationId().equals(info.reservationId()));

		// 티켓의 좌석 ID가 SOLD 좌석과 일치하는지 확인
		List<Long> ticketSeatIds = ticketsFromDB.stream()
			.map(Ticket::getSeatId)
			.toList();

		List<Long> soldSeatIds = soldSeatsFromDB.stream()
			.map(ScheduleSeat::getSeatId)
			.toList();

		assertThat(ticketSeatIds)
			.containsExactlyInAnyOrderElementsOf(soldSeatIds);

		System.out.println("=== 배치 실행 후 티켓 생성 검증 완료 ===");
		ticketsFromDB.forEach(ticket ->
			System.out.println("Ticket ID: " + ticket.getId()
				+ ", Seat ID: " + ticket.getSeatId())
		);
	}
}
