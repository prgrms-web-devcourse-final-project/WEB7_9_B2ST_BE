package com.back.b2st.domain.lottery.draw.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.lottery.result.repository.LotteryResultRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
class CancelUnpaidService {

	private final LotteryResultRepository lotteryResultRepository;

	@Transactional
	public List<Long> cancelUnpaid() {
		LocalDateTime now = LocalDateTime.now();
		List<Long> memberIds = lotteryResultRepository.findCancelUnpaidAll(now);

		int count = lotteryResultRepository.removeUnpaidAll(now);
		log.info("{} 미결제자 취소: {} 건", now, count);

		return memberIds;
	}
}
