package com.back.b2st.domain.notification.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import com.back.b2st.domain.email.service.EmailSender;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.notification.event.NotificationEmailEvent;
import com.back.b2st.domain.notification.service.NotificationMessageFactory;
import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performance.repository.PerformanceRepository;

import java.util.Map;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationEmailListener {

	private final MemberRepository memberRepository;
	private final PerformanceRepository performanceRepository;
	private final EmailSender emailSender;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
	public void onNotificationEmailEvent(NotificationEmailEvent event) {
		Member recipient = memberRepository.findById(event.recipientMemberId()).orElse(null);
		if (recipient == null || recipient.isDeleted()) {
			return;
		}

		String performanceTitle = performanceRepository.findById(event.performanceId())
			.map(Performance::getTitle)
			.orElse("공연");

		String subject = NotificationMessageFactory.subject(event.type(), performanceTitle);
		String message = NotificationMessageFactory.message(event.type(), performanceTitle);

		emailSender.sendTemplateEmail(
			recipient.getEmail(),
			subject,
			"email/notification",
			Map.of("message", message)
		);
	}
}
