package com.back.b2st.domain.trade.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeRequest;
import com.back.b2st.domain.trade.entity.TradeRequestStatus;

public interface TradeRequestRepository extends JpaRepository<TradeRequest, Long> {

	List<TradeRequest> findByTrade(Trade trade);

	List<TradeRequest> findByRequesterId(Long requesterId);

	List<TradeRequest> findByTradeAndStatus(Trade trade, TradeRequestStatus status);

	List<TradeRequest> findByRequesterIdAndStatus(Long requesterId, TradeRequestStatus status);
}
