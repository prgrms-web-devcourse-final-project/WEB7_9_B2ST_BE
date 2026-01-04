package com.back.b2st.domain.prereservation.entry;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.entity.PerformanceStatus;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.prereservation.entry.repository.PrereservationRepository;
import com.back.b2st.domain.prereservation.entry.service.PrereservationApplyService;
import com.back.b2st.domain.prereservation.entry.service.PrereservationSectionService;
import com.back.b2st.domain.prereservation.policy.entity.PrereservationTimeTable;
import com.back.b2st.domain.prereservation.policy.repository.PrereservationTimeTableRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.domain.venue.venue.repository.VenueRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PrereservationInitDataFlowTest {

	@Autowired
	private VenueRepository venueRepository;

	@Autowired
	private PerformanceRepository performanceRepository;

	@Autowired
	private PerformanceScheduleRepository performanceScheduleRepository;

	@Autowired
	private SectionRepository sectionRepository;

	@Autowired
	private PrereservationTimeTableRepository prereservationTimeTableRepository;

	@Autowired
	private PrereservationRepository prereservationRepository;

	@Autowired
	private PrereservationApplyService prereservationApplyService;

	@Autowired
	private PrereservationSectionService prereservationSectionService;

	@Test
	@DisplayName("신청예매(사전신청): PRERESERVE 회차에서 구역 조회/신청이 정상 동작한다")
	void prereservation_flow_success() {
		// given: initData와 동일한 형태(별도 공연 + PRERESERVE 회차 + 타임테이블)
		Venue venue = venueRepository.save(Venue.builder().name("신청예매 테스트 공연장").build());
		Performance performance = performanceRepository.save(Performance.builder()
			.venue(venue)
			.title("연극 - 신청예매 테스트")
			.category("연극")
			.startDate(LocalDateTime.now().minusDays(1))
			.endDate(LocalDateTime.now().plusDays(7))
			.status(PerformanceStatus.ACTIVE)
			.build());

		LocalDateTime bookingOpenAt = LocalDateTime.now().plusHours(1);
		LocalDateTime bookingCloseAt = bookingOpenAt.plusHours(3);

		PerformanceSchedule schedule = performanceScheduleRepository.save(PerformanceSchedule.builder()
			.performance(performance)
			.startAt(LocalDateTime.now().plusDays(2).withHour(19).withMinute(0).withSecond(0).withNano(0))
			.roundNo(1)
			.bookingType(BookingType.PRERESERVE)
			.bookingOpenAt(bookingOpenAt)
			.bookingCloseAt(bookingCloseAt)
			.build());

		Section sectionA = sectionRepository.save(Section.builder().venueId(venue.getVenueId()).sectionName("A").build());
		Section sectionB = sectionRepository.save(Section.builder().venueId(venue.getVenueId()).sectionName("B").build());

		prereservationTimeTableRepository.saveAll(List.of(
			PrereservationTimeTable.builder()
				.performanceScheduleId(schedule.getPerformanceScheduleId())
				.sectionId(sectionA.getId())
				.bookingStartAt(bookingOpenAt)
				.bookingEndAt(bookingOpenAt.plusHours(1).minusSeconds(1))
				.build(),
			PrereservationTimeTable.builder()
				.performanceScheduleId(schedule.getPerformanceScheduleId())
				.sectionId(sectionB.getId())
				.bookingStartAt(bookingOpenAt.plusHours(1))
				.bookingEndAt(bookingOpenAt.plusHours(2).minusSeconds(1))
				.build()
		));

		Long memberId = 1L;

		// when: 구역 조회 (신청 전)
		var sectionsBefore = prereservationSectionService.getSections(schedule.getPerformanceScheduleId(), memberId);

		// then
		assertThat(sectionsBefore).hasSize(2);
		assertThat(sectionsBefore).allSatisfy(s -> assertThat(s.applied()).isFalse());

		// when: A구역 신청
		prereservationApplyService.apply(schedule.getPerformanceScheduleId(), memberId, "", sectionA.getId());

		// then: 저장 확인
		assertThat(prereservationRepository.existsByPerformanceScheduleIdAndMemberIdAndSectionId(
			schedule.getPerformanceScheduleId(), memberId, sectionA.getId()
		)).isTrue();

		// when: 구역 조회 (신청 후)
		var sectionsAfter = prereservationSectionService.getSections(schedule.getPerformanceScheduleId(), memberId);

		// then: A만 applied=true
		assertThat(sectionsAfter).hasSize(2);
		assertThat(sectionsAfter.stream().filter(s -> s.sectionId().equals(sectionA.getId())).findFirst().orElseThrow().applied())
			.isTrue();
		assertThat(sectionsAfter.stream().filter(s -> s.sectionId().equals(sectionB.getId())).findFirst().orElseThrow().applied())
			.isFalse();
	}
}

