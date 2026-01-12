package com.back.b2st.domain.scheduleseat.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduleSeatLockService {

	private final StringRedisTemplate redisTemplate;

	private static final int LOCK_EXPIRE_SECONDS = 5;  // 락 TTL: hold 처리 중 서버 다운/예외 시 자동 해제되게 하는 안전장치

	/** === 좌석 lock 시도 (성공하면 true 반환) === */
	public String tryLock(Long scheduleId, Long seatId, Long memberId) {

		String key = getLockKey(scheduleId, seatId);
		String value = memberId + ":" + UUID.randomUUID(); // unlock 검증용

		Boolean success = redisTemplate
			.opsForValue()
			.setIfAbsent(
				key,
				value,
				Duration.ofSeconds(LOCK_EXPIRE_SECONDS)
			);

		return Boolean.TRUE.equals(success) ? value : null;
	}

	/** === 락 해제 (value 검증 후 해제) === */
	public void unlock(Long scheduleId, Long seatId, String expectedValue) {
		String key = getLockKey(scheduleId, seatId);

		String currentValue = redisTemplate.opsForValue().get(key);

		// 내가 잡은 락일 때만 삭제
		if (expectedValue != null && expectedValue.equals(currentValue)) {
			redisTemplate.delete(key);
		}
	}

	/** Redis Key Builder */
	private String getLockKey(Long scheduleId, Long seatId) {
		return "seat:lock:" + scheduleId + ":" + seatId;
	}
}
