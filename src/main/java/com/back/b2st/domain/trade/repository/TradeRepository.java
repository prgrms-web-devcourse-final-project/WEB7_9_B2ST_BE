package com.back.b2st.domain.trade.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	// 구매자가 받은 완료된 거래 조회 (티켓 획득 경로 확인용)
	List<Trade> findAllByBuyerIdAndStatus(Long buyerId, TradeStatus status);

	boolean existsByPerformanceId(Long performanceId);

	@Query("""
		select t.id
		  from Trade t
		 where t.performanceId = :performanceId
		""")
	List<Long> findIdsByPerformanceId(@Param("performanceId") Long performanceId);

	void deleteAllByPerformanceId(Long performanceId);
}
