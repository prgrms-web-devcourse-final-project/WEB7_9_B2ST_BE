package com.back.b2st.support;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.back.b2st.domain.lottery.entry.entity.LotteryEntry;
import com.back.b2st.domain.lottery.entry.repository.LotteryEntryRepository;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.entity.PerformanceStatus;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.seat.grade.entity.SeatGrade;
import com.back.b2st.domain.seat.grade.entity.SeatGradeType;
import com.back.b2st.domain.seat.grade.repository.SeatGradeRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.domain.venue.venue.repository.VenueRepository;

public class TestFixture {

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
					.bookingOpenAt(LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.MIDNIGHT))
					.bookingCloseAt(LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(12, 0)))
					.build()
				)
				.toList()
		);
	}

	public static List<PerformanceSchedule> createSchedulesSeatAllocation(
		Performance performance,
		int count,
		PerformanceScheduleRepository repo
	) {
		return repo.saveAll(
			IntStream.rangeClosed(1, count)
				.mapToObj(i -> PerformanceSchedule.builder()
					.performance(performance)
					.roundNo(i)
					.startAt(LocalDate.now().plusDays(3).atTime(10, 0))
					.bookingType(BookingType.LOTTERY)
					.bookingOpenAt(LocalDate.now().minusDays(5).atStartOfDay())
					.bookingCloseAt(LocalDate.now().minusDays(1).atTime(12, 0))
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
					.grade(switch (group) {
						case 0 -> SeatGradeType.VIP;
						case 1 -> SeatGradeType.ROYAL;
						default -> SeatGradeType.STANDARD;
					})
					.price(switch (group) {
						case 0 -> 30000;
						case 1 -> 20000;
						default -> 10000;
					})
					.build();
			})
			.toList();

		repo.saveAll(grades);
	}

	public static void createSeatGrades(
		Performance performance,
		List<Seat> seats,
		SeatGradeType gradeType,
		Integer price,
		SeatGradeRepository repo
	) {
		List<SeatGrade> grades = IntStream.range(0, seats.size())
			.mapToObj(i -> {
				int group = (i % 15) / 5;
				return SeatGrade.builder()
					.performanceId(performance.getPerformanceId())
					.seatId(seats.get(i).getId())
					.grade(gradeType)
					.price(price)
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

	public static List<LotteryEntry> createLotteryEntry(
		List<Member> members,
		Performance performance,
		PerformanceSchedule performanceSchedule,
		SeatGradeType seatGradeType,
		Integer quantity,
		LotteryEntryRepository repo
	) {
		List<LotteryEntry> lotteryEntries = IntStream.range(0, members.size())
			.mapToObj(i -> {
				return LotteryEntry.builder()
					.memberId(members.get(i).getId())
					.performanceId(performance.getPerformanceId())
					.scheduleId(performanceSchedule.getPerformanceScheduleId())
					.grade(seatGradeType)
					.quantity(quantity)
					.build();
			}).toList();

		return repo.saveAll(lotteryEntries);
	}

	/**
	 * ScheduleSeat 생성
	 * createScheduleSeats(scheduleId, seats, scheduleSeatRepository);
	 */
	public static List<ScheduleSeat> createScheduleSeats(
		Long scheduleId,
		List<Seat> seats,
		ScheduleSeatRepository repo
	) {
		return repo.saveAll(
			seats.stream()
				.map(seat -> ScheduleSeat.builder()
					.scheduleId(scheduleId)
					.seatId(seat.getId())
					.build()
				)
				.toList()
		);
	}

}
