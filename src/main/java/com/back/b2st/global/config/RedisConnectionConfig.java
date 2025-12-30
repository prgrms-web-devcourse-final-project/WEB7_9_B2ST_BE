package com.back.b2st.global.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Spring Data Redis Connection Factory 설정
 *
 * Redisson과 별도로 Spring Data Redis (@EnableRedisRepositories)를 위한
 * RedisConnectionFactory를 설정
 */
@Configuration
@Slf4j
public class RedisConnectionConfig {

	@Value("${spring.data.redis.mode:single}")
	private String redisMode;

	@Value("${spring.data.redis.host:localhost}")
	private String redisHost;

	@Value("${spring.data.redis.port:6379}")
	private int redisPort;

	@Value("${spring.data.redis.password:}")
	private String redisPassword;

	@Value("${spring.data.redis.cluster.nodes:}")
	private String clusterNodes;

	@Bean
	@Primary
	public RedisConnectionFactory redisConnectionFactory() {
		if ("cluster".equalsIgnoreCase(redisMode)) {
			log.info("Redis Cluster 모드로 RedisConnectionFactory 구성");
			return createClusterConnectionFactory();
		} else {
			log.info("Redis Single Server 모드로 RedisConnectionFactory 구성");
			return createStandaloneConnectionFactory();
		}
	}

	private RedisConnectionFactory createClusterConnectionFactory() {
		if (!StringUtils.hasText(clusterNodes)) {
			throw new IllegalArgumentException(
				"Cluster 모드 사용 시 'spring.data.redis.cluster.nodes' 설정이 필요합니다. " +
					"예: localhost:7000,localhost:7001,localhost:7002"
			);
		}

		// Cluster 노드 주소 리스트로 변환
		List<String> nodeList = Arrays.stream(clusterNodes.split(","))
			.map(String::trim)
			.toList();

		RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(nodeList);

		if (StringUtils.hasText(redisPassword)) {
			clusterConfig.setPassword(redisPassword);
		}

		LettuceConnectionFactory factory = new LettuceConnectionFactory(clusterConfig);
		factory.afterPropertiesSet();

		log.info("Redis Cluster ConnectionFactory 구성 완료 - Nodes: {}", nodeList);
		return factory;
	}

	private RedisConnectionFactory createStandaloneConnectionFactory() {
		RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
		standaloneConfig.setHostName(redisHost);
		standaloneConfig.setPort(redisPort);

		if (StringUtils.hasText(redisPassword)) {
			standaloneConfig.setPassword(redisPassword);
		}

		LettuceConnectionFactory factory = new LettuceConnectionFactory(standaloneConfig);
		factory.afterPropertiesSet();

		log.info("Redis Standalone ConnectionFactory 구성 완료 - Host: {}, Port: {}", redisHost, redisPort);
		return factory;
	}
}

