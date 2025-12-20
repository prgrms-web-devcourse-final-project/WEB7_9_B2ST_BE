package com.back.b2st.domain.payment.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeStatus;
import com.back.b2st.domain.trade.entity.TradeType;
import com.back.b2st.domain.trade.repository.TradeRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TradePaymentHandler implements PaymentDomainHandler {

	private final TradeRepository tradeRepository;

	@Override
	public boolean supports(DomainType domainType) {
		return domainType == DomainType.TRADE;
	}

	@Override
	@Transactional(readOnly = true)
	public PaymentTarget loadAndValidate(Long tradeId, Long memberId) {
		Trade trade = tradeRepository.findById(tradeId)
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND, "양도 게시글을 찾을 수 없습니다."));

		// 1. TRANSFER 타입만 결제 가능 (EXCHANGE는 무료 교환)
		if (trade.getType() != TradeType.TRANSFER) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE, "양도(TRANSFER) 타입만 결제가 필요합니다.");
		}

		// 2. 본인 글은 결제 불가
		if (trade.getMemberId().equals(memberId)) {
			throw new BusinessException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS, "본인의 양도글은 구매할 수 없습니다.");
		}

		// 3. ACTIVE 상태만 결제 가능
		if (trade.getStatus() != TradeStatus.ACTIVE) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE, "유효하지 않은 거래 상태입니다.");
		}

		// 4. 가격이 설정되어 있어야 함
		if (trade.getPrice() == null || trade.getPrice() <= 0) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE, "거래 가격이 설정되지 않았습니다.");
		}

		Long expectedAmount = trade.getPrice().longValue();
		return new PaymentTarget(DomainType.TRADE, tradeId, expectedAmount);
	}
}
