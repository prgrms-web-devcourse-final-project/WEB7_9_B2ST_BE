/*
package com.back.b2st.domain.lottery.draw.service;

import static com.back.b2st.support.TestFixture.*;
import static org.assertj.core.api.Assertions.*;

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
import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
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
	private ScheduleSeatRepository scheduleSeatRepository;

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

	// LotteryResult를 paid=true로 변경
	private void setPaidTrue() {
		List<LotteryResult> results = lotteryResultRepository.findAll();

		results.forEach(result -> {
			result.confirmPayment();
		});

		entityManager.flush();
	}

	// 추첨 실행 및 결제 완료 처리
	private void executeDrawAndPay() {
		entityManager.flush();
		drawService.executeDraws();
		setPaidTrue();
		entityManager.flush();
	}

	@Test
	@DisplayName("좌석 배정 성공")
	void allocateSeats_Success() {
		// given
		createLotteryEntry(members1.subList(0, 1), performance, schedule,
			SeatGradeType.STANDARD, 4, lotteryEntryRepository);
		executeDrawAndPay();

		List<LotteryReservationInfo> infos = seatAllocationService.findReservationInfos();
		assertThat(infos).isNotEmpty();
		LotteryReservationInfo info = infos.get(0);

		// when - 좌석 배정
		List<ScheduleSeat> allocatedSeats = seatAllocationService.allocateSeatsForLottery(info);
		entityManager.flush();
		entityManager.clear();

		// then - DB에서 재조회하여 검증
		List<Long> allocatedSeatIds = allocatedSeats.stream()
			.map(seat -> scheduleSeatRepository
				.findByScheduleIdAndSeatId(seat.getScheduleId(), seat.getSeatId())
				.orElseThrow()
				.getId())
			.toList();

		List<ScheduleSeat> seatsFromDB = scheduleSeatRepository.findAllById(allocatedSeatIds);

		assertThat(seatsFromDB).hasSize(4);
		assertThat(seatsFromDB).allMatch(seat -> seat.getStatus() == SeatStatus.SOLD);
		assertThat(seatsFromDB).allMatch(seat -> seat.getScheduleId().equals(info.scheduleId()));
	}

	@Test
	@DisplayName("좌석 배정 실패 - 좌석 부족")
	void allocateSeatsForLottery_InsufficientSeats() {
		// given - 당첨은 되도록 적게 신청
		createLotteryEntry(members1.subList(0, 1), performance, schedule,
			SeatGradeType.STANDARD, 3, lotteryEntryRepository);
		executeDrawAndPay();

		List<LotteryReservationInfo> infos = seatAllocationService.findReservationInfos();
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
		createLotteryEntry(members1.subList(0, 1), performance, schedule,
			SeatGradeType.STANDARD, 3, lotteryEntryRepository);
		executeDrawAndPay();

		entityManager.clear();
		int availableBefore = scheduleSeatRepository
			.findAvailableSeatsByGrade(schedule.getPerformanceScheduleId(), SeatGradeType.STANDARD)
			.size();

		List<LotteryReservationInfo> infos = seatAllocationService.findReservationInfos();
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
		createLotteryEntry(members1.subList(0, 3), performance, schedule,
			SeatGradeType.STANDARD, 2, lotteryEntryRepository);
		executeDrawAndPay();

		List<LotteryReservationInfo> infos = seatAllocationService.findReservationInfos();
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
		createLotteryEntry(members1.subList(0, 1), performance, schedule,
			SeatGradeType.ROYAL, 2, lotteryEntryRepository);

		entityManager.flush();
		entityManager.clear();

		drawService.executeDraws();
		setPaidTrue();

		entityManager.flush();
		entityManager.clear();

		// when & then - DB에서 검증
		List<LotteryReservationInfo> infos = seatAllocationService.findReservationInfos();
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

		createLotteryEntry(members1.subList(0, 1), performance, schedule,
			SeatGradeType.STANDARD, 3, lotteryEntryRepository);
		executeDrawAndPay();

		List<LotteryReservationInfo> infos = seatAllocationService.findReservationInfos();
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
	@DisplayName("좌석 배정 - 여러 등급 혼합")
	void allocateSeatsForLottery_MultipleGrades() {
		// given - VIP와 STANDARD 각각 응모
		createLotteryEntry(members1.subList(0, 1), performance, schedule,
			SeatGradeType.VIP, 2, lotteryEntryRepository);
		createLotteryEntry(members1.subList(1, 2), performance, schedule,
			SeatGradeType.STANDARD, 2, lotteryEntryRepository);

		executeDrawAndPay();

		List<LotteryReservationInfo> infos = seatAllocationService.findReservationInfos();
		assertThat(infos).hasSize(2);

		// when - 각 등급별 배정
		infos.forEach(info -> seatAllocationService.allocateSeatsForLottery(info));

		entityManager.flush();
		entityManager.clear();

		// then - DB에서 검증
		List<ScheduleSeat> soldSeatsFromDB = scheduleSeatRepository.findAll().stream()
			.filter(seat -> seat.getStatus() == SeatStatus.SOLD)
			.toList();

		assertThat(soldSeatsFromDB).hasSize(4); // VIP 2장 + STANDARD 2장

		// 각 등급별로 올바른 좌석이 배정되었는지 확인
		soldSeatsFromDB.forEach(scheduleSeat -> {
			SeatGrade seatGrade = seatGradeRepository.findAll().stream()
				.filter(sg -> sg.getSeatId().equals(scheduleSeat.getSeatId()))
				.filter(sg -> sg.getPerformanceId().equals(performance.getPerformanceId()))
				.findFirst()
				.orElseThrow();

			// VIP 또는 STANDARD 등급이어야 함
			assertThat(seatGrade.getGrade())
				.isIn(SeatGradeType.VIP, SeatGradeType.STANDARD);
		});
	}
}*/
