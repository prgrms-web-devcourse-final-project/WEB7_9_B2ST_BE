package com.back.b2st.domain.payment.service;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;

/**
 * 결제 취소 시 도메인별 후처리를 담당하는 핸들러 인터페이스
 */
public interface PaymentCancelHandler {

	/**
	 * 해당 도메인 타입을 지원하는지 확인
	 */
	boolean supports(DomainType domainType);

	/**
	 * 결제 취소 시 도메인별 복구 로직 실행
	 * 예: 티켓 복구, 좌석 상태 원복, 거래 상태 변경 등
	 */
	void handleCancel(Payment payment);
}
