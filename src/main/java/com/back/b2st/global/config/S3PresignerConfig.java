package com.back.b2st.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * S3 Presigner Bean 설정
 *
 * S3Presigner를 Bean으로 관리하여 재사용
 */
@Configuration
public class S3PresignerConfig {

	private final S3ConfigProperties s3Config;

	public S3PresignerConfig(S3ConfigProperties s3Config) {
		this.s3Config = s3Config;
	}

	@Bean(destroyMethod = "close")
	public S3Presigner s3Presigner() {
		return S3Presigner.builder()
			.region(Region.of(s3Config.region()))
			.credentialsProvider(DefaultCredentialsProvider.create())
			.build();
	}
}

