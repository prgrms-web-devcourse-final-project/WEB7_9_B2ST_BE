package com.back.b2st.domain.lottery.result.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.b2st.domain.lottery.result.entity.LotteryResult;

public interface LotteryResultRepository extends JpaRepository<LotteryResult, Long> {
	
}
