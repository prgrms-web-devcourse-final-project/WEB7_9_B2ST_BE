package com.back.b2st.domain.trade.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeStatus;
import com.back.b2st.domain.trade.entity.TradeType;

public interface TradeRepository extends JpaRepository<Trade, Long> {

	boolean existsByTicketIdAndStatus(Long ticketId, TradeStatus status);

	Page<Trade> findAllByStatus(TradeStatus status, Pageable pageable);

	Page<Trade> findAllByType(TradeType type, Pageable pageable);

	Page<Trade> findAllByTypeAndStatus(TradeType type, TradeStatus status, Pageable pageable);

	List<Trade> findAllByBuyerIdAndTypeAndStatusOrderByPurchasedAtDesc(Long buyerId, TradeType type, TradeStatus status);

	List<Trade> findAllByMemberIdAndTypeAndStatusOrderByPurchasedAtDesc(Long memberId, TradeType type, TradeStatus status);
}
