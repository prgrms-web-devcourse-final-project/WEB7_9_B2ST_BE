package com.back.b2st.domain.lottery.draw.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.lottery.draw.DrawService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawScheduler {

	private final DrawService drawService;

	// 테스트용: 1분마다 실행 (나중에 새벽 3시로 변경)
	@Scheduled(cron = "0 * * * * *")  // 매분 0초에 실행
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
