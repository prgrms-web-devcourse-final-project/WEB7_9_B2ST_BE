package com.back.b2st.domain.lottery.draw.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.lottery.draw.service.DrawService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawScheduler {

	private final DrawService drawService;

	@Scheduled(cron = "${lottery.draw.cron:0 0 3 * * *}")
	@Transactional
	public void executeDailyDraw() {
		log.info("=== 추첨 스케줄러 시작 ===");

		try {
			drawService.executeDraws();
			log.info("=== 추첨 스케줄러 완료 ===");
		} catch (Exception e) {
			log.error("=== 추첨 스케줄러 실패 ===", e);
		}
	}
}
