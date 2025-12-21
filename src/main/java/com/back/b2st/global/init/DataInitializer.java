package com.back.b2st.global.init;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
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
	private final PaymentRepository paymentRepository;
	private final TicketRepository ticketRepository;

	@Override
	public void run(String... args) throws Exception {
		// 서버 재시작시 중복 생성 방지 차
		initMemberData();
		initConnectedSet();
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
				.bookingType(i % 2 == 0 ? BookingType.SEAT : BookingType.LOTTERY)
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
				.seatId(reservedSeat.getId())
				.build();

			reservation.complete(LocalDateTime.now());
			Reservation savedReservation = reservationRepository.save(reservation);

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
		paidScheduleSeat.hold();

		// 예매 생성
		Reservation paidReservation = Reservation.builder()
			.scheduleId(performanceSchedule.getPerformanceScheduleId())
			.memberId(user1.getId())
			.seatId(paidSeat.getId())
			.build();

		Reservation savedPaidReservation = reservationRepository.save(paidReservation);

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
				.seatId(reservedSeat.getId())
				.build();

			reservation.complete(LocalDateTime.now());
			Reservation savedReservation = reservationRepository.save(reservation);

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
}
