package com.back.b2st.domain.reservation.service;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Redis integration test - 추후 테스트 코드 기입")
public class SeatLockServiceIntegrationTest {

	@Autowired
	private SeatLockService seatLockService;

	@Test
	void 락_획득_성공() {
		String value = seatLockService.tryLock(1L, 1L, 1L);
		assertThat(value).isNotNull();
	}

}
