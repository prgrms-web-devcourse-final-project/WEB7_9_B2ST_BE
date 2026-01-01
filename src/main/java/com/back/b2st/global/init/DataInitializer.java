package com.back.b2st.global.init;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.lottery.entry.entity.LotteryEntry;
import com.back.b2st.domain.lottery.entry.repository.LotteryEntryRepository;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentMethod;
import com.back.b2st.domain.payment.repository.PaymentRepository;
import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.entity.PerformanceStatus;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.prereservation.policy.entity.PrereservationTimeTable;
import com.back.b2st.domain.prereservation.policy.repository.PrereservationTimeTableRepository;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationSeat;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.reservation.repository.ReservationSeatRepository;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
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
import com.back.b2st.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
@Transactional
public class DataInitializer implements CommandLineRunner {

	private static final String TEST_PERFORMANCE_TITLE = "2024 아이유 콘서트 - HEREH WORLD TOUR";

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final ScheduleSeatRepository scheduleSeatRepository;
	private final SectionRepository sectionRepository;
	private final SeatRepository seatRepository;
	private final VenueRepository venueRepository;
	private final PerformanceRepository performanceRepository;
	private final PerformanceScheduleRepository performanceScheduleRepository;
	private final SeatGradeRepository seatGradeRepository;
	private final ReservationRepository reservationRepository;
	private final ReservationSeatRepository reservationSeatRepository;
	private final PaymentRepository paymentRepository;
	private final TicketRepository ticketRepository;
	private final LotteryEntryRepository lotteryEntryRepository;
	private final PrereservationTimeTableRepository prereservationTimeTableRepository;

	@Override
	public void run(String... args) throws Exception {
		// 서버 재시작시 중복 생성 방지 차
		initMemberData();
		initConnectedSet();
		lottery();
	}

	private void initMemberData() {
		if (memberRepository.count() > 0) {
			log.info("[DataInit] 이미 계정 존재하여 초기화 스킵");
			return;
		}

		log.info("[DataInit] 테스트 계정 데이터 생성");

		Member admin = Member.builder()
			.email("admin@tt.com")
			.password(passwordEncoder.encode("1234567a!")) // 어드민, 유저 비번 전부 1234567a!입니다
			.name("관리자")
			.role(Member.Role.ADMIN)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build();

		Member savedAdmin = memberRepository.save(admin);

		log.info("[DataInit] 관리자 계정 생성 완");
		log.info("   - 관리자: admin@tt.com / 1234");

		// SecurityContext에 admin 설정 (초기화용)
		setAuthenticationContext(savedAdmin);

		Member user1 = Member.builder()
			.email("user1@tt.com")
			.password(passwordEncoder.encode("1234567a!"))
			.name("유저일")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build();

		Member user2 = Member.builder()
			.email("user2@tt.com")
			.password(passwordEncoder.encode("1234567a!"))
			.name("유저이")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build();

		Member user3 = Member.builder()
			.email("codeisneverodd@gmail.com")
			.password(passwordEncoder.encode("1234567a!"))
			.name("유저삼")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build();

		memberRepository.save(user1);
		memberRepository.save(user2);
		memberRepository.save(user3);

		log.info("[DataInit] 계정 생성 완");
		log.info("   - 유저1 : user1@tt.com / 1234567a!");
		log.info("   - 유저2 : user2@tt.com / 1234567a!");
		log.info("   - 유저3 : codeisneverodd@gmail.com / 1234567a!");
	}

