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

	private Venue venue;
	private Performance performance;

	@Override
	public void run(String... args) throws Exception {
		// 서버 재시작시 중복 생성 방지 차
		initMemberData();
		initPerformance();
		initSectionData();
		initSectData();
	}

	private void initMemberData() {
		if (memberRepository.count() > 0) {
			log.info("[DataInit] 이미 계정 존재하여 초기화 스킵");
			return;
		}

		log.info("[DataInit] 테스트 계정 데이터 생성");

		Member admin = Member.builder()
			.email("admin@tt.com")
			.password(passwordEncoder.encode("1234")) // 어드민, 유저 비번 전부 1234입니다
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
			.password(passwordEncoder.encode("1234"))
			.name("유저1")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isVerified(true)
			.build();

		Member user2 = Member.builder()
			.email("user2@tt.com")
			.password(passwordEncoder.encode("1234"))
			.name("유저2")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isVerified(true)
			.build();

		memberRepository.save(user1);
		memberRepository.save(user2);

		log.info("[DataInit] 계정 생성 완");
		log.info("   - 유저1 : user1@tt.com / 1234");
		log.info("   - 유저2 : user2@tt.com / 1234");

		ScheduleSeat testSeat1 = ScheduleSeat.builder()
			.scheduleId(1001L)
			.seatId(55L)
			.build();

		ScheduleSeat testSeat2 = ScheduleSeat.builder()
			.scheduleId(1000L)
			.seatId(54L)
			.build();

		scheduleSeatRepository.saveAll(List.of(testSeat1, testSeat2));
		log.info("[DataInit] 테스트용 좌석 생성 완료 → scheduleId=1001, seatId=55");
	}

	private void setAuthenticationContext(Member admin) {
		CustomUserDetails userDetails = new CustomUserDetails(admin);
		Authentication auth = new UsernamePasswordAuthenticationToken(
			userDetails,
			null,
			userDetails.getAuthorities()
		);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	// 공연장 - 공연 데이터 생성
	private void initPerformance() {
		if (!(venueRepository.count() > 0)) {
			Venue venue1 = Venue.builder()
				.name("잠실실내체육관")
				.build();

			venue = venueRepository.save(venue1);
		} else {
			log.info("[DataInit] 장소 데이터 초기화 스킵");
		}

		if (!(performanceRepository.count() < 0)) {
			Performance performance1 = Performance.builder()
				.venue(venue)
				.title("2024 아이유 콘서트 - HEREH WORLD TOUR")
				.category("콘서트")
				.posterUrl("")
				.description("아이유의 월드투어 서울 공연입니다. 최고의 무대와 감동을 선사합니다.")
				.startDate(LocalDateTime.of(2024, 12, 20, 19, 0))
				.endDate(LocalDateTime.of(2024, 12, 22, 21, 0))
				.status(PerformanceStatus.ON_SALE)
				.build();

			performance = performanceRepository.save(performance1);
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

		Section section1A = Section.builder()
			.venueId(venueId1)
			.sectionName("A")
			.build();

		Section section1B = Section.builder()
			.venueId(venueId1)
			.sectionName("B")
			.build();

		Section section2A = Section.builder()
			.venueId(venueId2)
			.sectionName("A")
			.build();

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
				seats.add(
					Seat.builder()
						.venueId(venueId)
						.sectionId(section1A.getId())
						.sectionName("A")
						.rowLabel(String.valueOf(row))
						.seatNumber(number)
						.build()
				);
			}
		}

		for (int row = 1; row <= 3; row++) {
			for (int number = 1; number <= 5; number++) {
				seats.add(
					Seat.builder()
						.venueId(venueId)
						.sectionId(section1A.getId())
						.sectionName("B")
						.rowLabel(String.valueOf(row))
						.seatNumber(number)
						.build()
				);
			}
		}

		seatRepository.saveAll(seats);

		log.info("[DataInit/Test] Seat initialized. count=25 (section=A11 ~ A115, A21 ~ A215, ... , A51 ~ A55");

	}
}
