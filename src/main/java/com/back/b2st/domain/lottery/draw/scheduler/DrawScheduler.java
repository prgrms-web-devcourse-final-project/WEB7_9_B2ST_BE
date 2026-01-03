package com.back.b2st.domain.lottery.draw.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.back.b2st.domain.lottery.draw.service.DrawService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawScheduler {

	private final DrawService drawService;

	@Scheduled(cron = "${lottery.draw.cron:0 0 3 * * *}")
	public void executeDailyDraw() {
		try {
			drawService.executeDraws();
		} catch (Exception e) {
			log.error("Error, drawService.executeDraws()", e);
		}
	}
}
