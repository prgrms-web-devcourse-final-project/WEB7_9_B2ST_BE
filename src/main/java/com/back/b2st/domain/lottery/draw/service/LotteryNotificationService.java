package com.back.b2st.domain.lottery.draw.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.back.b2st.domain.email.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LotteryNotificationService {

	private final EmailService emailService;

	public void notifyWinners(Long scheduleId) {
		try {
			emailService.sendWinnerNotifications(scheduleId);
		} catch (Exception e) {
			log.error("당첨자 알림 실패 - scheduleId: {}", scheduleId, e);
		}
	}

	public void notifyCancelUnpaid(List<Long> memberIds) {
		try {
			emailService.sendCancelUnpaidNotifications(memberIds);
		} catch (Exception e) {
			log.error("당첨 취소 알림 실패", e);
		}
	}
}
