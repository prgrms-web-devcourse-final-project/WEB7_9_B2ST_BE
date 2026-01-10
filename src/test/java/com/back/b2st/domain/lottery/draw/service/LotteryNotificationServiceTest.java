package com.back.b2st.domain.lottery.draw.service;

import static com.back.b2st.support.TestFixture.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.email.service.EmailService;
import com.back.b2st.domain.lottery.entry.entity.LotteryEntry;
import com.back.b2st.domain.lottery.entry.repository.LotteryEntryRepository;
import com.back.b2st.domain.lottery.result.entity.LotteryResult;
import com.back.b2st.domain.lottery.result.repository.LotteryResultRepository;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.seat.grade.entity.SeatGradeType;
import com.back.b2st.domain.seat.grade.repository.SeatGradeRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.domain.venue.venue.repository.VenueRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LotteryNotificationServiceTest {

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
	private ScheduleSeatRepository scheduleSeatRepository;

	@Autowired
	private EmailService emailService;

	@Autowired
	private LotteryResultRepository lotteryResultRepository;

	@Autowired
	private LotteryEntryRepository lotteryEntryRepository;

	@Autowired
	private DrawService drawService;

	@Autowired
	EntityManager entityManager;

	List<Member> members1;
	Venue venue;
	Performance performance;
	List<PerformanceSchedule> schedules;
	List<Section> sections;
	List<Seat> seats;
	PerformanceSchedule schedule;

	List<LotteryEntry> lotteryEntries1;

	@BeforeEach
	void setUp() {
		members1 = createMembers(10, memberRepository, passwordEncoder);

		venue = createVenue("잠실실내체육관", venueRepository);
		performance = createPerformance(venue, performanceRepository);
		schedules = createSchedules(performance, 1, BookingType.LOTTERY,
			performanceScheduleRepository);
		schedule = schedules.getFirst();

		sections = createSections(venue.getVenueId(), sectionRepository, "A", "B", "C");
		seats = createSeats(venue.getVenueId(), sections, 3, 5, seatRepository);
		createSeatGrades(performance, seats, SeatGradeType.STANDARD, 10000, seatGradeRepository);
		createSeatGrades(performance, seats, SeatGradeType.VIP, 30000, seatGradeRepository);
		createScheduleSeats(schedule.getPerformanceScheduleId(), seats, scheduleSeatRepository);

		System.out.println("============================== init data OK ==============================");
	}

	@Autowired
	LotteryNotificationService notificationService;

	@Test
	@DisplayName("실제 SMTP로 이메일 1건 발송")
	@Disabled
	void notifyWinners_success() {
		// given
		Member member = memberRepository.save(
			Member.builder()
				.email("codingworld@naver.com")
				.password(passwordEncoder.encode("1234567a!"))
				.name("실제발송테스트")
				.role(Member.Role.MEMBER)
				.provider(Member.Provider.EMAIL)
				.isEmailVerified(true)
				.isIdentityVerified(true)
				.build()
		);

		createLotteryEntry(List.of(member), performance, schedule,
			SeatGradeType.STANDARD, 1, lotteryEntryRepository);

		entityManager.flush();
		drawService.executeDraws();
		List<LotteryResult> results = lotteryResultRepository.findAll();
		//
		// results.forEach(result -> {
		// 	result.confirmPayment();
		// });

		entityManager.flush();

		// when
		// emailService.sendWinnerNotifications(schedule.getPerformanceScheduleId());

		// when
		// notificationService.notifyWinners(scheduleId);

		// then
		// verify(emailService).sendWinnerNotifications(scheduleId);
	}

	@Test
	@DisplayName("실제 SMTP로 당첨 취소 이메일 1건 발송")
	@Disabled
	void notifyCancelUnpaid_success() {
		// given
		Member member = memberRepository.save(
			Member.builder()
				.email("codingworld@naver.com")
				.password(passwordEncoder.encode("1234567a!"))
				.name("실제발송테스트")
				.role(Member.Role.MEMBER)
				.provider(Member.Provider.EMAIL)
				.isEmailVerified(true)
				.isIdentityVerified(true)
				.build()
		);

		// 응모 생성
		createLotteryEntry(List.of(member), performance, schedule,
			SeatGradeType.STANDARD, 1, lotteryEntryRepository);

		entityManager.flush();
		drawService.executeDraws();

		List<LotteryResult> results = lotteryResultRepository.findAll();

		results.forEach(result -> {
			entityManager.createQuery("""
						UPDATE LotteryResult lr
						   SET lr.paymentDeadline = :expired
						 WHERE lr.id = :id
					""")
				.setParameter("expired", LocalDateTime.now().minusDays(1))
				.setParameter("id", result.getId())
				.executeUpdate();
		});

		entityManager.flush();
		entityManager.clear();

		// when
		drawService.executecancelUnpaid();
	}
}