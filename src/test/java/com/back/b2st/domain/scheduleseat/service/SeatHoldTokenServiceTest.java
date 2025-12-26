package com.back.b2st.domain.scheduleseat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.back.b2st.domain.scheduleseat.error.ScheduleSeatErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class SeatHoldTokenServiceTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@InjectMocks
	private SeatHoldTokenService seatHoldTokenService;

	private static final Long SCHEDULE_ID = 1001L;
	private static final Long SEAT_ID = 55L;
	private static final Long MEMBER_ID = 1L;

	private static String key(Long scheduleId, Long seatId) {
		return "seat:hold:" + scheduleId + ":" + seatId;
	}

	@Test
	@DisplayName("save(): Redis에 (key, memberId) 를 TTL과 함께 저장한다")
	void save_success() {
		// given
		given(redisTemplate.opsForValue()).willReturn(valueOperations);

		// when
		seatHoldTokenService.save(SCHEDULE_ID, SEAT_ID, MEMBER_ID);

		// then
		then(valueOperations).should()
			.set(key(SCHEDULE_ID, SEAT_ID), MEMBER_ID.toString(), SeatHoldTokenService.HOLD_TTL);
	}

	@Test
	@DisplayName("remove(): Redis에서 HOLD 토큰 키를 삭제한다")
	void remove_success() {
		// when
		seatHoldTokenService.remove(SCHEDULE_ID, SEAT_ID);

		// then
		then(redisTemplate).should().delete(key(SCHEDULE_ID, SEAT_ID));
	}

	@Test
	@DisplayName("validateOwnership(): holder가 같으면 예외 없이 통과한다")
	void validateOwnership_success() {
		// given
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get(key(SCHEDULE_ID, SEAT_ID))).willReturn(MEMBER_ID.toString());

		// when & then
		assertThatCode(() -> seatHoldTokenService.validateOwnership(SCHEDULE_ID, SEAT_ID, MEMBER_ID))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("validateOwnership(): holder가 null이면 SEAT_HOLD_EXPIRED 예외")
	void validateOwnership_expired_throw() {
		// given
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get(key(SCHEDULE_ID, SEAT_ID))).willReturn(null);

		// when & then
		assertThatThrownBy(() -> seatHoldTokenService.validateOwnership(SCHEDULE_ID, SEAT_ID, MEMBER_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(ScheduleSeatErrorCode.SEAT_HOLD_EXPIRED.getMessage());
	}

	@Test
	@DisplayName("validateOwnership(): holder가 다르면 SEAT_HOLD_FORBIDDEN 예외")
	void validateOwnership_forbidden_throw() {
		// given
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get(key(SCHEDULE_ID, SEAT_ID))).willReturn("2");

		// when & then
		assertThatThrownBy(() -> seatHoldTokenService.validateOwnership(SCHEDULE_ID, SEAT_ID, MEMBER_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(ScheduleSeatErrorCode.SEAT_HOLD_FORBIDDEN.getMessage());
	}
}
