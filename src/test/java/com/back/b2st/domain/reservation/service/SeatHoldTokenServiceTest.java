package com.back.b2st.domain.reservation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.back.b2st.domain.scheduleseat.error.ScheduleSeatErrorCode;
import com.back.b2st.domain.scheduleseat.service.SeatHoldTokenService;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class SeatHoldTokenServiceTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@InjectMocks
	private SeatHoldTokenService seatHoldTokenService;

	@Test
	void HOLD_소유자_검증_성공() {
		// given
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get(any())).willReturn("1");

		// when & then
		assertThatCode(() ->
			seatHoldTokenService.validateOwnership(1001L, 55L, 1L)
		).doesNotThrowAnyException();
	}

	@Test
	void HOLD_만료_예외() {
		// given
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get(any())).willReturn(null);

		// when & then
		assertThatThrownBy(() ->
			seatHoldTokenService.validateOwnership(1001L, 55L, 1L)
		)
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(ScheduleSeatErrorCode.SEAT_HOLD_EXPIRED.getMessage());
	}

	@Test
	void HOLD_소유자_불일치_예외() {
		// given
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get(any())).willReturn("2");

		// when & then
		assertThatThrownBy(() ->
			seatHoldTokenService.validateOwnership(1001L, 55L, 1L)
		)
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(ScheduleSeatErrorCode.SEAT_HOLD_FORBIDDEN.getMessage());
	}
}
