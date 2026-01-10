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
import com.back.b2st.domain.lottery.entry.repository.LotteryEntryRepository;
import com.back.b2st.domain.lottery.result.repository.LotteryResultRepository;
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
@Transactional
class CancelUnpaidServiceTest {

	@Autowired
	private CancelUnpaidService cancelUnpaidService;

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

	List<Member> members1;
	Venue venue;
	Performance performance;
	List<PerformanceSchedule> schedules;
	List<Section> sections;
	List<Seat> seats;
	PerformanceSchedule performanceSchedule;

	List<LotteryEntry> lotteryEntries1;

	@BeforeEach
	void setUp() {
		// 사용자 등록 <- 여러명
		members1 = createMembers(10, memberRepository, passwordEncoder);

		venue = createVenue("당첨취소", venueRepository);
		performance = createPerformance(venue, performanceRepository);
		schedules = createSchedules(performance, 3, BookingType.LOTTERY,
			performanceScheduleRepository);
		performanceSchedule = schedules.getFirst();

		sections = createSections(venue.getVenueId(), sectionRepository, "A", "B", "C");
		seats = createSeats(venue.getVenueId(), sections, 3, 5, seatRepository);
		createSeatGrades(performance, seats, seatGradeRepository);

		lotteryEntries1 = createLotteryEntry(members1, performance, performanceSchedule, SeatGradeType.STANDARD,
			lotteryEntryRepository);

		createLotteryResult(lotteryEntries1, members1, lotteryResultRepository);
	}

	@Test
	@DisplayName("당첨취소")
	void cancelUnpaid() {
		// given
		LocalDateTime now = LocalDateTime.now();
		long before = lotteryResultRepository.countUnpaidAll(now);
		assertThat(before).isGreaterThan(0);

		// when
		cancelUnpaidService.cancelUnpaid();

		// then
		long after = lotteryResultRepository.countUnpaidAll(now);
		assertThat(after).isZero();
	}
}