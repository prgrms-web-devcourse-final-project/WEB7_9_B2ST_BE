package com.back.b2st.domain.prereservation.entry.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.back.b2st.domain.email.service.EmailSender;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.prereservation.entry.dto.response.PrereservationRes;
import com.back.b2st.domain.prereservation.entry.entity.Prereservation;
import com.back.b2st.domain.prereservation.entry.error.PrereservationErrorCode;
import com.back.b2st.domain.prereservation.entry.repository.PrereservationRepository;
import com.back.b2st.domain.prereservation.policy.service.PrereservationSlotService;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class PrereservationApplyServiceTest {

	@Mock
	private PerformanceScheduleRepository performanceScheduleRepository;

	@Mock
	private SectionRepository sectionRepository;

	@Mock
	private PrereservationRepository prereservationRepository;

	@Mock
	private PrereservationSlotService prereservationSlotService;

	@Mock
	private EmailSender emailSender;

	@InjectMocks
	private PrereservationApplyService prereservationApplyService;

	private static final Long MEMBER_ID = 1L;
	private static final Long SCHEDULE_ID = 10L;
	private static final Long SECTION_ID = 7L;

	@Test
	@DisplayName("apply(): 공연 회차가 없으면 SCHEDULE_NOT_FOUND 예외")
	void apply_scheduleNotFound_throw() {
		// given
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> prereservationApplyService.apply(SCHEDULE_ID, MEMBER_ID, "", SECTION_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.SCHEDULE_NOT_FOUND.getMessage());
	}

	@Test
	@DisplayName("apply(): BookingType이 PRERESERVE가 아니면 BOOKING_TYPE_NOT_SUPPORTED 예외")
	void apply_wrongType() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(schedule.getBookingType()).willReturn(BookingType.FIRST_COME);
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		// when & then
		assertThatThrownBy(() -> prereservationApplyService.apply(SCHEDULE_ID, MEMBER_ID, "", SECTION_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.BOOKING_TYPE_NOT_SUPPORTED.getMessage());
	}

	@Test
	@DisplayName("apply(): 예매 오픈 시간이 없으면 BOOKING_TIME_NOT_CONFIGURED 예외")
	void apply_bookingTimeNotConfigured_throw() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		// when & then
		assertThatThrownBy(() -> prereservationApplyService.apply(SCHEDULE_ID, MEMBER_ID, "", SECTION_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.BOOKING_TIME_NOT_CONFIGURED.getMessage());
	}

	@Test
	@DisplayName("apply(): 신청 시작 시간 전이면 APPLICATION_NOT_OPEN 예외")
	void apply_beforeApplyOpen_throw() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.now().plusDays(2));
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		// when & then
		assertThatThrownBy(() -> prereservationApplyService.apply(SCHEDULE_ID, MEMBER_ID, "", SECTION_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.APPLICATION_NOT_OPEN.getMessage());
	}

	@Test
	@DisplayName("apply(): 예매 오픈 시간 이후면 APPLICATION_CLOSED 예외")
	void apply_afterBookingOpen_throw() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.now().minusMinutes(1));
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		// when & then
		assertThatThrownBy(() -> prereservationApplyService.apply(SCHEDULE_ID, MEMBER_ID, "", SECTION_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.APPLICATION_CLOSED.getMessage());
	}

	@Test
	@DisplayName("apply(): 구역이 없으면 SECTION_NOT_FOUND 예외")
	void apply_sectionNotFound_throw() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.now().plusMinutes(10));
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> prereservationApplyService.apply(SCHEDULE_ID, MEMBER_ID, "", SECTION_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.SECTION_NOT_FOUND.getMessage());
	}

	@Test
	@DisplayName("apply(): 구역이 공연장에 속하지 않으면 SECTION_NOT_IN_VENUE 예외")
	void apply_sectionNotInVenue_throw() {
		// given
		Long venueId = 100L;
		Long otherVenueId = 200L;

		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		Performance performance = mock(Performance.class);
		Venue venue = mock(Venue.class);
		Section section = mock(Section.class);

		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.now().plusMinutes(10));
		given(schedule.getPerformance()).willReturn(performance);
		given(performance.getVenue()).willReturn(venue);
		given(venue.getVenueId()).willReturn(venueId);

		given(section.getVenueId()).willReturn(otherVenueId);

		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(section));

		// when & then
		assertThatThrownBy(() -> prereservationApplyService.apply(SCHEDULE_ID, MEMBER_ID, "", SECTION_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.SECTION_NOT_IN_VENUE.getMessage());
	}

	@Test
	@DisplayName("apply(): 이미 신청한 구역이면 DUPLICATE_APPLICATION 예외")
	void apply_alreadyApplied_throw() {
		// given
		Long venueId = 100L;

		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		Performance performance = mock(Performance.class);
		Venue venue = mock(Venue.class);
		Section section = mock(Section.class);

		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.now().plusMinutes(10));
		given(schedule.getPerformance()).willReturn(performance);
		given(performance.getVenue()).willReturn(venue);
		given(venue.getVenueId()).willReturn(venueId);

		given(section.getVenueId()).willReturn(venueId);

		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(section));
		given(prereservationRepository.existsByPerformanceScheduleIdAndMemberIdAndSectionId(
			SCHEDULE_ID, MEMBER_ID, SECTION_ID
		)).willReturn(true);

		// when & then
		assertThatThrownBy(() -> prereservationApplyService.apply(SCHEDULE_ID, MEMBER_ID, "", SECTION_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.DUPLICATE_APPLICATION.getMessage());
	}

	@Test
	@DisplayName("apply(): 정상적으로 신청이 저장된다(이메일 없음)")
	void apply_success() {
		// given
		Long venueId = 100L;

		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		Performance performance = mock(Performance.class);
		Venue venue = mock(Venue.class);
		Section section = mock(Section.class);

		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.now().plusMinutes(10));
		given(schedule.getPerformance()).willReturn(performance);
		given(performance.getVenue()).willReturn(venue);
		given(venue.getVenueId()).willReturn(venueId);

		given(section.getVenueId()).willReturn(venueId);

		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(section));
		given(prereservationRepository.existsByPerformanceScheduleIdAndMemberIdAndSectionId(
			SCHEDULE_ID, MEMBER_ID, SECTION_ID
		)).willReturn(false);

		// when & then
		assertThatCode(() -> prereservationApplyService.apply(SCHEDULE_ID, MEMBER_ID, "", SECTION_ID))
			.doesNotThrowAnyException();

		then(prereservationRepository).should().save(any(Prereservation.class));
		then(emailSender).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("apply(): 정상적으로 신청이 저장된다(이메일 발송)")
	void apply_successWithEmail_sendsEmail() {
		// given
		Long venueId = 100L;
		String email = "user@example.com";

		LocalDateTime bookingOpenAt = LocalDateTime.now().plusMinutes(10);
		LocalDateTime slotStartAt = bookingOpenAt.plusHours(1);
		LocalDateTime slotEndAt = bookingOpenAt.plusHours(2);

		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		Performance performance = mock(Performance.class);
		Venue venue = mock(Venue.class);
		Section section = mock(Section.class);

		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);
		given(schedule.getBookingOpenAt()).willReturn(bookingOpenAt);
		given(schedule.getPerformance()).willReturn(performance);
		given(performance.getVenue()).willReturn(venue);
		given(venue.getVenueId()).willReturn(venueId);

		given(section.getVenueId()).willReturn(venueId);
		given(section.getSectionName()).willReturn("A구역");

		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(section));
		given(prereservationRepository.existsByPerformanceScheduleIdAndMemberIdAndSectionId(
			SCHEDULE_ID, MEMBER_ID, SECTION_ID
		)).willReturn(false);
		given(prereservationSlotService.calculateSlotOrThrow(schedule, section))
			.willReturn(new PrereservationSlotService.Slot(slotStartAt, slotEndAt));

		// when
		prereservationApplyService.apply(SCHEDULE_ID, MEMBER_ID, email, SECTION_ID);

		// then
		then(prereservationRepository).should().save(any(Prereservation.class));
		then(emailSender).should().sendNotificationEmail(eq(email), eq("[TT] 신청 예매 사전 신청 완료"), anyString());
	}

	@Test
	@DisplayName("apply(): 저장 중 중복이면 DUPLICATE_APPLICATION 예외(DataIntegrityViolation)")
	void apply_duplicateOnSave_throw() {
		// given
		Long venueId = 100L;

		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		Performance performance = mock(Performance.class);
		Venue venue = mock(Venue.class);
		Section section = mock(Section.class);

		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.now().plusMinutes(10));
		given(schedule.getPerformance()).willReturn(performance);
		given(performance.getVenue()).willReturn(venue);
		given(venue.getVenueId()).willReturn(venueId);

		given(section.getVenueId()).willReturn(venueId);

		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(section));
		given(prereservationRepository.existsByPerformanceScheduleIdAndMemberIdAndSectionId(
			SCHEDULE_ID, MEMBER_ID, SECTION_ID
		)).willReturn(false);
		given(prereservationRepository.save(any(Prereservation.class)))
			.willThrow(new DataIntegrityViolationException("duplicate"));

		// when & then
		assertThatThrownBy(() -> prereservationApplyService.apply(SCHEDULE_ID, MEMBER_ID, "", SECTION_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.DUPLICATE_APPLICATION.getMessage());
	}

	@Test
	@DisplayName("getMyApplications(): 신청한 구역 목록을 조회한다")
	void getMyApplications_success() {
		// given
		Prereservation prereservation1 = mock(Prereservation.class);
		Prereservation prereservation2 = mock(Prereservation.class);

		given(prereservation1.getSectionId()).willReturn(1L);
		given(prereservation2.getSectionId()).willReturn(2L);

		given(prereservationRepository.findAllByPerformanceScheduleIdAndMemberIdOrderByCreatedAtDesc(
			SCHEDULE_ID, MEMBER_ID
		)).willReturn(java.util.List.of(prereservation1, prereservation2));

		// when
		PrereservationRes result = prereservationApplyService.getMyApplications(SCHEDULE_ID, MEMBER_ID);

		// then
		assertThat(result.scheduleId()).isEqualTo(SCHEDULE_ID);
		assertThat(result.sectionIds()).containsExactly(1L, 2L);
	}

	@Test
	@DisplayName("getMyApplicationList(): 전체 회차별 신청 구역 목록을 조회한다")
	void getMyApplicationList_success() {
		// given
		Prereservation prereservation1 = mock(Prereservation.class);
		Prereservation prereservation2 = mock(Prereservation.class);
		Prereservation prereservation3 = mock(Prereservation.class);

		given(prereservation1.getPerformanceScheduleId()).willReturn(10L);
		given(prereservation1.getSectionId()).willReturn(1L);

		given(prereservation2.getPerformanceScheduleId()).willReturn(10L);
		given(prereservation2.getSectionId()).willReturn(2L);

		given(prereservation3.getPerformanceScheduleId()).willReturn(20L);
		given(prereservation3.getSectionId()).willReturn(3L);

		given(prereservationRepository.findAllByMemberIdOrderByCreatedAtDesc(MEMBER_ID))
			.willReturn(java.util.List.of(prereservation1, prereservation2, prereservation3));

		// when
		var result = prereservationApplyService.getMyApplicationList(MEMBER_ID);

		// then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).scheduleId()).isEqualTo(10L);
		assertThat(result.get(0).sectionIds()).containsExactly(1L, 2L);
		assertThat(result.get(1).scheduleId()).isEqualTo(20L);
		assertThat(result.get(1).sectionIds()).containsExactly(3L);
	}

	@Test
	@DisplayName("getMyApplicationList(): 신청 내역이 없으면 빈 리스트를 반환한다")
	void getMyApplicationList_empty() {
		// given
		given(prereservationRepository.findAllByMemberIdOrderByCreatedAtDesc(MEMBER_ID))
			.willReturn(java.util.List.of());

		// when
		var result = prereservationApplyService.getMyApplicationList(MEMBER_ID);

		// then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("getMyApplicationList(): 같은 회차에 여러 구역 신청 시 중복 제거하여 반환한다")
	void getMyApplicationList_duplicateSectionInSameSchedule() {
		// given
		Prereservation prereservation1 = mock(Prereservation.class);
		Prereservation prereservation2 = mock(Prereservation.class);

		given(prereservation1.getPerformanceScheduleId()).willReturn(10L);
		given(prereservation1.getSectionId()).willReturn(1L);

		given(prereservation2.getPerformanceScheduleId()).willReturn(10L);
		given(prereservation2.getSectionId()).willReturn(1L);

		given(prereservationRepository.findAllByMemberIdOrderByCreatedAtDesc(MEMBER_ID))
			.willReturn(java.util.List.of(prereservation1, prereservation2));

		// when
		var result = prereservationApplyService.getMyApplicationList(MEMBER_ID);

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).scheduleId()).isEqualTo(10L);
		assertThat(result.get(0).sectionIds()).containsExactly(1L);
	}
}
