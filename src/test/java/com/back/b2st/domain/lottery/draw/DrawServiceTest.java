package com.back.b2st.domain.lottery.draw;

import static com.back.b2st.support.TestFixture.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.back.b2st.domain.lottery.entry.entity.LotteryEntry;
import com.back.b2st.domain.lottery.entry.repository.LotteryEntryRepository;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.seat.grade.entity.SeatGradeType;
import com.back.b2st.domain.seat.grade.repository.SeatGradeRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.domain.venue.venue.repository.VenueRepository;

@SpringBootTest
@ActiveProfiles("test")
class DrawServiceTest {

	@Autowired
	private DrawService drawService;

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

	List<Member> members;
	Venue venue;
	Performance performance;
	List<PerformanceSchedule> schedules;
	List<Section> sections;
	List<Seat> seats;
	PerformanceSchedule performanceSchedule;

	List<LotteryEntry> lotteryEntries1;
	List<LotteryEntry> lotteryEntries2;
	List<LotteryEntry> lotteryEntries3;

	@BeforeEach
	void setUp() {
		// 사용자 등록 <- 여러명
		List<Member> members1 = createMembers(10, memberRepository, passwordEncoder);
		List<Member> members2 = createMembers(10, memberRepository, passwordEncoder);
		List<Member> members3 = createMembers(10, memberRepository, passwordEncoder);

		venue = createVenue("잠실실내체육관", venueRepository);

		performance = createPerformance(venue, performanceRepository);

		schedules = createSchedules(performance, 5, BookingType.LOTTERY,
			performanceScheduleRepository);

		performanceSchedule = schedules.getFirst();

		sections = createSections(venue.getVenueId(), sectionRepository, "A", "B", "C");

		seats = createSeats(venue.getVenueId(), sections, 3, 5, seatRepository);

		createSeatGrades(performance, seats, seatGradeRepository);

		lotteryEntries1 = createLotteryEntry(members1, performance, performanceSchedule, SeatGradeType.STANDARD, 2,
			lotteryEntryRepository);
		lotteryEntries2 = createLotteryEntry(members2, performance, performanceSchedule, SeatGradeType.VIP, 2,
			lotteryEntryRepository);
		lotteryEntries3 = createLotteryEntry(members3, performance, performanceSchedule, SeatGradeType.ROYAL, 2,
			lotteryEntryRepository);

		System.out.println("============================== init data OK ==============================");
	}

	@Test
	void drawForPerformance() {
		drawService.drawForPerformance(performance.getPerformanceId(), performanceSchedule.getPerformanceScheduleId());
	}

}