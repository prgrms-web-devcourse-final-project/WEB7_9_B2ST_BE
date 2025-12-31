package com.back.b2st.domain.payment.dto.request;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaymentFailReasonTest {

	@Test
	@DisplayName("PaymentFailReason: description이 정상 매핑된다")
	void description() {
		assertThat(PaymentFailReason.USER_CANCELED.getDescription()).isEqualTo("사용자 결제 취소");
		assertThat(PaymentFailReason.PAYMENT_FAILED.getDescription()).isEqualTo("결제 실패");
		assertThat(PaymentFailReason.TIMEOUT.getDescription()).isEqualTo("결제 시간 초과");
		assertThat(PaymentFailReason.UNKNOWN.getDescription()).isEqualTo("기타");
	}
}

