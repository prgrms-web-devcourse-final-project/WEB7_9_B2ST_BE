package com.back.b2st.domain.lottery.draw.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.back.b2st.domain.lottery.result.repository.LotteryResultRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
class CancelUnpaidService {

	private final LotteryResultRepository lotteryResultRepository;

	public void cancelUnpaid() {
		LocalDateTime now = LocalDateTime.now();
		int count = lotteryResultRepository.removeUnpaidAll(now);

		log.info("{} 미결제자 취소: {} 건", now, count);
	}
}
