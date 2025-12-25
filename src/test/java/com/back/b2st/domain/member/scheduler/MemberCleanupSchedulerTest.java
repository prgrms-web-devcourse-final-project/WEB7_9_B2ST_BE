package com.back.b2st.domain.member.scheduler;

import static com.back.b2st.domain.member.fixture.MemberTestFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class MemberCleanupSchedulerTest {

	@InjectMocks
	private MemberCleanupScheduler scheduler;

	@Mock
	private MemberRepository memberRepository;

	// 밑으로 헬퍼 메서드
	private Member createExpiredMember(Long id) {
		Member member = createMemberWithId(id, "expired@test.com", "password");
		member.softDelete();
		ReflectionTestUtils.setField(member, "deletedAt", LocalDateTime.now().minusDays(35));
		return member;
	}

	@Nested
	@DisplayName("익명화 처리")
	class AnonymizeTest {

		@Test
		@DisplayName("30일 경과 회원 익명화 성공")
		void processExpiredWithdrawals_success() {
			Member member = createExpiredMember(1L);

			given(memberRepository.findAllByDeletedAtBefore(any(LocalDateTime.class)))
				.willReturn(List.of(member));

			scheduler.processExpiredWithdrawals();

			assertThat(member.getEmail()).startsWith("withdrawn_");
			assertThat(member.getName()).isEqualTo("탈퇴회원");
		}

		@Test
		@DisplayName("대상 없으면 처리 스킵")
		void processExpiredWithdrawals_noTarget() {
			given(memberRepository.findAllByDeletedAtBefore(any(LocalDateTime.class)))
				.willReturn(Collections.emptyList());

			scheduler.processExpiredWithdrawals();
			// 예외 없이 정상 종료
		}

		@Test
		@DisplayName("이미 익명화된 회원은 스킵")
		void processExpiredWithdrawals_alreadyAnonymized() {
			Member member = createExpiredMember(1L);
			ReflectionTestUtils.setField(member, "email", "withdrawn_1@deleted.local");

			given(memberRepository.findAllByDeletedAtBefore(any(LocalDateTime.class)))
				.willReturn(List.of(member));

			scheduler.processExpiredWithdrawals();

			assertThat(member.getEmail()).isEqualTo("withdrawn_1@deleted.local");
		}

		@Test
		@DisplayName("익명화 중 예외 발생 시 다음 회원 처리 계속")
		void processExpiredWithdrawals_exceptionContinues() {
			Member normalMember = createExpiredMember(1L);
			Member faultyMember = mock(Member.class);
			Member anotherMember = createExpiredMember(3L);

			// faultyMember는 예외 발생하도록 설정
			given(faultyMember.getEmail()).willReturn("faulty@test.com");
			doThrow(new RuntimeException("익명화 실패")).when(faultyMember).anonymize();

			given(memberRepository.findAllByDeletedAtBefore(any(LocalDateTime.class)))
				.willReturn(List.of(normalMember, faultyMember, anotherMember));

			// 예외 발생해도 전체 처리는 계속됨
			scheduler.processExpiredWithdrawals();

			// 정상 회원들은 익명화됨
			assertThat(normalMember.getEmail()).startsWith("withdrawn_");
			assertThat(anotherMember.getEmail()).startsWith("withdrawn_");
		}
	}
}
