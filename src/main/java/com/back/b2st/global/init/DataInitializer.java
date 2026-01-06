package com.back.b2st.global.init;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

import com.back.b2st.domain.lottery.draw.service.DrawService;
import com.back.b2st.domain.lottery.entry.entity.LotteryEntry;
import com.back.b2st.domain.lottery.entry.entity.LotteryStatus;
import com.back.b2st.domain.lottery.entry.repository.LotteryEntryRepository;
import com.back.b2st.domain.lottery.result.repository.LotteryResultRepository;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.payment.dto.request.PaymentPayReq;
import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentMethod;
import com.back.b2st.domain.payment.repository.PaymentRepository;
import com.back.b2st.domain.payment.service.PaymentOneClickService;
import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.entity.PerformanceStatus;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.prereservation.booking.repository.PrereservationBookingRepository;
import com.back.b2st.domain.prereservation.entry.entity.Prereservation;
import com.back.b2st.domain.prereservation.entry.repository.PrereservationRepository;
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
	private static final String TEST_PRERESERVE_PLAY_TITLE = "연극 - B2ST 신청예매 테스트";

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
	private final PrereservationRepository prereservationRepository;
	private final PrereservationBookingRepository prereservationBookingRepository;
	private final DrawService drawService;
	private final LotteryResultRepository lotteryResultRepository;
	private final PaymentOneClickService paymentOneClickService;

	@Override
	public void run(String... args) throws Exception {
		// 서버 재시작시 중복 생성 방지 차
		initMemberData();
		initConnectedSet();
		lottery();
		lotteryForDrawExecution();
		lotteryForSeatAllocation();
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
		// 신청예매 테스트 공연은 init 정책이 바뀐 경우에만 1회성으로 재생성
		recreatePrereservePerformance();

		// 중복 생성 방지: 이미 공연장이 있으면 스킵
		if (venueRepository.count() > 0) {
			log.info("[DataInit] 이미 데이터 존재하여 초기화 스킵");
			seedPrereservationTimeTablesIfMissing();
			seedPrereservationApplicationsIfMissing();
			ensurePrereservationHoldTestAlwaysOpen();
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
			.posterKey(null)
			.description(null)
			.startDate(LocalDateTime.of(2024, 12, 20, 19, 0))
			.endDate(LocalDateTime.of(2024, 12, 29, 21, 0))
			.status(PerformanceStatus.ACTIVE)
			.build());

		Long venueId = venue.getVenueId();

		// 기존 회차 24개 생성 (일반예매/추첨 - 신청예매 구현 전 원래 데이터)
		List<PerformanceSchedule> schedules = IntStream.rangeClosed(0, 23)
			.mapToObj(i -> PerformanceSchedule.builder()
				.performance(performance)
				.startAt(LocalDateTime.of(2025, 1, 1, 19, 0).plusDays(i))
				.roundNo(i + 1)
				.bookingType(i % 2 == 0 ? BookingType.FIRST_COME : BookingType.LOTTERY)
				.bookingOpenAt(LocalDateTime.now().minusHours(1))
				.bookingCloseAt(LocalDateTime.now().plusDays(30))
				.build()
			).toList();
		performanceScheduleRepository.saveAll(schedules);
		performanceSchedule = schedules.getFirst();
		performanceSchedule2 = schedules.get(1);

		// 신청예매(PRERESERVE) 테스트 시간 세팅
		// - 사전 신청: now < bookingOpenAt 이어야 신청 가능
		// - 실제 예매(HOLD/booking): now >= bookingOpenAt 이어야 진행 가능
		// 같은 회차에서 둘을 동시에 테스트할 수 없어서, 회차별로 bookingOpenAt을 다르게 세팅한다.
		LocalDateTime nowHour = LocalDateTime.now()
			.withMinute(0)
			.withSecond(0)
			.withNano(0);
		LocalDateTime bookingOpenAtForHold = nowHour;          // 오픈됨 → HOLD/예매 테스트용
		LocalDateTime bookingOpenAtForApply = nowHour.plusHours(24); // 오픈 전 → 사전 신청 테스트용
		LocalDateTime prereserveStartAtBase = LocalDateTime.now()
			.withHour(19)
			.withMinute(0)
			.withSecond(0)
			.withNano(0);

		// 신청예매 테스트 전용 공연(연극) 추가: 기존(1~24회차) 데이터와 겹치지 않게 별도 공연으로 분리
		// 구성은 기존 콘서트 데이터와 동일하게 맞추되(venue/필수 필드), 내용만 다르게 한다.
		Performance prereservePlay = performanceRepository.save(Performance.builder()
			.venue(venue)
			.title(TEST_PRERESERVE_PLAY_TITLE)
			.category("연극")
			.posterKey(null)
			.description("신청예매(사전신청) 기능 테스트용 연극 공연입니다.")
			.startDate(prereserveStartAtBase)
			.endDate(prereserveStartAtBase.plusDays(5))
			.status(PerformanceStatus.ACTIVE)
			.build());

		// 신청예매 테스트용 회차 추가 (1~6회차)
		// - 예: 오늘이 1/5이면 1/5~1/10까지 선택 가능하도록 구성
		List<PerformanceSchedule> prereserveSchedules = IntStream.rangeClosed(0, 5)
			.mapToObj(idx -> PerformanceSchedule.builder()
				.performance(prereservePlay)
				.startAt(prereserveStartAtBase.plusDays(idx))
				.roundNo(1 + idx)
				.bookingType(BookingType.PRERESERVE)
				// - 1~2회차: 예매 오픈(=HOLD 가능)
				// - 3~6회차: 사전 신청 가능(예매 오픈 전)
				.bookingOpenAt(idx < 2 ? bookingOpenAtForHold : bookingOpenAtForApply)
				.bookingCloseAt(LocalDateTime.now().plusDays(30))
				.build()
			).toList();
		performanceScheduleRepository.saveAll(prereserveSchedules);

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
		List<PrereservationTimeTable> timeTables = prereserveSchedules.stream()
			.flatMap(schedule -> IntStream.range(0, sections.size())
				.mapToObj(idx -> {
					LocalDateTime bookingOpenAt = schedule.getBookingOpenAt();
					LocalDateTime startAt = bookingOpenAt;
					LocalDateTime endAt = schedule.getBookingCloseAt() != null
						? schedule.getBookingCloseAt()
						: bookingOpenAt.plusDays(30);

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

		// 신청예매 테스트용 사전 신청 시드(user1/user2)
		// - 예매 오픈된 회차(HOLD 테스트용)에 한해서만 미리 신청을 만들어 둔다.
		// - 사전 신청 테스트용 회차는 프론트에서 직접 신청 → 신청 성공/실패 케이스를 확인할 수 있도록 비워둔다.
		seedPrereservationApplications(prereserveSchedules, sections);
		seedPrereservationApplicationsIfMissing();
		ensurePrereservationHoldTestAlwaysOpen();

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

		// 신청예매 회차에도 좌석을 붙여서(선점/예매 테스트 가능) 별도 생성
		List<ScheduleSeat> prereserveScheduleSeats = prereserveSchedules.stream()
			.flatMap(schedule -> savedSeats.stream()
				.map(seat -> ScheduleSeat.builder()
					.scheduleId(schedule.getPerformanceScheduleId())
					.seatId(seat.getId())
					.build()))
			.toList();
		scheduleSeatRepository.saveAll(prereserveScheduleSeats);

		// 신청예매 공연에도 좌석 등급(정가)을 별도 생성 (SeatGrade는 performanceId 기준)
		List<SeatGrade> prereserveSeatGrades = IntStream.range(0, savedSeats.size())
			.mapToObj(idx -> {
				int seatInSection = idx % 15;
				int gradeGroup = seatInSection / 5;
				return SeatGrade.builder()
					.performanceId(prereservePlay.getPerformanceId())
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
		seatGradeRepository.saveAll(prereserveSeatGrades);

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

	private void seedPrereservationApplications(List<PerformanceSchedule> prereserveSchedules, List<Section> sections) {
		var user1 = memberRepository.findByEmail("user1@tt.com").orElse(null);
		var user2 = memberRepository.findByEmail("user2@tt.com").orElse(null);
		var user3 = memberRepository.findByEmail("codeisneverodd@gmail.com").orElse(null);
		if (user1 == null && user2 == null && user3 == null) {
			return;
		}

		var members = java.util.List.of(user1, user2, user3).stream().filter(java.util.Objects::nonNull).toList();
		int created = 0;

		LocalDateTime now = LocalDateTime.now();
		for (PerformanceSchedule schedule : prereserveSchedules) {
			// 예매 오픈 전(사전 신청 테스트용) 회차는 미리 신청을 생성하지 않는다.
			if (now.isBefore(schedule.getBookingOpenAt())) {
				continue;
			}

			Long scheduleId = schedule.getPerformanceScheduleId();
			for (var member : members) {
				for (Section section : sections) {
					if (prereservationRepository.existsByPerformanceScheduleIdAndMemberIdAndSectionId(
						scheduleId, member.getId(), section.getId()
					)) {
						continue;
					}
					prereservationRepository.save(
						Prereservation.builder()
							.performanceScheduleId(scheduleId)
							.memberId(member.getId())
							.sectionId(section.getId())
							.build()
					);
					created++;
				}
			}
		}

		if (created > 0) {
			log.info("[DataInit/Test] Prereservation applications seeded. created={}", created);
		}
	}

	/**
	 * 기존 DB에 user1/user2 신청 내역만 남아있는 경우가 있어, user3 포함해서 누락분을 보충한다.
	 * - 신청예매 테스트 공연에 한함(TEST_PRERESERVE_PLAY_TITLE)
	 * - 오픈된 회차(now >= bookingOpenAt)만 대상
	 */
	private void seedPrereservationApplicationsIfMissing() {
		var user1 = memberRepository.findByEmail("user1@tt.com").orElse(null);
		var user2 = memberRepository.findByEmail("user2@tt.com").orElse(null);
		var user3 = memberRepository.findByEmail("codeisneverodd@gmail.com").orElse(null);
		if (user1 == null && user2 == null && user3 == null) {
			return;
		}

		var members = java.util.List.of(user1, user2, user3).stream().filter(java.util.Objects::nonNull).toList();
		LocalDateTime now = LocalDateTime.now();

		List<PerformanceSchedule> openSchedules = performanceScheduleRepository.findAll().stream()
			.filter(schedule -> schedule.getBookingType() == BookingType.PRERESERVE)
			.filter(schedule -> schedule.getBookingOpenAt() != null)
			.filter(schedule -> schedule.getPerformance() != null)
			.filter(schedule -> TEST_PRERESERVE_PLAY_TITLE.equals(schedule.getPerformance().getTitle()))
			.filter(schedule -> !now.isBefore(schedule.getBookingOpenAt()))
			.toList();

		if (openSchedules.isEmpty()) {
			return;
		}

		int created = 0;
		for (PerformanceSchedule schedule : openSchedules) {
			Long scheduleId = schedule.getPerformanceScheduleId();
			Long venueId = schedule.getPerformance().getVenue().getVenueId();
			List<Section> sections = sectionRepository.findByVenueId(venueId);
			if (sections.isEmpty()) {
				continue;
			}

			for (var member : members) {
				for (Section section : sections) {
					if (prereservationRepository.existsByPerformanceScheduleIdAndMemberIdAndSectionId(
						scheduleId, member.getId(), section.getId()
					)) {
						continue;
					}
					prereservationRepository.save(
						Prereservation.builder()
							.performanceScheduleId(scheduleId)
							.memberId(member.getId())
							.sectionId(section.getId())
							.build()
					);
					created++;
				}
			}
		}

		if (created > 0) {
			log.info("[DataInit/Test] Prereservation applications ensured. created={}", created);
		}
	}

	private void seedPrereservationTimeTablesIfMissing() {
		List<PerformanceSchedule> prereserveSchedules = performanceScheduleRepository.findAll().stream()
			.filter(schedule -> schedule.getBookingType() == BookingType.PRERESERVE)
			.filter(schedule -> schedule.getBookingOpenAt() != null)
			.filter(schedule -> schedule.getPerformance() != null)
			.filter(schedule -> schedule.getPerformance().getTitle() != null)
			.filter(schedule -> TEST_PERFORMANCE_TITLE.equals(schedule.getPerformance().getTitle())
				|| TEST_PRERESERVE_PLAY_TITLE.equals(schedule.getPerformance().getTitle()))
			.toList();

		if (prereserveSchedules.isEmpty()) {
			return;
		}

		int createdCount = 0;
		for (PerformanceSchedule schedule : prereserveSchedules) {
			Long scheduleId = schedule.getPerformanceScheduleId();
			var existing = prereservationTimeTableRepository
				.findAllByPerformanceScheduleIdOrderByBookingStartAtAscSectionIdAsc(scheduleId);
			var existingSectionIds = existing.stream()
				.map(PrereservationTimeTable::getSectionId)
				.collect(java.util.stream.Collectors.toSet());

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

				LocalDateTime startAt = bookingOpenAt;
				LocalDateTime endAt = bookingCloseAt != null ? bookingCloseAt : bookingOpenAt.plusDays(30);
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

	/**
	 * 신청예매 테스트가 시간대(구역별 1시간 슬롯)에 묶이지 않도록,
	 * 신청예매 테스트 공연의 구역별 시간표를 bookingOpenAt ~ bookingCloseAt(없으면 +30일)로 확장한다.
	 */
	private void ensurePrereservationHoldTestAlwaysOpen() {
		List<PerformanceSchedule> prereserveSchedules = performanceScheduleRepository.findAll().stream()
			.filter(schedule -> schedule.getBookingType() == BookingType.PRERESERVE)
			.filter(schedule -> schedule.getBookingOpenAt() != null)
			.filter(schedule -> schedule.getPerformance() != null)
			.filter(schedule -> TEST_PRERESERVE_PLAY_TITLE.equals(schedule.getPerformance().getTitle()))
			.toList();

		if (prereserveSchedules.isEmpty()) {
			return;
		}

		int updated = 0;
		for (PerformanceSchedule schedule : prereserveSchedules) {
			Long venueId = schedule.getPerformance().getVenue().getVenueId();
			List<Section> sections = sectionRepository.findByVenueId(venueId).stream()
				.sorted(java.util.Comparator.comparingLong(Section::getId))
				.toList();
			if (sections.isEmpty()) {
				continue;
			}

			LocalDateTime startAtForSchedule = schedule.getBookingOpenAt();
			LocalDateTime endAtForSchedule = schedule.getBookingCloseAt() != null
				? schedule.getBookingCloseAt()
				: startAtForSchedule.plusDays(30);

			Long scheduleId = schedule.getPerformanceScheduleId();
			List<PrereservationTimeTable> existing = prereservationTimeTableRepository
				.findAllByPerformanceScheduleIdOrderByBookingStartAtAscSectionIdAsc(scheduleId);
			Map<Long, PrereservationTimeTable> bySectionId = existing.stream()
				.collect(java.util.stream.Collectors.toMap(
					PrereservationTimeTable::getSectionId,
					tt -> tt,
					(left, right) -> right
				));

			for (Section section : sections) {
				var timeTable = bySectionId.get(section.getId());
				if (timeTable == null) {
					prereservationTimeTableRepository.save(PrereservationTimeTable.builder()
						.performanceScheduleId(scheduleId)
						.sectionId(section.getId())
						.bookingStartAt(startAtForSchedule)
						.bookingEndAt(endAtForSchedule)
						.build());
					updated++;
					continue;
				}

				// 1시간 슬롯 등 과거 데이터가 남아있어도 "전체 기간 오픈"으로 정규화
				if (!startAtForSchedule.equals(timeTable.getBookingStartAt())
					|| !endAtForSchedule.equals(timeTable.getBookingEndAt())) {
					timeTable.updateBookingTime(startAtForSchedule, endAtForSchedule);
					updated++;
				}
			}
		}

		if (updated > 0) {
			log.info("[DataInit/Test] Prereservation time tables widened. updated={}", updated);
		}
	}

	// 추첨 데이터
	private void lottery() {
		List<Member> members1 = createMembers(3, memberRepository, passwordEncoder);
		List<Member> members2 = createMembers(3, memberRepository, passwordEncoder);
		List<Member> members3 = createMembers(3, memberRepository, passwordEncoder);

		Venue venue = createVenue("추첨공연장", venueRepository);
		List<Section> sections = createSections(venue.getVenueId(), sectionRepository, "A", "B", "C");
		List<Seat> seats = createSeats(venue.getVenueId(), sections, 3, 5, seatRepository);

		Performance performance = createPerformance(venue, performanceRepository);
		List<PerformanceSchedule> schedules = createSchedules(performance, 2, BookingType.LOTTERY,
			performanceScheduleRepository);
		createSeatGrades(performance, seats, seatGradeRepository);

		createLotteryEntry(members1, performance, schedules.getFirst(), SeatGradeType.STANDARD, lotteryEntryRepository);
		createLotteryEntry(members2, performance, schedules.getFirst(), SeatGradeType.VIP, lotteryEntryRepository);
		createLotteryEntry(members3, performance, schedules.getFirst(), SeatGradeType.ROYAL, lotteryEntryRepository);

		createLotteryEntry(members1, performance, schedules.get(1), SeatGradeType.STANDARD, lotteryEntryRepository);
		createLotteryEntry(members2, performance, schedules.get(1), SeatGradeType.VIP, lotteryEntryRepository);
		createLotteryEntry(members3, performance, schedules.get(1), SeatGradeType.ROYAL, lotteryEntryRepository);
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
				.email("user" + i + "@tt.com")
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
				.posterKey(null)
				.startDate(LocalDateTime.now())
				.endDate(LocalDateTime.now().plusDays(7))
				.status(PerformanceStatus.ACTIVE)
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
					.bookingOpenAt(LocalDateTime.now().minusHours(1))
					.bookingCloseAt(LocalDateTime.now().plusDays(30))
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
				SeatGradeType grade = SeatGradeType.values()[
					ThreadLocalRandom.current().nextInt(SeatGradeType.values().length)];

				return SeatGrade.builder()
					.performanceId(performance.getPerformanceId())
					.seatId(seats.get(i).getId())
					.grade(grade)
					.price(switch (grade) {
						case VIP -> 30000;
						case ROYAL -> 20000;
						default -> 10000;
					})
					.build();
			})
			.toList();

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

	/**
	 * 신청예매 테스트 공연 재생성(필요 시)
	 * - 기존 데이터가 현재 init 정책과 불일치하면 관련 데이터 정리 후 재생성
	 * - 이미 정책에 맞게 생성되어 있으면 유지(매 실행마다 삭제하지 않음)
	 */
	private void recreatePrereservePerformance() {
		List<Performance> candidates = performanceRepository.findAll().stream()
			.filter(p -> TEST_PRERESERVE_PLAY_TITLE.equals(p.getTitle()))
			.toList();

		Performance keep = candidates.stream()
			.filter(p -> isPrereservePerformanceCompatible(p.getPerformanceId()))
			.findFirst()
			.orElse(null);

		if (keep != null) {
			for (Performance p : candidates) {
				if (p.getPerformanceId().equals(keep.getPerformanceId())) {
					continue;
				}
				log.info("[DataInit] 중복 신청예매 공연 삭제: ID={}", p.getPerformanceId());
				deletePrereserveRelatedData(p.getPerformanceId());
				performanceRepository.delete(p);
			}
			return;
		}

		for (Performance oldPerformance : candidates) {
			log.info("[DataInit] 기존 신청예매 공연 삭제(정책 불일치): ID={}", oldPerformance.getPerformanceId());
			deletePrereserveRelatedData(oldPerformance.getPerformanceId());
			performanceRepository.delete(oldPerformance);
		}

		// 공연장이 없으면 신청예매 공연도 생성 불가
		if (venueRepository.count() == 0) {
			return;
		}

		Venue venue = venueRepository.findAll().stream().findFirst().orElse(null);
		if (venue == null) {
			return;
		}

		List<Section> sections = sectionRepository.findByVenueId(venue.getVenueId());
		if (sections.isEmpty()) {
			return;
		}

		// 신청예매(PRERESERVE) 테스트 시간 세팅
		// - 사전 신청: now < bookingOpenAt 이어야 신청 가능
		// - 실제 예매(HOLD/booking): now >= bookingOpenAt 이어야 진행 가능
		LocalDateTime nowHour = LocalDateTime.now()
			.withMinute(0)
			.withSecond(0)
			.withNano(0);
		LocalDateTime bookingOpenAtForHold = nowHour;
		LocalDateTime bookingOpenAtForApply = nowHour.plusHours(24);
		LocalDateTime prereserveStartAtBase = LocalDateTime.now()
			.withHour(19)
			.withMinute(0)
			.withSecond(0)
			.withNano(0);

		Performance prereservePlay = performanceRepository.save(Performance.builder()
			.venue(venue)
			.title(TEST_PRERESERVE_PLAY_TITLE)
			.category("연극")
			.posterKey(null)
			.description("신청예매(사전신청) 기능 테스트용 연극 공연입니다.")
			.startDate(prereserveStartAtBase)
			.endDate(prereserveStartAtBase.plusDays(5))
			.status(PerformanceStatus.ACTIVE)
			.build());

		log.info("[DataInit] 신청예매 공연 재생성: 날짜={} ~ {}", prereserveStartAtBase.toLocalDate(),
			prereserveStartAtBase.plusDays(5).toLocalDate());

		// 회차 생성
		List<PerformanceSchedule> prereserveSchedules = IntStream.rangeClosed(0, 5)
			.mapToObj(idx -> PerformanceSchedule.builder()
				.performance(prereservePlay)
				.startAt(prereserveStartAtBase.plusDays(idx))
				.roundNo(1 + idx)
				.bookingType(BookingType.PRERESERVE)
				.bookingOpenAt(idx < 2 ? bookingOpenAtForHold : bookingOpenAtForApply)
				.bookingCloseAt(LocalDateTime.now().plusDays(30))
				.build()
			).toList();
		performanceScheduleRepository.saveAll(prereserveSchedules);

		log.info("[DataInit] 신청예매 회차 생성: {}개 (1~6회차)", prereserveSchedules.size());

		// 시간표 생성
		List<PrereservationTimeTable> timeTables = prereserveSchedules.stream()
			.flatMap(schedule -> IntStream.range(0, sections.size())
				.mapToObj(idx -> {
					LocalDateTime bookingOpenAt = schedule.getBookingOpenAt();
					LocalDateTime startAt = bookingOpenAt;
					LocalDateTime endAt = schedule.getBookingCloseAt() != null
						? schedule.getBookingCloseAt()
						: bookingOpenAt.plusDays(30);

					return PrereservationTimeTable.builder()
						.performanceScheduleId(schedule.getPerformanceScheduleId())
						.sectionId(sections.get(idx).getId())
						.bookingStartAt(startAt)
						.bookingEndAt(endAt)
						.build();
				}))
			.toList();
		prereservationTimeTableRepository.saveAll(timeTables);

		log.info("[DataInit] 신청예매 시간표 생성: {}개", timeTables.size());

		// 좌석 등급 생성
		List<Seat> seats = seatRepository.findAll();
		if (!seats.isEmpty()) {
			List<SeatGrade> prereserveSeatGrades = IntStream.range(0, seats.size())
				.mapToObj(idx -> {
					int seatInSection = idx % 15;
					int gradeGroup = seatInSection / 5;
					return SeatGrade.builder()
						.performanceId(prereservePlay.getPerformanceId())
						.seatId(seats.get(idx).getId())
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
			seatGradeRepository.saveAll(prereserveSeatGrades);

			// 회차별 좌석 생성
			List<ScheduleSeat> prereserveScheduleSeats = prereserveSchedules.stream()
				.flatMap(schedule -> seats.stream()
					.map(seat -> ScheduleSeat.builder()
						.scheduleId(schedule.getPerformanceScheduleId())
						.seatId(seat.getId())
						.build()))
				.toList();
			scheduleSeatRepository.saveAll(prereserveScheduleSeats);

			log.info("[DataInit] 신청예매 좌석 등급 및 회차별 좌석 생성 완료");
		}

		// 사전 신청 데이터 시드
		seedPrereservationApplications(prereserveSchedules, sections);
	}

	private void deletePrereserveRelatedData(Long performanceId) {
		try {
			List<Long> scheduleIds = performanceScheduleRepository
				.findAllByPerformance_PerformanceIdOrderByStartAtAsc(performanceId)
				.stream()
				.map(PerformanceSchedule::getPerformanceScheduleId)
				.toList();

			if (!scheduleIds.isEmpty()) {
				prereservationBookingRepository.deleteAllByScheduleIdIn(scheduleIds);
				scheduleSeatRepository.deleteAllByScheduleIdIn(scheduleIds);
				prereservationTimeTableRepository.deleteAllByPerformanceScheduleIdIn(scheduleIds);
				prereservationRepository.deleteAllByPerformanceScheduleIdIn(scheduleIds);
				performanceScheduleRepository.deleteAllByPerformanceId(performanceId);
			}

			seatGradeRepository.deleteAllByPerformanceId(performanceId);
		} catch (Exception e) {
			log.warn("[DataInit] 신청예매 관련 데이터 정리 실패 - performanceId={}", performanceId, e);
		}
	}

	private boolean isPrereservePerformanceCompatible(Long performanceId) {
		try {
			List<PerformanceSchedule> schedules =
				performanceScheduleRepository.findAllByPerformance_PerformanceIdOrderByStartAtAsc(performanceId);

			List<PerformanceSchedule> prereserveSchedules = schedules.stream()
				.filter(s -> s.getBookingType() == BookingType.PRERESERVE)
				.toList();

			if (prereserveSchedules.size() != 6) {
				return false;
			}

			LocalDateTime now = LocalDateTime.now();
			long openCount = prereserveSchedules.stream().filter(s -> !now.isBefore(s.getBookingOpenAt())).count();
			long preOpenCount = prereserveSchedules.stream().filter(s -> now.isBefore(s.getBookingOpenAt())).count();

			return openCount >= 1 && preOpenCount >= 1;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 추첨 실행용 공연 데이터 생성
	 */
	private void lotteryForDrawExecution() {
		List<Member> members1 = createMembers(3, memberRepository, passwordEncoder);
		List<Member> members2 = createMembers(3, memberRepository, passwordEncoder);
		List<Member> members3 = createMembers(3, memberRepository, passwordEncoder);

		Venue venue = createVenue("추첨실행-테스트공연장", venueRepository);
		List<Section> sections = createSections(venue.getVenueId(), sectionRepository, "A", "B", "C");
		List<Seat> seats = createSeats(venue.getVenueId(), sections, 3, 5, seatRepository);

		Performance performance = createPerformance(venue, performanceRepository);

		LocalDateTime now = LocalDateTime.now();

		PerformanceSchedule schedule = performanceScheduleRepository.save(
			PerformanceSchedule.builder()
				.performance(performance)
				.roundNo(1)
				.bookingType(BookingType.LOTTERY)
				.bookingOpenAt(now.minusDays(3))
				.bookingCloseAt(
					LocalDate.now().minusDays(1).atTime(10, 0) // ✅ 어제
				)
				.startAt(now.plusDays(10)) // 의미 없음
				.build()
		);

		createSeatGrades(performance, seats, seatGradeRepository);

		createLotteryEntry(members1, performance, schedule, SeatGradeType.STANDARD, lotteryEntryRepository);
		createLotteryEntry(members2, performance, schedule, SeatGradeType.VIP, lotteryEntryRepository);
		createLotteryEntry(members3, performance, schedule, SeatGradeType.ROYAL, lotteryEntryRepository);

		drawService.executeDraws();

		log.info("[DataInit/Lottery] 추첨 실행 대상 공연 데이터 생성 완료");
	}

	/**
	 * 좌석 배치용 공연 데이터 생성 (추첨 완료 상태)
	 */
	private void lotteryForSeatAllocation() {
		List<Member> members = createMembers(5, memberRepository, passwordEncoder);

		Venue venue = createVenue("좌석배치-테스트공연장", venueRepository);
		List<Section> sections = createSections(venue.getVenueId(), sectionRepository, "A", "B", "C");
		List<Seat> seats = createSeats(venue.getVenueId(), sections, 3, 5, seatRepository);

		Performance performance = createPerformance(venue, performanceRepository);

		// PerformanceSchedule schedule = performanceScheduleRepository.save(
		// 	PerformanceSchedule.builder()
		// 		.performance(performance)
		// 		.roundNo(1)
		// 		.bookingType(BookingType.LOTTERY)
		// 		.bookingOpenAt(LocalDateTime.now().minusDays(5))
		// 		.bookingCloseAt(LocalDateTime.now().minusDays(3))
		// 		.startAt(
		// 			LocalDate.now().plusDays(2).atTime(19, 0) // ✅ 조회 범위 내
		// 		)
		// 		.build()
		// );

		PerformanceSchedule schedule = performanceScheduleRepository.save(
			PerformanceSchedule.builder()
				.performance(performance)
				.roundNo(1)
				.bookingType(BookingType.LOTTERY)
				.bookingOpenAt(LocalDateTime.now().minusDays(3))
				.bookingCloseAt(
					LocalDate.now().minusDays(1).atTime(10, 0) // ✅ 어제
				)
				.startAt(LocalDateTime.now().plusDays(10)) // 의미 없음
				.build()
		);

		// 좌석 등급 생성
		createSeatGrades(performance, seats, seatGradeRepository);

		// ScheduleSeat 생성 (좌석 배치를 위해 필요)
		createScheduleSeatsForSchedule(
			schedule.getPerformanceScheduleId(),
			seats,
			scheduleSeatRepository
		);

		// 추첨 응모 생성 (STANDARD 등급만)
		List<LotteryEntry> entries = createLotteryEntry(members, performance, schedule, SeatGradeType.STANDARD,
			lotteryEntryRepository);

		// 추첨
		drawService.executeDraws();

		List<LotteryEntry> wonEntries = lotteryEntryRepository.findAllById(
				entries.stream().map(LotteryEntry::getId).toList()
			).stream()
			.filter(entry -> entry.getStatus() == LotteryStatus.WIN)
			.toList();

		log.info("[DataInit] 결제 완료: wonEntries.size()={}", wonEntries.size());

		// 당첨 결제 진행
		for (LotteryEntry entry : wonEntries) {
			try {
				PaymentPayReq req = new PaymentPayReq(
					DomainType.LOTTERY,
					PaymentMethod.CARD,
					0L,
					entry.getUuid()
				);
				paymentOneClickService.pay(entry.getMemberId(), req);
				log.info("[DataInit] 결제 완료: entryId={}", entry.getUuid());
			} catch (Exception e) {
				log.warn("[DataInit] 결제 실패: entryId={}, error={}", entry.getUuid(), e.getMessage());
			}
		}

		drawService.executeAllocation();

		log.info("[DataInit/Lottery] 좌석 배치 대상 공연 데이터 생성 완료 (추첨 완료 상태)");
	}

	/**
	 * 특정 회차에 대한 ScheduleSeat 생성
	 */
	public static List<ScheduleSeat> createScheduleSeatsForSchedule(
		Long scheduleId,
		List<Seat> seats,
		ScheduleSeatRepository repo
	) {
		return repo.saveAll(
			seats.stream()
				.map(seat -> ScheduleSeat.builder()
					.scheduleId(scheduleId)
					.seatId(seat.getId())
					.build())
				.toList()
		);
	}

}
