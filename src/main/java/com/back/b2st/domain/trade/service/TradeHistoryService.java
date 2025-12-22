package com.back.b2st.domain.trade.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.trade.dto.response.TransferTradeHistoryRes;
import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeStatus;
import com.back.b2st.domain.trade.entity.TradeType;
import com.back.b2st.domain.trade.repository.TradeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeHistoryService {

	private final TradeRepository tradeRepository;

	public List<TransferTradeHistoryRes> getMyTransferPurchases(Long memberId) {
		List<Trade> trades = tradeRepository.findAllByBuyerIdAndTypeAndStatusOrderByPurchasedAtDesc(
			memberId,
			TradeType.TRANSFER,
			TradeStatus.COMPLETED
		);
		return trades.stream()
			.map(TransferTradeHistoryRes::from)
			.toList();
	}

	public List<TransferTradeHistoryRes> getMyTransferSales(Long memberId) {
		List<Trade> trades = tradeRepository.findAllByMemberIdAndTypeAndStatusOrderByPurchasedAtDesc(
			memberId,
			TradeType.TRANSFER,
			TradeStatus.COMPLETED
		);
		return trades.stream()
			.map(TransferTradeHistoryRes::from)
			.toList();
	}
}

