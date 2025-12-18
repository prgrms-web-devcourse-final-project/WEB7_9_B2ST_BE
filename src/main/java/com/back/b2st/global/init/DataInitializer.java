package com.back.b2st.global.init;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.entity.PerformanceStatus;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.reservation.entity.ScheduleSeat;
import com.back.b2st.domain.reservation.repository.ScheduleSeatRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
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
public class DataInitializer implements CommandLineRunner {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final ScheduleSeatRepository scheduleSeatRepository;
	private final SectionRepository sectionRepository;
	private final SeatRepository seatRepository;
	private final VenueRepository venueRepository;
	private final PerformanceRepository performanceRepository;
	private final PerformanceScheduleRepository performanceScheduleRepository;

	private Venue venue;
	private Performance performance;
	private PerformanceSchedule performanceSchedule;

	@Override
	public void run(String... args) throws Exception {
		// 서버 재시작시 중복 생성 방지 차
		initMemberData();
		initPerformance();
		initSectionData();
		initSectData();
		//initConnectedSet(); // 테스트용
		//initConnectedSet2();
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
			.isVerified(true)
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
			.isVerified(true)
			.build();

		Member user2 = Member.builder()
			.email("user2@tt.com")
			.password(passwordEncoder.encode("1234567a!"))
			.name("유저이")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isVerified(true)
			.build();

		memberRepository.save(user1);
		memberRepository.save(user2);

		log.info("[DataInit] 계정 생성 완");
		log.info("   - 유저1 : user1@tt.com / 1234567a!");
		log.info("   - 유저2 : user2@tt.com / 1234567a!");
	}

