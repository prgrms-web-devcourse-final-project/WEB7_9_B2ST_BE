package com.back.b2st.global.config;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Redisson 설정
 *
 * 분산 락(Distributed Lock)을 위한 Redisson Client 설정
 */
@Configuration
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class RedissonConfig {

	// 단일 서버 모드 설정
	@Value("${spring.data.redis.host:localhost}")
	private String redisHost;

	@Value("${spring.data.redis.port:6379}")
	private int redisPort;

	@Value("${spring.data.redis.password:}")
	private String redisPassword;

	// Redis 모드 설정 (single, sentinel, cluster)
	@Value("${spring.data.redis.mode:single}")
	private String redisMode;

	// Sentinel 모드 설정
	@Value("${spring.data.redis.sentinel.master:mymaster}")
	private String sentinelMaster;

	@Value("${spring.data.redis.sentinel.nodes:}")
	private String sentinelNodes; // "sentinel1:26379,sentinel2:26379,sentinel3:26379"

	// Cluster 모드 설정
	@Value("${spring.data.redis.cluster.nodes:}")
	private String clusterNodes; // "node1:6379,node2:6379,node3:6379"

	// 대규모 트래픽 최적화 설정
	@Value("${spring.data.redis.cluster.master-connection-pool-size:64}")
	private int clusterMasterConnectionPoolSize;

	@Value("${spring.data.redis.cluster.slave-connection-pool-size:64}")
	private int clusterSlaveConnectionPoolSize;

	@Value("${spring.data.redis.cluster.master-connection-minimum-idle-size:10}")
	private int clusterMasterConnectionMinimumIdleSize;

	@Value("${spring.data.redis.cluster.slave-connection-minimum-idle-size:10}")
	private int clusterSlaveConnectionMinimumIdleSize;

	@Value("${spring.data.redis.cluster.scan-interval:2000}")
	private int clusterScanInterval;

	@Value("${spring.data.redis.cluster.timeout:5000}")
	private int clusterTimeout;

	@Value("${spring.data.redis.cluster.connect-timeout:10000}")
	private int clusterConnectTimeout;

	@Bean
	public RedissonClient redissonClient() {
		Config config = new Config();

		switch (redisMode.toLowerCase()) {
			case "sentinel" -> {
				log.info("Redis Sentinel 모드로 Redisson 클라이언트 구성");
				configureSentinel(config);
			}
			case "cluster" -> {
				log.info("Redis Cluster 모드로 Redisson 클라이언트 구성");
				configureCluster(config);
			}
			default -> {
				log.info("Redis Single Server 모드로 Redisson 클라이언트 구성");
				configureSingleServer(config);
			}
		}

		return Redisson.create(config);
	}

	/**
	 * Sentinel 모드 설정
	 *
	 * Master-Slave 자동 전환 지원
	 */
	private void configureSentinel(Config config) {
		if (!StringUtils.hasText(sentinelNodes)) {
			throw new IllegalArgumentException(
				"Sentinel 모드 사용 시 'spring.data.redis.sentinel.nodes' 설정이 필요합니다. " +
					"예: sentinel1:26379,sentinel2:26379,sentinel3:26379"
			);
		}

		// Sentinel 주소 배열로 변환
		String[] sentinelAddresses = Arrays.stream(sentinelNodes.split(","))
			.map(node -> {
				String trimmed = node.trim();
				return trimmed.startsWith("redis://") ? trimmed : "redis://" + trimmed;
			})
			.toArray(String[]::new);

		config.useSentinelServers()
			.setMasterName(sentinelMaster)
			.addSentinelAddress(sentinelAddresses)
			.setPassword(StringUtils.hasText(redisPassword) ? redisPassword : null)
			.setDatabase(0)
			// Master 연결 풀 설정
			.setMasterConnectionPoolSize(10)
			.setMasterConnectionMinimumIdleSize(2)
			// Slave 연결 풀 설정
			.setSlaveConnectionPoolSize(10)
			.setSlaveConnectionMinimumIdleSize(2)
			// 재시도 설정
			.setRetryAttempts(3)
			.setRetryInterval(1500)
			// 타임아웃 설정
			.setTimeout(3000)
			.setConnectTimeout(10000);

		log.info("Sentinel 구성 완료 - Master: {}, Nodes: {}", sentinelMaster, Arrays.toString(sentinelAddresses));
	}

	/**
	 * Cluster 모드 설정
	 */
	private void configureCluster(Config config) {
		if (!StringUtils.hasText(clusterNodes)) {
			throw new IllegalArgumentException(
				"Cluster 모드 사용 시 'spring.data.redis.cluster.nodes' 설정이 필요합니다. " +
					"예: node1:6379,node2:6379,node3:6379"
			);
		}

		// Cluster 노드 주소 배열로 변환
		String[] nodeAddresses = Arrays.stream(clusterNodes.split(","))
			.map(node -> {
				String trimmed = node.trim();
				return trimmed.startsWith("redis://") ? trimmed : "redis://" + trimmed;
			})
			.toArray(String[]::new);

		config.useClusterServers()
			.addNodeAddress(nodeAddresses)
			.setPassword(StringUtils.hasText(redisPassword) ? redisPassword : null)
			// 클러스터 노드 스캔 간격 (밀리초)
			.setScanInterval(clusterScanInterval)
			// Master 연결 풀 설정
			.setMasterConnectionPoolSize(clusterMasterConnectionPoolSize)
			.setMasterConnectionMinimumIdleSize(clusterMasterConnectionMinimumIdleSize)
			// Slave 연결 풀 설정
			.setSlaveConnectionPoolSize(clusterSlaveConnectionPoolSize)
			.setSlaveConnectionMinimumIdleSize(clusterSlaveConnectionMinimumIdleSize)
			// 읽기 모드: Master에서만 읽기
			.setReadMode(org.redisson.config.ReadMode.SLAVE) // Slave에서 읽기로 부하 분산
			// 재시도 설정
			.setRetryAttempts(3)
			.setRetryInterval(1500)
			// 타임아웃 설정
			.setTimeout(clusterTimeout)
			.setConnectTimeout(clusterConnectTimeout)
			.setIdleConnectionTimeout(10000)
			.setPingConnectionInterval(30000)
			.setKeepAlive(true)
			.setTcpNoDelay(true);

		log.info("Cluster 구성 완료 (대규모 트래픽 최적화) - Nodes: {}, " +
				"Master Pool: {}, Slave Pool: {}, Timeout: {}ms",
			Arrays.toString(nodeAddresses),
			clusterMasterConnectionPoolSize,
			clusterSlaveConnectionPoolSize,
			clusterTimeout);
	}

	/**
	 * 단일 서버 모드 설정
	 */
	private void configureSingleServer(Config config) {
		String address = String.format("redis://%s:%d", redisHost, redisPort);

		config.useSingleServer()
			.setAddress(address)
			.setPassword(StringUtils.hasText(redisPassword) ? redisPassword : null)
			.setConnectionPoolSize(10)
			.setConnectionMinimumIdleSize(2)
			.setRetryAttempts(3)
			.setRetryInterval(1500);

		log.info("Single Server 구성 완료 - Address: {}", address);
	}
}

