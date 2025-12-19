package com.back.b2st.domain.payment.toss;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Base64;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

class TossPaymentsRestClient implements TossPaymentsClient {

	private final RestClient restClient;
	private final TossPaymentsProperties properties;

	TossPaymentsRestClient(RestClient restClient, TossPaymentsProperties properties) {
		this.restClient = restClient;
		this.properties = properties;
	}

	@Override
	public TossPaymentsConfirmResult confirm(String paymentKey, String orderId, Long amount) {
		if (properties.secretKey() == null || properties.secretKey().isBlank()) {
			throw new BusinessException(PaymentErrorCode.TOSS_CONFIRM_FAILED, "Toss secretKey 설정이 필요합니다.");
		}

		String authHeader = buildBasicAuth(properties.secretKey());

		try {
			TossConfirmResponse response = restClient.post()
				.uri("/v1/payments/confirm")
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(new TossConfirmRequest(paymentKey, orderId, amount))
				.retrieve()
				.body(TossConfirmResponse.class);

			if (response == null) {
				throw new BusinessException(PaymentErrorCode.TOSS_CONFIRM_FAILED, "Toss 응답이 비어있습니다.");
			}

			return new TossPaymentsConfirmResult(
				response.paymentKey(),
				response.orderId(),
				response.totalAmount(),
				parseApprovedAt(response.approvedAt())
			);
		} catch (RestClientResponseException e) {
			throw new BusinessException(PaymentErrorCode.TOSS_CONFIRM_FAILED,
				"Toss 결제 승인 실패: " + e.getResponseBodyAsString());
		}
	}

	private static String buildBasicAuth(String secretKey) {
		String raw = secretKey + ":";
		String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
		return "Basic " + encoded;
	}

	private static LocalDateTime parseApprovedAt(String approvedAt) {
		if (approvedAt == null || approvedAt.isBlank()) {
			return null;
		}
		return OffsetDateTime.parse(approvedAt).toLocalDateTime();
	}

	private record TossConfirmRequest(String paymentKey, String orderId, Long amount) {
	}

	private record TossConfirmResponse(
		String paymentKey,
		String orderId,
		Long totalAmount,
		String approvedAt
	) {
	}
}