	private void setAuthenticationContext(Member admin) {
		CustomUserDetails userDetails = new CustomUserDetails(admin);
		Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	// 공연장 - 공연 데이터 생성
	private void initPerformance() {
		if (!(venueRepository.count() > 0)) {
			venue = venueRepository.save(Venue.builder().name("잠실실내체육관").build());
		} else {
			log.info("[DataInit] 장소 데이터 초기화 스킵");
		}

		if (!(performanceRepository.count() < 0)) {
			performance = performanceRepository.save(Performance.builder()
				.venue(venue)
				.title("2024 아이유 콘서트 - HEREH WORLD TOUR")
				.category("콘서트")
				.posterUrl("")
				.description(null)
				.startDate(LocalDateTime.of(2024, 12, 20, 19, 0))
				.endDate(LocalDateTime.of(2024, 12, 22, 21, 0))
				.status(PerformanceStatus.ON_SALE)
				.build());

			// 회차
			performanceSchedule = performanceScheduleRepository.save(PerformanceSchedule.builder()
				.performance(performance)
				.startAt(LocalDateTime.of(2024, 12, 20, 19, 0))
				.roundNo(1)
				.bookingType(BookingType.LOTTERY)
				.bookingOpenAt(LocalDateTime.of(2024, 12, 10, 12, 0))
				.bookingCloseAt(LocalDateTime.of(2024, 12, 15, 23, 59))
				.build());
		} else {
			log.info("[DataInit] 공연 데이터 초기화 스킵");
		}

	}

	private void initSectionData() {
		if (sectionRepository.count() > 0) {
			log.info("[DataInit] 구역 데이터 초기화 스킵");
			return;
		}

		Long venueId1 = venue.getVenueId();
		Long venueId2 = 2L;

		Section section1A = Section.builder().venueId(venueId1).sectionName("A").build();

		Section section1B = Section.builder().venueId(venueId1).sectionName("B").build();

		Section section2A = Section.builder().venueId(venueId2).sectionName("A").build();

		Section sectionA = sectionRepository.save(section1A);
		Section sectionB = sectionRepository.save(section1B);
		sectionRepository.save(section2A);

		log.info("[DataInit/Test] Section initialized. count=3 (venueId=1[A,B], venueId=2[A])");
		log.info("[DataInit/Test] Section initialized. " + sectionA.getId());
	}

	private void initSectData() {
		if (seatRepository.count() > 0) {
			log.info("[DataInit] 좌석 데이터 초기화 스킵");
			return;
		}
		Long venueId = venue.getVenueId();
		List<Section> sections = sectionRepository.findByVenueId(venueId);
		Section section1A = sections.get(0);
		Section section1B = sections.get(1);

		List<Seat> seats = new ArrayList<>();

		for (int row = 1; row <= 3; row++) {
			for (int number = 1; number <= 5; number++) {
				seats.add(Seat.builder()
					.venueId(venueId)
					.sectionId(section1A.getId())
					.sectionName("A")
					.rowLabel(String.valueOf(row))
					.seatNumber(number)
					.build());
			}
		}

		for (int row = 1; row <= 3; row++) {
			for (int number = 1; number <= 5; number++) {
				seats.add(Seat.builder()
					.venueId(venueId)
					.sectionId(section1A.getId())
					.sectionName("B")
					.rowLabel(String.valueOf(row))
					.seatNumber(number)
					.build());
			}
		}

		seatRepository.saveAll(seats);

		log.info("[DataInit/Test] Seat initialized. count=25 (section=A11 ~ A115, A21 ~ A215, ... , A51 ~ A55");

	}

	private void initConnectedSet() {

		// 공연장 생성
		Venue connectedVenue = venueRepository.save(Venue.builder().name("QueryDSL 연결 테스트 공연장").build());

		// 공연 생성
		Performance connectedPerformance = performanceRepository.save(Performance.builder()
			.venue(connectedVenue)
			.title("QueryDSL 연결 테스트 공연")
			.category("콘서트")
			.posterUrl("")
			.description(null)
			.startDate(LocalDateTime.now().plusDays(5))
			.endDate(LocalDateTime.now().plusDays(7))
			.status(PerformanceStatus.ON_SALE)
			.build());

		// 공연 회차 생성
		PerformanceSchedule connectedSchedule = performanceScheduleRepository.save(PerformanceSchedule.builder()
			.performance(connectedPerformance)
			.roundNo(1)
			.startAt(LocalDateTime.now().plusDays(5))
			.bookingType(BookingType.FIRST_COME)
			.bookingOpenAt(LocalDateTime.now().minusDays(1))
			.bookingCloseAt(LocalDateTime.now().plusDays(3))
			.build());

		// 구역 생성
		Section connectedSection = sectionRepository.save(
			Section.builder().venueId(connectedVenue.getVenueId()).sectionName("A").build());

		// 좌석 생성
		Seat connectedSeat = seatRepository.save(Seat.builder()
			.venueId(connectedVenue.getVenueId())
			.sectionId(connectedSection.getId())
			.sectionName(connectedSection.getSectionName())
			.rowLabel("1")
			.seatNumber(1)
			.build());

		// 회차별 좌석 생성
		ScheduleSeat connectedScheduleSeat = scheduleSeatRepository.save(ScheduleSeat.builder()
			.scheduleId(connectedSchedule.getPerformanceScheduleId())
			.seatId(connectedSeat.getId())
			.build());

	}

	private void initConnectedSet2() {

		// 공연장 생성
		Venue venue = venueRepository.save(Venue.builder().name("QueryDSL 연결 테스트 공연장 2").build());

		// 공연 생성
		Performance performance = performanceRepository.save(Performance.builder()
			.venue(venue)
			.title("QueryDSL 연결 테스트 공연 2")
			.category("뮤지컬")
			.posterUrl("")
			.description(null)
			.startDate(LocalDateTime.now().plusDays(10))
			.endDate(LocalDateTime.now().plusDays(12))
			.status(PerformanceStatus.ON_SALE)
			.build());

		// 공연 회차 생성
		PerformanceSchedule schedule = performanceScheduleRepository.save(PerformanceSchedule.builder()
			.performance(performance)
			.roundNo(1)
			.startAt(LocalDateTime.now().plusDays(10))
			.bookingType(BookingType.FIRST_COME)
			.bookingOpenAt(LocalDateTime.now().minusDays(1))
			.bookingCloseAt(LocalDateTime.now().plusDays(8))
			.build());

		// 구역 생성
		Section section = sectionRepository.save(
			Section.builder().venueId(venue.getVenueId()).sectionName("A").build());

		// 좌석 생성
		Seat seat = seatRepository.save(Seat.builder()
			.venueId(venue.getVenueId())
			.sectionId(section.getId())
			.sectionName(section.getSectionName())
			.rowLabel("1")
			.seatNumber(1)
			.build());

		// 회차별 좌석 생성
		scheduleSeatRepository.save(
			ScheduleSeat.builder().scheduleId(schedule.getPerformanceScheduleId()).seatId(seat.getId()).build());
	}

}
