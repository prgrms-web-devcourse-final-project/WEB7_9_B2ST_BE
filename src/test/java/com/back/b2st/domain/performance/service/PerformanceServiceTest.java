package com.back.b2st.domain.performance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.back.b2st.domain.performance.dto.response.PerformanceCursorPageRes;
import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.entity.PerformanceStatus;
import com.back.b2st.domain.performance.mapper.PerformanceMapper;
import com.back.b2st.domain.performance.repository.PerformanceRepository;

@ExtendWith(MockitoExtension.class)
class PerformanceServiceTest {

	@Mock
	private PerformanceRepository performanceRepository;

	@Mock
	private PerformanceMapper performanceMapper;

	@InjectMocks
	private PerformanceService performanceService;

	@Test
	@DisplayName("getActivePerformancesWithCursor(): 종료된 공연을 필터링한다")
	void getActivePerformancesWithCursor_filtersEndedPerformances() {
		// given
		Long cursor = 100L;
		int size = 10;
		LocalDate today = LocalDate.now();
		LocalDateTime todayStart = today.atStartOfDay();

		given(performanceRepository.findByStatusWithCursor(
			eq(PerformanceStatus.ACTIVE),
			any(LocalDateTime.class),
			eq(cursor),
			any(Pageable.class)
		)).willReturn(List.of());

		// when
		performanceService.getActivePerformancesWithCursor(cursor, size);

		// then
		ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

		verify(performanceRepository).findByStatusWithCursor(
			eq(PerformanceStatus.ACTIVE),
			dateCaptor.capture(),
			eq(cursor),
			pageableCaptor.capture()
		);

		// todayStart가 오늘 00:00:00인지 확인
		LocalDateTime capturedDate = dateCaptor.getValue();
		assertThat(capturedDate).isEqualTo(todayStart);
		assertThat(capturedDate.toLocalDate()).isEqualTo(today);
		assertThat(capturedDate.getHour()).isEqualTo(0);
		assertThat(capturedDate.getMinute()).isEqualTo(0);
		assertThat(capturedDate.getSecond()).isEqualTo(0);

		// Pageable이 size+1로 설정되었는지 확인
		Pageable capturedPageable = pageableCaptor.getValue();
		assertThat(capturedPageable.getPageSize()).isEqualTo(size + 1);
	}

	@Test
	@DisplayName("searchActivePerformancesWithCursor(): 종료된 공연을 필터링한다")
	void searchActivePerformancesWithCursor_filtersEndedPerformances() {
		// given
		Long cursor = 200L;
		String keyword = "콘서트";
		int size = 20;
		LocalDate today = LocalDate.now();
		LocalDateTime todayStart = today.atStartOfDay();

		given(performanceRepository.searchActiveWithCursor(
			eq(PerformanceStatus.ACTIVE),
			any(LocalDateTime.class),
			eq(keyword),
			eq(cursor),
			any(Pageable.class)
		)).willReturn(List.of());

		// when
		performanceService.searchActivePerformancesWithCursor(cursor, keyword, size);

		// then
		ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

		verify(performanceRepository).searchActiveWithCursor(
			eq(PerformanceStatus.ACTIVE),
			dateCaptor.capture(),
			eq(keyword),
			eq(cursor),
			pageableCaptor.capture()
		);

		// todayStart가 오늘 00:00:00인지 확인
		LocalDateTime capturedDate = dateCaptor.getValue();
		assertThat(capturedDate).isEqualTo(todayStart);
		assertThat(capturedDate.toLocalDate()).isEqualTo(today);
		assertThat(capturedDate.getHour()).isEqualTo(0);
		assertThat(capturedDate.getMinute()).isEqualTo(0);
		assertThat(capturedDate.getSecond()).isEqualTo(0);

		// Pageable이 size+1로 설정되었는지 확인
		Pageable capturedPageable = pageableCaptor.getValue();
		assertThat(capturedPageable.getPageSize()).isEqualTo(size + 1);
	}

	@Test
	@DisplayName("searchActivePerformancesWithCursor(): 키워드가 null이면 일반 목록 조회로 위임")
	void searchActivePerformancesWithCursor_nullKeyword_delegatesToGetActivePerformances() {
		// given
		Long cursor = 300L;
		int size = 15;

		given(performanceRepository.findByStatusWithCursor(
			eq(PerformanceStatus.ACTIVE),
			any(LocalDateTime.class),
			eq(cursor),
			any(Pageable.class)
		)).willReturn(List.of());

		// when
		performanceService.searchActivePerformancesWithCursor(cursor, null, size);

		// then
		verify(performanceRepository).findByStatusWithCursor(
			eq(PerformanceStatus.ACTIVE),
			any(LocalDateTime.class),
			eq(cursor),
			any(Pageable.class)
		);
		verify(performanceRepository, never()).searchActiveWithCursor(
			any(),
			any(),
			any(),
			any(),
			any()
		);
	}

	@Test
	@DisplayName("searchActivePerformancesWithCursor(): 키워드가 빈 문자열이면 일반 목록 조회로 위임")
	void searchActivePerformancesWithCursor_emptyKeyword_delegatesToGetActivePerformances() {
		// given
		Long cursor = 400L;
		int size = 25;

		given(performanceRepository.findByStatusWithCursor(
			eq(PerformanceStatus.ACTIVE),
			any(LocalDateTime.class),
			eq(cursor),
			any(Pageable.class)
		)).willReturn(List.of());

		// when
		performanceService.searchActivePerformancesWithCursor(cursor, "  ", size);

		// then
		verify(performanceRepository).findByStatusWithCursor(
			eq(PerformanceStatus.ACTIVE),
			any(LocalDateTime.class),
			eq(cursor),
			any(Pageable.class)
		);
		verify(performanceRepository, never()).searchActiveWithCursor(
			any(),
			any(),
			any(),
			any(),
			any()
		);
	}
}
