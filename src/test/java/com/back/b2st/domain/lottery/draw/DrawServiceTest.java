package com.back.b2st.domain.lottery.draw;

import static com.back.b2st.support.TestFixture.*;
import static org.assertj.core.api.Assertions.*;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

	List<Member> members1;
	List<Member> members2;
	List<Member> members3;
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
		members1 = createMembers(10, memberRepository, passwordEncoder);
		members2 = createMembers(10, memberRepository, passwordEncoder);
		members3 = createMembers(10, memberRepository, passwordEncoder);

		venue = createVenue("잠실실내체육관", venueRepository);
		performance = createPerformance(venue, performanceRepository);
		schedules = createSchedules(performance, 20, BookingType.LOTTERY,
			performanceScheduleRepository);
		performanceSchedule = schedules.getFirst();

		sections = createSections(venue.getVenueId(), sectionRepository, "A", "B", "C");
		seats = createSeats(venue.getVenueId(), sections, 3, 5, seatRepository);
		createSeatGrades(performance, seats, seatGradeRepository);

		lotteryEntries1 = createLotteryEntry(members1, performance, performanceSchedule, SeatGradeType.STANDARD,
			lotteryEntryRepository);
		lotteryEntries2 = createLotteryEntry(members2, performance, performanceSchedule, SeatGradeType.VIP,
			lotteryEntryRepository);
		lotteryEntries3 = createLotteryEntry(members3, performance, performanceSchedule, SeatGradeType.ROYAL,
			lotteryEntryRepository);

		System.out.println("============================== init data OK ==============================");
	}

	private Map<SeatGradeType, Long> getSeatCountsByGrade(Performance performance) {
		Map<SeatGradeType, Long> result = new EnumMap<>(SeatGradeType.class);

		seatGradeRepository.countSeatGradesByGrade(performance.getPerformanceId())
			.forEach(dto -> result.put(dto.garde(), dto.count()));

		// 없는 등급은 0으로 초기화
		for (SeatGradeType grade : SeatGradeType.values()) {
			result.putIfAbsent(grade, 0L);
		}

		return result;
	}

	@Test
	@DisplayName("응모자 > 좌석")
	void drawForPerformance() {
		// given : STANDARD 10명, VIP 10명, ROYAL 10명 응모
		Map<SeatGradeType, Long> seatCountsByGrade = getSeatCountsByGrade(performance);

		// when
		drawService.executeDraws();

		// then
		List<LotteryEntry> allEntries = lotteryEntryRepository.findAll();

		// 상태별 그룹핑
		Map<LotteryStatus, List<LotteryEntry>> entriesByStatus = allEntries.stream()
			.collect(Collectors.groupingBy(LotteryEntry::getStatus));

		List<LotteryEntry> winEntries = entriesByStatus.getOrDefault(LotteryStatus.WIN, List.of());
		List<LotteryEntry> loseEntries = entriesByStatus.getOrDefault(LotteryStatus.LOSE, List.of());

		System.out.println("=== 추첨 결과 ===");
		System.out.println("전체 응모: " + allEntries.size());
		System.out.println("당첨: " + winEntries.size());
		System.out.println("낙첨: " + loseEntries.size());

		// 전체 응모자 수 확인
		assertThat(allEntries.size()).isEqualTo(30);
		assertThat(winEntries.size() + loseEntries.size()).isEqualTo(30);

		// 등급별 검증
		Map<SeatGradeType, List<LotteryEntry>> winnersByGrade = winEntries.stream()
			.collect(Collectors.groupingBy(LotteryEntry::getGrade));

		winnersByGrade.forEach((grade, entries) -> {
			// 등급별 좌석 수
			Long totalSeats = (long)entries.stream().mapToInt(LotteryEntry::getQuantity).sum();

			// 등급별 사용 가능 좌석 수
			Long availableSeats = seatCountsByGrade.get(grade);

			System.out.println(grade + " 당첨: " + entries.size() + "명, 총 좌석: " + totalSeats);

			assertThat(entries).isNotEmpty();
			assertThat(totalSeats).isLessThanOrEqualTo(availableSeats);
		});

		// 추첨 완료 상태 확인
		PerformanceSchedule result = performanceScheduleRepository
			.findById(performanceSchedule.getPerformanceScheduleId()).orElseThrow();

		assertThat(result.isDrawCompleted()).isTrue();
	}

	@Test
	@DisplayName("응모자 <= 좌석")
	void drawForPerformance_LessApplicantsThanSeats() {
		// given STANDARD 3명
		List<PerformanceSchedule> newSchedules = createSchedules(performance, 1, BookingType.LOTTERY,
			performanceScheduleRepository);
		PerformanceSchedule newSchedule = newSchedules.getFirst();

		List<Member> fewMembers = members1.subList(0, 3);
		createLotteryEntry(fewMembers, performance, newSchedule, SeatGradeType.STANDARD,
			lotteryEntryRepository);

		// when
		drawService.executeDraws();

		// then
		List<LotteryEntry> winEntries = lotteryEntryRepository.findAll().stream()
			.filter(e -> e.getScheduleId().equals(newSchedule.getPerformanceScheduleId()))
			.filter(e -> e.getStatus() == LotteryStatus.WIN)
			.toList();

		assertThat(winEntries).hasSize(fewMembers.size());
	}

	@Test
	@DisplayName("응모자 없음")
	void drawForPerformance_NoApplicants() {
		// given - 새로운 회차 생성 (응모자 없음)
		List<PerformanceSchedule> emptySchedules = createSchedules(performance, 1, BookingType.LOTTERY,
			performanceScheduleRepository);
		PerformanceSchedule emptySchedule = emptySchedules.getFirst();

		// when
		drawService.executeDraws();

		// then
		List<LotteryEntry> entries = lotteryEntryRepository.findAll().stream()
			.filter(e -> e.getScheduleId().equals(emptySchedule.getPerformanceScheduleId()))
			.toList();
		assertThat(entries).isEmpty();

		PerformanceSchedule result = performanceScheduleRepository
			.findById(performanceSchedule.getPerformanceScheduleId()).orElseThrow();
		assertThat(result.isDrawCompleted()).isTrue();
	}

	@Test
	@DisplayName("중복 당첨자")
	void drawForPerformance_NoDuplicateWinners() {
		// when
		drawService.executeDraws();

		// then
		List<LotteryEntry> winEntries = lotteryEntryRepository.findAll().stream()
			.filter(e -> e.getStatus() == LotteryStatus.WIN)
			.toList();

		List<Long> winnerIds = winEntries.stream()
			.map(LotteryEntry::getId)
			.toList();

		// 중복 체크
		assertThat(winnerIds).doesNotHaveDuplicates();
	}

	@Test
	@DisplayName("가중치")
	@Disabled
	void drawForPerformance_WeightByQuantity() {
		// given - 새로운 회차 생성
		List<PerformanceSchedule> newSchedules = createSchedules(performance, 1, BookingType.LOTTERY,
			performanceScheduleRepository);
		PerformanceSchedule newSchedule = newSchedules.getFirst();

		// 1장 신청자 50명
		List<Member> oneTicketMembers = createMembers(50, memberRepository, passwordEncoder);
		oneTicketMembers.forEach(member ->
			lotteryEntryRepository.save(LotteryEntry.builder()
				.performanceId(performance.getPerformanceId())
				.scheduleId(newSchedule.getPerformanceScheduleId())
				.memberId(member.getId())
				.grade(SeatGradeType.VIP)
				.quantity(1)
				.build())
		);

		// 4장 신청자 50명
		List<Member> fourTicketMembers = createMembers(50, memberRepository, passwordEncoder);
		fourTicketMembers.forEach(member ->
			lotteryEntryRepository.save(LotteryEntry.builder()
				.performanceId(performance.getPerformanceId())
				.scheduleId(newSchedule.getPerformanceScheduleId())
				.memberId(member.getId())
				.grade(SeatGradeType.VIP)
				.quantity(4)
				.build())
		);

		// when - 여러 번 추첨하여 통계 확인
		int iterations = 100;
		int oneTicketWinCount = 0;
		int fourTicketWinCount = 0;

		for (int i = 0; i < iterations; i++) {
			// 상태 초기화 (APPLIED로 리셋)
			lotteryEntryRepository.findAll().stream()
				.filter(e -> e.getScheduleId().equals(newSchedule.getPerformanceScheduleId()))
				.forEach(entry -> {
					entry.setApplied();
					lotteryEntryRepository.save(entry);
				});

			// 추첨 실행
			drawService.executeDraws();

			// 당첨자 집계
			List<LotteryEntry> winEntries = lotteryEntryRepository.findAll().stream()
				.filter(e -> e.getScheduleId().equals(newSchedule.getPerformanceScheduleId()))
				.filter(e -> e.getStatus() == LotteryStatus.WIN)
				.toList();

			long oneTicketWins = winEntries.stream()
				.filter(e -> e.getQuantity() == 1)
				.count();

			long fourTicketWins = winEntries.stream()
				.filter(e -> e.getQuantity() == 4)
				.count();

			oneTicketWinCount += oneTicketWins;
			fourTicketWinCount += fourTicketWins;
		}

		// then
		System.out.println("=== 가중치 반복 테스트 (총 " + iterations + "회) ===");
		System.out.println("1장 신청자 총 당첨 횟수: " + oneTicketWinCount);
		System.out.println("4장 신청자 총 당첨 횟수: " + fourTicketWinCount);
		System.out.println("당첨 비율: " + String.format("%.2f", (double)oneTicketWinCount / fourTicketWinCount) + ":1");

		// 1장 신청자의 당첨 횟수가 4장 신청자보다 확실히 많아야 함
		// 가중치가 12:3 = 4:1 이므로, 최소 2배 이상 차이
		assertThat(oneTicketWinCount).isGreaterThan(fourTicketWinCount * 2);
	}

	@Test
	@DisplayName("좌석 없음")
	void drawForPerformance_NoSeats() {
		// given - 좌석이 없는 새 공연 생성
		Venue newVenue = createVenue("좌석없는공연장", venueRepository);
		Performance noSeatPerformance = createPerformance(newVenue, performanceRepository);

		List<PerformanceSchedule> newSchedules = createSchedules(noSeatPerformance, 1, BookingType.LOTTERY,
			performanceScheduleRepository);
		PerformanceSchedule newSchedule = newSchedules.getFirst();

		// VIP 응모자 10명 (하지만 좌석은 0개)
		List<Member> applicants = createMembers(10, memberRepository, passwordEncoder);
		applicants.forEach(member ->
			lotteryEntryRepository.save(LotteryEntry.builder()
				.scheduleId(newSchedule.getPerformanceScheduleId())
				.performanceId(noSeatPerformance.getPerformanceId())
				.memberId(member.getId())
				.grade(SeatGradeType.VIP)
				.quantity(1)
				.build())
		);

		// when
		drawService.executeDraws();

		// then
		List<LotteryEntry> winEntries = lotteryEntryRepository.findAll().stream()
			.filter(e -> e.getScheduleId().equals(newSchedule.getPerformanceScheduleId()))
			.filter(e -> e.getStatus() == LotteryStatus.WIN)
			.toList();

		System.out.println("=== 좌석 없음 ===");
		System.out.println("응모자: 10명, 당첨자: " + winEntries.size() + "명");

		assertThat(winEntries).isEmpty();
	}

	@Test
	@DisplayName("남은 좌석 < 신청 좌석")
	void drawForPerformance_QuantityExceedsRemaining() {
		// given - 새 회차 생성
		List<PerformanceSchedule> newSchedules = createSchedules(performance, 1, BookingType.LOTTERY,
			performanceScheduleRepository);
		PerformanceSchedule newSchedule = newSchedules.getFirst();

		// ROYAL 좌석 수 확인
		Map<SeatGradeType, Long> seatCounts = getSeatCountsByGrade(performance);
		Long royalSeats = seatCounts.get(SeatGradeType.ROYAL);

		System.out.println("ROYAL 좌석 수: " + royalSeats);

		// 4장씩만 신청 (좌석 수보다 많은 신청자)
		int applicantCount = (int)(royalSeats / 4) + 10;
		List<Member> fourTicketMembers = createMembers(applicantCount, memberRepository, passwordEncoder);

		fourTicketMembers.forEach(member ->
			lotteryEntryRepository.save(LotteryEntry.builder()
				.scheduleId(newSchedule.getPerformanceScheduleId())
				.performanceId(performance.getPerformanceId())
				.memberId(member.getId())
				.grade(SeatGradeType.ROYAL)
				.quantity(4)
				.build())
		);

		// when
		drawService.executeDraws();

		// then
		List<LotteryEntry> winEntries = lotteryEntryRepository.findAll().stream()
			.filter(e -> e.getScheduleId().equals(newSchedule.getPerformanceScheduleId()))
			.filter(e -> e.getStatus() == LotteryStatus.WIN)
			.toList();

		int totalAssignedSeats = winEntries.stream()
			.mapToInt(LotteryEntry::getQuantity)
			.sum();

		System.out.println("=== 부분 당첨 방지 ===");
		System.out.println("전체 좌석: " + royalSeats);
		System.out.println("배정 좌석: " + totalAssignedSeats);
		System.out.println("당첨자: " + winEntries.size() + "명");

		// 배정된 좌석이 전체 좌석을 초과하지 않음
		assertThat((long)totalAssignedSeats).isLessThanOrEqualTo(royalSeats);

		// 모든 당첨자는 4장 받음 (부분 당첨 없음)
		assertThat(winEntries).allMatch(e -> e.getQuantity() == 4);
	}
}