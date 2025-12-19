package com.back.b2st.domain.payment.toss;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Configuration
@Profile("toss")
@EnableConfigurationProperties(TossPaymentsProperties.class)
public class TossPaymentsConfig {

	@Bean
	RestClient tossPaymentsRestClient(TossPaymentsProperties properties) {
		return RestClient.builder()
			.baseUrl(properties.baseUrl())
			.build();
	}

	@Bean
	TossPaymentsClient tossPaymentsClient(RestClient tossPaymentsRestClient, TossPaymentsProperties properties) {
		return new TossPaymentsRestClient(tossPaymentsRestClient, properties);
	}
}
