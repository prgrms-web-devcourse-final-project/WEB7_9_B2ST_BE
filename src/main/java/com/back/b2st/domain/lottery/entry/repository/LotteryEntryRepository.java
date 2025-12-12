package com.back.b2st.domain.lottery.entry.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.b2st.domain.lottery.entry.entity.LotteryEntry;

public interface LotteryEntryRepository extends JpaRepository<LotteryEntry, Long> {
}
