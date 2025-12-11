package com.back.b2st.domain.trade.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeStatus;

public interface TradeRepository extends JpaRepository<Trade, Long> {

	boolean existsByTicketIdAndStatus(Long ticketId, TradeStatus status);
}
