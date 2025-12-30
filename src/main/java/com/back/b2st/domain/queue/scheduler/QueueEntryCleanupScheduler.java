package com.back.b2st.domain.queue.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.queue.entity.QueueEntry;
import com.back.b2st.domain.queue.entity.QueueEntryStatus;
import com.back.b2st.domain.queue.repository.QueueEntryRepository;
import com.back.b2st.domain.queue.repository.QueueRedisRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Queue Entry Cleanup Scheduler
 *
 * DB에 남아있는 ENTERABLE 상태를 정리하는 배치 작업
 *
 *  SoT=Redis 토큰 정책:
 * - 유효한 ENTERABLE은 Redis 토큰이 있을 때만
 * - DB에 ENTERABLE이 남아있더라도:
 *   1) expiresAt <= now → 무조건 EXPIRED로 정리
 *   2) expiresAt > now 인데 Redis 토큰 없음 → stale로 보고 EXPIRED로 정리
 *
 * 개발 초기 단계 - 대기열 기능 활성화 시에만 로드
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
public class QueueEntryCleanupScheduler {

	private static final int BATCH_SIZE = 500;

	private final QueueEntryRepository queueEntryRepository;
	private final QueueRedisRepository queueRedisRepository;

	@Value("${queue.cleanup.stale.enabled:false}")
	private boolean staleCleanupEnabled;

	/**
	 * 1) 만료된 ENTERABLE(DB) 정리: expiresAt <= now
	 *
	 * - Redis 상태와 무관하게 "DB 기록상 만료"는 정리해도 SoT에 영향 없음
	 */
	@Scheduled(fixedDelayString = "${queue.cleanup.expired.fixedDelayMs:60000}")
	@Transactional(readOnly = false)
	public void expireDbEnterablesByExpiresAt() {
		try {
			LocalDateTime now = LocalDateTime.now();

			List<QueueEntry> targets = queueEntryRepository.findExpiredEnterables(
				QueueEntryStatus.ENTERABLE,
				now,
				PageRequest.of(0, BATCH_SIZE)
			);

			if (targets.isEmpty()) {
				return;
			}

			int updated = 0;
			for (QueueEntry entry : targets) {
				entry.updateToExpired();
				updated++;
			}

			// JPA dirty checking으로 flush됨
			log.info("Expired ENTERABLE entries cleaned by expiresAt - updated: {}", updated);
		} catch (Exception e) {
			log.error("Failed to expire DB ENTERABLE entries by expiresAt", e);
		}
	}

	/**
	 * 2) (옵션) 토큰 없는 ENTERABLE(DB) 정리: expiresAt > now 인데 Redis 토큰 없음
	 *
	 * - SoT=Redis 토큰 정책을 강하게 유지하려면 켜는 편이 좋을 듯
	 * - 다만 Redis 장애/운영 중 토큰 조회가 불안정하면 오탐으로 EXPIRED 처리될 수 있으니
	 *   운영 안정성에 따라 ON/OFF 권장.
	 */
	@Scheduled(fixedDelayString = "${queue.cleanup.stale.fixedDelayMs:60000}")
	@Transactional(readOnly = false)
	public void expireStaleDbEnterablesWithoutRedisToken() {
		// 프로퍼티로 끄고 켤 수 있게: 기본 false 권장
		if (!staleCleanupEnabled) {
			return;
		}

		try {
			LocalDateTime now = LocalDateTime.now();

			List<QueueEntry> candidates = queueEntryRepository.findNonExpiredEnterables(
				QueueEntryStatus.ENTERABLE,
				now,
				PageRequest.of(0, BATCH_SIZE)
			);

			if (candidates.isEmpty()) {
				return;
			}

			int expiredAsStale = 0;

			for (QueueEntry entry : candidates) {
				Long queueId = entry.getQueueId();
				Long userId = entry.getUserId();

				boolean hasToken;
				try {
					hasToken = queueRedisRepository.isInEnterable(queueId, userId);
				} catch (Exception e) {
					log.warn("Redis token check failed, skip stale cleanup - queueId: {}, userId: {}", queueId, userId, e);
					continue;
				}

				if (!hasToken) {
					entry.updateToExpired();
					expiredAsStale++;
				}
			}

			if (expiredAsStale > 0) {
				log.info("Stale ENTERABLE entries cleaned (no Redis token) - updated: {}", expiredAsStale);
			}
		} catch (Exception e) {
			log.error("Failed to expire stale DB ENTERABLE entries without Redis token", e);
		}
	}
}

