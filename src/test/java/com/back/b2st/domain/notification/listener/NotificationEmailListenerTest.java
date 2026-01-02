package com.back.b2st.domain.notification.listener;

import com.back.b2st.domain.email.service.EmailSender;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.entity.Member.Provider;
import com.back.b2st.domain.member.entity.Member.Role;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.notification.event.NotificationEmailEvent;
import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.entity.PerformanceStatus;
import com.back.b2st.domain.performance.repository.PerformanceRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class NotificationEmailListenerTest {

	@InjectMocks
	private NotificationEmailListener listener;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private PerformanceRepository performanceRepository;

	@Mock
	private EmailSender emailSender;

	@Test
	@DisplayName("알림 이메일 이벤트 수신 시 수신자에게 메일 발송")
	void onNotificationEmailEvent_sendsEmail() {
		// given
		Long memberId = 1L;
		Long performanceId = 10L;

		Member member = Member.builder()
			.email("user@test.com")
			.password("pw")
			.name("테스터")
			.phone("01000000000")
			.birth(null)
			.role(Role.MEMBER)
			.provider(Provider.EMAIL)
			.providerId(null)
			.isEmailVerified(true)
			.isIdentityVerified(false)
			.build();
		ReflectionTestUtils.setField(member, "id", memberId);

		Venue venue = Venue.builder()
			.name("공연장")
			.build();
		Performance performance = Performance.builder()
			.venue(venue)
			.title("테스트공연")
			.category("CAT")
			.posterUrl(null)
			.description(null)
			.startDate(java.time.LocalDateTime.now())
			.endDate(java.time.LocalDateTime.now().plusDays(1))
			.status(PerformanceStatus.ACTIVE)
			.build();

		given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
		given(performanceRepository.findById(performanceId)).willReturn(Optional.of(performance));

		NotificationEmailEvent event = NotificationEmailEvent.tradeSold(memberId, performanceId);

		// when
		listener.onNotificationEmailEvent(event);

		// then
		then(emailSender).should().sendTemplateEmail(eq("user@test.com"), anyString(), eq("email/notification"), anyMap());
	}
}