	private void setAuthenticationContext(Member admin) {
		CustomUserDetails userDetails = new CustomUserDetails(admin);
		Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	// 데이터 생성 1
	private void initConnectedSet() {
		// 중복 생성 방지: 이미 공연장이 있으면 스킵
		if (venueRepository.count() > 0) {
			log.info("[DataInit] 이미 데이터 존재하여 초기화 스킵");
			seedPrereservationTimeTablesIfMissing();
			return;
		}

		Venue venue;
		Performance performance;
		PerformanceSchedule performanceSchedule;
		PerformanceSchedule performanceSchedule2;

		Section sectionA;
		Section sectionB;
		Section sectionC;

		// 공연장 생성
		venue = venueRepository.save(Venue.builder().name("잠실실내체육관").build());

		// 공연 정보 생성
		performance = performanceRepository.save(Performance.builder()
			.venue(venue)
			.title("2024 아이유 콘서트 - HEREH WORLD TOUR")
			.category("콘서트")
			.posterUrl("")
			.description(null)
			.startDate(LocalDateTime.of(2024, 12, 20, 19, 0))
			.endDate(LocalDateTime.of(2024, 12, 29, 21, 0))
			.status(PerformanceStatus.ON_SALE)
			.build());

		Long venueId = venue.getVenueId();

		// 회차 23개 추가 생성
		List<PerformanceSchedule> schedules = IntStream.rangeClosed(0, 23)
			.mapToObj(i -> PerformanceSchedule.builder()
				.performance(performance)
				.startAt(LocalDateTime.of(2025, 1, 1, 19, 0).plusDays(i))
				.roundNo(i + 1)
				.bookingType(i % 2 == 0 ? BookingType.PRERESERVE : BookingType.LOTTERY)
				.bookingOpenAt(LocalDateTime.of(2024, 12, 20, 12, 0))
				.bookingCloseAt(LocalDateTime.of(2024, 12, 25, 23, 59))
				.build()
			).toList();
		performanceScheduleRepository.saveAll(schedules);
		performanceSchedule = schedules.getFirst();
		performanceSchedule2 = schedules.get(1);

		// 구역 생성
		sectionA = sectionRepository.save(Section.builder().venueId(venueId).sectionName("A").build());
		sectionB = sectionRepository.save(Section.builder().venueId(venueId).sectionName("B").build());
		sectionC = sectionRepository.save(Section.builder().venueId(venueId).sectionName("C").build());
		log.info("[DataInit/Test] Section initialized. count=3 (venueId=1[A,B,C])");

		// 구역 정보 정의 (구역, 행 수, 등급, 가격, 회차)
		record SectionConfig(Section section) {
		}

		// 구역 설정 리스트
		List<Section> sections = List.of(sectionA, sectionB, sectionC);

		// 신청 예매(BookingType.PRERESERVE) 시간표 시드 생성
		List<PrereservationTimeTable> timeTables = schedules.stream()
			.filter(schedule -> schedule.getBookingType() == BookingType.PRERESERVE)
			.flatMap(schedule -> IntStream.range(0, sections.size())
				.mapToObj(idx -> {
					LocalDateTime bookingOpenAt = schedule.getBookingOpenAt();
					LocalDateTime startAt = bookingOpenAt.plusHours(idx);
					LocalDateTime endAt = startAt.plusHours(1).minusSeconds(1);

					LocalDateTime bookingCloseAt = schedule.getBookingCloseAt();
					if (bookingCloseAt != null && bookingCloseAt.isBefore(endAt)) {
						endAt = bookingCloseAt;
					}

					return PrereservationTimeTable.builder()
						.performanceScheduleId(schedule.getPerformanceScheduleId())
						.sectionId(sections.get(idx).getId())
						.bookingStartAt(startAt)
						.bookingEndAt(endAt)
						.build();
				}))
			.toList();
		prereservationTimeTableRepository.saveAll(timeTables);
		log.info("[DataInit/Test] Prereservation time tables initialized. count={}", timeTables.size());

		// 모든 구역의 좌석 생성
		List<Seat> seats = sections.stream()
			.flatMap(section -> IntStream.rangeClosed(1, 3)
				.boxed()
				.flatMap(row -> IntStream.rangeClosed(1, 5)
					.mapToObj(number -> Seat.builder()
						.venueId(venueId)
						.sectionId(section.getId())
						.sectionName(section.getSectionName())
						.rowLabel(String.valueOf(row))
						.seatNumber(number)
						.build())))
			.toList();
		List<Seat> savedSeats = seatRepository.saveAll(seats);

		// 좌석 등급 생성 (구역별로 다른 등급 적용)
		List<SeatGrade> allSeatGrades = IntStream.range(0, savedSeats.size())
			.mapToObj(idx -> {
				int seatInSection = idx % 15;  // 구역 내 좌석 번호 (0~14)
				int gradeGroup = seatInSection / 5;

				return SeatGrade.builder()
					.performanceId(performance.getPerformanceId())
					.seatId(savedSeats.get(idx).getId())
					.grade(switch (gradeGroup) {
						case 0 -> SeatGradeType.VIP;
						case 1 -> SeatGradeType.ROYAL;
						default -> SeatGradeType.STANDARD;
					})
					.price(switch (gradeGroup) {
						case 0 -> 30000;
						case 1 -> 20000;
						default -> 10000;
					})
					.build();
			}).toList();

		seatGradeRepository.saveAll(allSeatGrades);

		// 회차별 좌석 생성
		List<ScheduleSeat> allScheduleSeats = IntStream.range(0, savedSeats.size())
			.mapToObj(idx -> {
				return ScheduleSeat.builder()
					.scheduleId(performanceSchedule.getPerformanceScheduleId())
					.seatId(savedSeats.get(idx).getId())
					.build();
			})
			.toList();
		scheduleSeatRepository.saveAll(allScheduleSeats);

		/**
		 * A구역 (0~14):
		 *   - 좌석 0~4:   VIP (30,000원)
		 *   - 좌석 5~9:   ROYAL (20,000원)
		 *   - 좌석 10~14: STANDARD (10,000원)
		 *
		 * B구역 (15~29):
		 *   - 좌석 15~19: VIP (30,000원)
		 *   - 좌석 20~24: ROYAL (20,000원)
		 *   - 좌석 25~29: STANDARD (10,000원)
		 *
		 * C구역 (30~44):
		 *   - 좌석 30~34: VIP (30,000원)
		 *   - 좌석 35~39: ROYAL (20,000원)
		 *   - 좌석 40~44: STANDARD (10,000원)
		 */

		Member user1 = memberRepository.findByEmail("user1@tt.com")
			.orElseThrow(() -> new IllegalStateException("user1 not found"));

		// user1@tt.com에 3개의 티켓 생성 (다중 선택 테스트용)
		for (int i = 0; i < 3; i++) {
			Seat reservedSeat = seats.get(i);

			// 회차별 좌석 조회
			ScheduleSeat reservedScheduleSeat = scheduleSeatRepository
				.findByScheduleIdAndSeatId(
					performanceSchedule.getPerformanceScheduleId(),
					reservedSeat.getId()
				)
				.orElseThrow(() -> new IllegalStateException("ScheduleSeat not found"));

			// 좌석 상태 SOLD 처리
			reservedScheduleSeat.sold();

			// 예매 생성 (PENDING → COMPLETED)
			Reservation reservation = Reservation.builder()
				.scheduleId(performanceSchedule.getPerformanceScheduleId())
				.memberId(user1.getId())
				.expiresAt(LocalDateTime.now().plusMinutes(5))
				.build();

			Reservation savedReservation = reservationRepository.save(reservation);

			// Reservation ↔ ScheduleSeat 연결
			reservationSeatRepository.save(
				ReservationSeat.builder()
					.reservationId(savedReservation.getId())
					.scheduleSeatId(reservedScheduleSeat.getId())
					.build()
			);

			reservation.complete(LocalDateTime.now());

			// 결제 생성 (DONE 상태)
			Payment payment = Payment.builder()
				.orderId("ORDER-INIT-" + System.currentTimeMillis() + "-" + i)
				.memberId(user1.getId())
				.domainType(DomainType.RESERVATION)
				.domainId(savedReservation.getId())
				.amount(10000L)
				.method(PaymentMethod.CARD)
				.expiresAt(null)
				.build();

			payment.complete(LocalDateTime.now());
			paymentRepository.save(payment);

			// 티켓 생성
			Ticket ticket = Ticket.builder()
				.reservationId(savedReservation.getId())
				.memberId(user1.getId())
				.seatId(reservedSeat.getId())
				.build();

			ticketRepository.save(ticket);

			log.info("[DataInit] user1@tt.com - 예매/결제/티켓 생성 완료 (좌석: {}구역 {}행 {}번)",
				reservedSeat.getSectionName(), reservedSeat.getRowLabel(), reservedSeat.getSeatNumber());

			// orderId 중복 방지를 위한 짧은 대기
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		Seat paidSeat = seats.get(5); // 아직 사용 안 한 좌석 하나 선택

		// 회차별 좌석 조회
		ScheduleSeat paidScheduleSeat = scheduleSeatRepository
			.findByScheduleIdAndSeatId(
				performanceSchedule.getPerformanceScheduleId(),
				paidSeat.getId()
			)
			.orElseThrow(() -> new IllegalStateException("ScheduleSeat not found"));

		// 좌석 상태 HOLD 처리 (결제 완료했지만 확정 전)
		paidScheduleSeat.hold(LocalDateTime.now().plusMinutes(5));

		// 예매 생성
		Reservation paidReservation = Reservation.builder()
			.scheduleId(performanceSchedule.getPerformanceScheduleId())
			.memberId(user1.getId())
			.expiresAt(LocalDateTime.now().plusMinutes(5))
			.build();

		Reservation savedPaidReservation = reservationRepository.save(paidReservation);

		// Reservation ↔ ScheduleSeat 연결
		reservationSeatRepository.save(
			ReservationSeat.builder()
				.reservationId(savedPaidReservation.getId())
				.scheduleSeatId(paidScheduleSeat.getId())
				.build()
		);

		// codeisneverodd@gmail.com에 2개의 티켓 생성
		Member user3 = memberRepository.findByEmail("codeisneverodd@gmail.com")
			.orElseThrow(() -> new IllegalStateException("user3 not found"));

		for (int i = 3; i < 5; i++) {
			Seat reservedSeat = seats.get(i);

			// 회차별 좌석 조회
			ScheduleSeat reservedScheduleSeat = scheduleSeatRepository
				.findByScheduleIdAndSeatId(
					performanceSchedule.getPerformanceScheduleId(),
					reservedSeat.getId()
				)
				.orElseThrow(() -> new IllegalStateException("ScheduleSeat not found"));

			// 좌석 상태 SOLD 처리
			reservedScheduleSeat.sold();

			// 예매 생성 (PENDING → COMPLETED)
			Reservation reservation = Reservation.builder()
				.scheduleId(performanceSchedule.getPerformanceScheduleId())
				.memberId(user3.getId())
				.expiresAt(LocalDateTime.now().plusMinutes(5))
				.build();

			Reservation savedReservation = reservationRepository.save(reservation);

			reservationSeatRepository.save(
				ReservationSeat.builder()
					.reservationId(savedReservation.getId())
					.scheduleSeatId(reservedScheduleSeat.getId())
					.build()
			);

			reservation.complete(LocalDateTime.now());

			// 결제 생성 (DONE 상태)
			Payment payment = Payment.builder()
				.orderId("ORDER-INIT-" + System.currentTimeMillis() + "-" + i)
				.memberId(user3.getId())
				.domainType(DomainType.RESERVATION)
				.domainId(savedReservation.getId())
				.amount(10000L)
				.method(PaymentMethod.CARD)
				.expiresAt(null)
				.build();

			payment.complete(LocalDateTime.now());
			paymentRepository.save(payment);

			// 티켓 생성
			Ticket ticket = Ticket.builder()
				.reservationId(savedReservation.getId())
				.memberId(user3.getId())
				.seatId(reservedSeat.getId())
				.build();

			ticketRepository.save(ticket);

			log.info("[DataInit] codeisneverodd@gmail.com - 예매/결제/티켓 생성 완료 (좌석: {}구역 {}행 {}번)",
				reservedSeat.getSectionName(), reservedSeat.getRowLabel(), reservedSeat.getSeatNumber());

			// orderId 중복 방지를 위한 짧은 대기
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private void seedPrereservationTimeTablesIfMissing() {
		List<PerformanceSchedule> prereserveSchedules = performanceScheduleRepository.findAll().stream()
			.filter(schedule -> schedule.getBookingType() == BookingType.PRERESERVE)
			.filter(schedule -> schedule.getBookingOpenAt() != null)
			.filter(schedule -> schedule.getPerformance() != null)
			.filter(schedule -> schedule.getPerformance().getTitle() != null)
			.filter(schedule -> TEST_PERFORMANCE_TITLE.equals(schedule.getPerformance().getTitle()))
			.toList();

		if (prereserveSchedules.isEmpty()) {
			return;
		}

		int createdCount = 0;
		for (PerformanceSchedule schedule : prereserveSchedules) {
			Long scheduleId = schedule.getPerformanceScheduleId();
			var existing = prereservationTimeTableRepository
				.findAllByPerformanceScheduleIdOrderByBookingStartAtAscSectionIdAsc(scheduleId);
			var existingSectionIds = existing.stream().map(PrereservationTimeTable::getSectionId).collect(java.util.stream.Collectors.toSet());

			Long venueId = schedule.getPerformance().getVenue().getVenueId();
			List<Section> sections = sectionRepository.findByVenueId(venueId).stream()
				.sorted(java.util.Comparator.comparingLong(Section::getId))
				.toList();
			if (sections.isEmpty()) {
				continue;
			}

			LocalDateTime bookingOpenAt = schedule.getBookingOpenAt();
			LocalDateTime bookingCloseAt = schedule.getBookingCloseAt();

			List<PrereservationTimeTable> toCreate = new java.util.ArrayList<>();
			for (int idx = 0; idx < sections.size(); idx++) {
				Section section = sections.get(idx);
				if (existingSectionIds.contains(section.getId())) {
					continue;
				}

				LocalDateTime startAt = bookingOpenAt.plusHours(idx);
				LocalDateTime endAt = startAt.plusHours(1).minusSeconds(1);

				if (bookingCloseAt != null && bookingCloseAt.isBefore(endAt)) {
					endAt = bookingCloseAt;
				}
				if (!endAt.isAfter(startAt)) {
					continue;
				}

				toCreate.add(PrereservationTimeTable.builder()
					.performanceScheduleId(scheduleId)
					.sectionId(section.getId())
					.bookingStartAt(startAt)
					.bookingEndAt(endAt)
					.build());
			}

			if (!toCreate.isEmpty()) {
				prereservationTimeTableRepository.saveAll(toCreate);
				createdCount += toCreate.size();
			}
		}

		if (createdCount > 0) {
			log.info("[DataInit/Test] Prereservation time tables ensured. created={}", createdCount);
		}
	}

	// 추첨 데이터
	private void lottery() {

		List<Member> members1 = createMembers(10, memberRepository, passwordEncoder);
		List<Member> members2 = createMembers(10, memberRepository, passwordEncoder);
		List<Member> members3 = createMembers(10, memberRepository, passwordEncoder);

		Venue venue = createVenue("추첨공연장", venueRepository);
		List<Section> sections = createSections(venue.getVenueId(), sectionRepository, "A", "B", "C");
		List<Seat> seats = createSeats(venue.getVenueId(), sections, 3, 5, seatRepository);

		Performance performance = createPerformance(venue, performanceRepository);
		List<PerformanceSchedule> schedules = createSchedules(performance, 3, BookingType.LOTTERY,
			performanceScheduleRepository);
		createSeatGrades(performance, seats, seatGradeRepository);

		createLotteryEntry(members1, performance, schedules.getFirst(), SeatGradeType.STANDARD, lotteryEntryRepository);
		createLotteryEntry(members2, performance, schedules.getFirst(), SeatGradeType.VIP, lotteryEntryRepository);
		createLotteryEntry(members3, performance, schedules.getFirst(), SeatGradeType.ROYAL, lotteryEntryRepository);

		createLotteryEntry(members1, performance, schedules.get(1), SeatGradeType.STANDARD, lotteryEntryRepository);
		createLotteryEntry(members2, performance, schedules.get(1), SeatGradeType.VIP, lotteryEntryRepository);
		createLotteryEntry(members3, performance, schedules.get(1), SeatGradeType.ROYAL, lotteryEntryRepository);

		createLotteryEntry(members1, performance, schedules.get(2), SeatGradeType.STANDARD, lotteryEntryRepository);
		createLotteryEntry(members2, performance, schedules.get(2), SeatGradeType.VIP, lotteryEntryRepository);
		createLotteryEntry(members3, performance, schedules.get(2), SeatGradeType.ROYAL, lotteryEntryRepository);
	}

	/**
	 * 멤버 생성
	 * createMembers(10, memberRepo, encoder);
	 */
	public static List<Member> createMembers(
		int count,
		MemberRepository memberRepository,
		PasswordEncoder passwordEncoder
	) {
		int row = (int)(memberRepository.count() + 1);
		return IntStream.rangeClosed(row, row + count - 1)
			.mapToObj(i -> Member.builder()
				.email("user" + i + "@test.com")
				.password(passwordEncoder.encode("1234567a!"))
				.name("테스트유저" + i)
				.role(Member.Role.MEMBER)
				.provider(Member.Provider.EMAIL)
				.isEmailVerified(true)
				.isIdentityVerified(true)
				.build()
			)
			.map(memberRepository::save)
			.toList();
	}

	/**
	 * 공연장 생성
	 * createVenue("잠실실내체육관", venueRepo);
	 */
	public static Venue createVenue(
		String name,
		VenueRepository venueRepository
	) {
		return venueRepository.save(
			Venue.builder()
				.name(name)
				.build()
		);
	}

	/**
	 * 공연 생성
	 * createPerformance(venue, performanceRepo);
	 */
	public static Performance createPerformance(
		Venue venue,
		PerformanceRepository repo
	) {
		return repo.save(
			Performance.builder()
				.venue(venue)
				.title("테스트 공연")
				.category("콘서트")
				.posterUrl("")
				.startDate(LocalDateTime.now())
				.endDate(LocalDateTime.now().plusDays(7))
				.status(PerformanceStatus.ON_SALE)
				.build()
		);
	}

	/**
	 * 공연 회차 생성
	 * createSchedules(performance, 5, BookingType.LOTTERY, scheduleRepo);
	 */
	public static List<PerformanceSchedule> createSchedules(
		Performance performance,
		int count,
		BookingType bookingType,
		PerformanceScheduleRepository repo
	) {
		return repo.saveAll(
			IntStream.rangeClosed(1, count)
				.mapToObj(i -> PerformanceSchedule.builder()
					.performance(performance)
					.roundNo(i)
					.startAt(LocalDateTime.now().plusDays(i))
					.bookingType(bookingType)
					.bookingOpenAt(LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.MIDNIGHT))
					.bookingCloseAt(LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(12, 0)))
					.build()
				)
				.toList()
		);
	}

	/**
	 * 구역 생성
	 * createSections(venueId, sectionRepo, "A", "B", "C");
	 */
	public static List<Section> createSections(
		Long venueId,
		SectionRepository repo,
		String... names
	) {
		return repo.saveAll(
			Arrays.stream(names)
				.map(name -> Section.builder()
					.venueId(venueId)
					.sectionName(name)
					.build()
				)
				.toList()
		);
	}

	/**
	 * 좌석 생성
	 * createSeats(venue.getVenueId(), sections, 3, 5, seatRepo);
	 */
	public static List<Seat> createSeats(
		Long venueId,
		List<Section> sections,
		int rows,
		int cols,
		SeatRepository repo
	) {
		return repo.saveAll(
			sections.stream()
				.flatMap(section ->
					IntStream.rangeClosed(1, rows).boxed()
						.flatMap(r ->
							IntStream.rangeClosed(1, cols)
								.mapToObj(c -> Seat.builder()
									.venueId(venueId)
									.sectionId(section.getId())
									.sectionName(section.getSectionName())
									.rowLabel(String.valueOf(r))
									.seatNumber(c)
									.build()
								)
						)
				)
				.toList()
		);
	}

	/**
	 * 좌석 등급 생성 (VIP, ROYAL, STANDARD)
	 * createSeatGrades(performance, seats, seatGradeRepo);
	 */
	public static void createSeatGrades(
		Performance performance,
		List<Seat> seats,
		SeatGradeRepository repo
	) {
		List<SeatGrade> grades = IntStream.range(0, seats.size())
			.mapToObj(i -> {
				int group = (i % 15) / 5;
				return SeatGrade.builder()
					.performanceId(performance.getPerformanceId())
					.seatId(seats.get(i).getId())
					.grade(SeatGradeType.values()[
						ThreadLocalRandom.current().nextInt(SeatGradeType.values().length)
						])
					.price(switch (group) {
						case 0 -> 30000;
						case 1 -> 20000;
						default -> 10000;
					})
					.build();
			})
				.

			toList();

		repo.saveAll(grades);
	}

	/**
	 * 추첨 응모 생성
	 */
	public static List<LotteryEntry> createLotteryEntry(
		List<Member> members,
		Performance performance,
		PerformanceSchedule performanceSchedule,
		SeatGradeType seatGradeType,
		LotteryEntryRepository repo
	) {
		List<LotteryEntry> lotteryEntries = IntStream.range(0, members.size())
			.mapToObj(i -> {
				return LotteryEntry.builder()
					.memberId(members.get(i).getId())
					.performanceId(performance.getPerformanceId())
					.scheduleId(performanceSchedule.getPerformanceScheduleId())
					.grade(seatGradeType)
					.quantity((ThreadLocalRandom.current().nextInt(4) + 1))
					.build();
			}).toList();

		return repo.saveAll(lotteryEntries);
	}

}
