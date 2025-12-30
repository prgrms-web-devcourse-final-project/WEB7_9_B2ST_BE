package com.back.b2st.domain.queue.scheduler;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.back.b2st.domain.queue.entity.Queue;
import com.back.b2st.domain.queue.repository.QueueRepository;
import com.back.b2st.domain.queue.service.QueueSchedulerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * QueueEntryScheduler
 *
 * 📌 역할
 * - 대기열을 자동으로 흘려보내는 스케줄러
 * - WAITING 상태의 사용자를 ENTERABLE 상태로 자동 이동
 * - 사람이 버튼을 누르지 않아도 정해진 시간마다 서버가 알아서 처리
 *
 * 핵심 동작:
 * 1. 모든 활성 대기열 조회
 * 2. 각 대기열별로 입장 가능 인원 계산
 * 3. 상위 N명을 자동으로 입장 처리
 *
 * ⚠️ 주의사항
 * - 분산 락이 적용되어 있어 멀티 인스턴스 환경에서도 안전
 * - @Profile("!test")로 테스트 환경에서는 비활성화 (수동 제어)
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
@Profile("!test") // 테스트 환경에서는 비활성화
public class QueueEntryScheduler {

	private final QueueRepository queueRepository;
	private final QueueSchedulerService queueSchedulerService;

	/**
	 * 자동 입장 처리
	 *
	 * 실행 주기: 10초마다 (설정으로 변경 가능)
	 * - 각 대기열별로 대기 중인 사용자를 입장 처리
	 * - 한 번에 처리할 인원: 10명 (배치 크기)
	 *
	 * @Scheduled(fixedDelay = 10000)
	 * - 이전 실행이 완료된 후 10초 뒤에 다시 실행
	 * - 처리 시간이 길어져도 중복 실행 방지
	 */
	@Scheduled(fixedDelay = 10000) // 10초마다
	public void autoProcessQueueEntries() {
		try {
			// 1. 모든 활성 대기열 조회
			List<Queue> activeQueues = queueRepository.findAll();

			if (activeQueues.isEmpty()) {
				log.debug("활성 대기열 없음");
				return;
			}

			// 2. 각 대기열별로 자동 입장 처리
			for (Queue queue : activeQueues) {
				try {
					// 분산 락 적용되어 있어 안전하게 처리됨
					queueSchedulerService.processNextEntries(queue.getId(), 10);
				} catch (Exception e) {
					// 특정 대기열 실패해도 다른 대기열 처리는 계속
					log.error("대기열 자동 입장 처리 실패 - queueId: {}", queue.getId(), e);
				}
			}

		} catch (Exception e) {
			// 스케줄러는 절대 죽으면 안 됨
			log.error("자동 입장 스케줄러 실패", e);
		}
	}
}

