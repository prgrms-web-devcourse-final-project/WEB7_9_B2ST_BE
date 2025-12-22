package com.back.b2st.domain.scheduleseat.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.back.b2st.domain.scheduleseat.error.ScheduleSeatErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatHoldTokenService {

	private final StringRedisTemplate redisTemplate;

	public static final Duration HOLD_TTL = Duration.ofMinutes(5);

	/** === HOLD 소유권 저장 === */
	public void save(Long scheduleId, Long seatId, Long memberId) {
		redisTemplate.opsForValue().set(
			getKey(scheduleId, seatId),
			memberId.toString(),
			HOLD_TTL
		);
	}

	/** === HOLD 소유권 검증 === */
	public void validateOwnership(Long scheduleId, Long seatId, Long memberId) {

		String holder = getHolder(scheduleId, seatId);

		if (holder == null) {
			throw new BusinessException(ScheduleSeatErrorCode.SEAT_HOLD_EXPIRED);
		}

		if (!holder.equals(memberId.toString())) {
			throw new BusinessException(ScheduleSeatErrorCode.SEAT_HOLD_FORBIDDEN);
		}
	}

	/** === HOLD 소유자 조회 === */
	public String getHolder(Long scheduleId, Long seatId) {
		return redisTemplate.opsForValue().get(getKey(scheduleId, seatId));
	}

	/** === HOLD 소유권 제거 === */
	public void remove(Long scheduleId, Long seatId) {
		redisTemplate.delete(getKey(scheduleId, seatId));
	}

	private String getKey(Long scheduleId, Long seatId) {
		return "seat:hold:" + scheduleId + ":" + seatId;
	}
}