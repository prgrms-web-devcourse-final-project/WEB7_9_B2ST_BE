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
 *
 * 지원 모드:
 * - single: 단일 Redis 서버
 * - sentinel: Redis Sentinel
 * - cluster: Redis Cluster
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

		// Redisson 4.0: Config 레벨에서 password, TCP 설정
		if (StringUtils.hasText(redisPassword)) {
			config.setPassword(redisPassword);
		}
		config.setTcpKeepAlive(true);
		config.setTcpNoDelay(true);

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
			.setDatabase(0)
			// Master 연결 풀 설정
			.setMasterConnectionPoolSize(10)
			.setMasterConnectionMinimumIdleSize(2)
			// Slave 연결 풀 설정 (읽기 부하 분산)
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
	 * Cluster 모드 설정 (대규모 트래픽 최적화)
	 *
	 * 수평 확장 지원 (샤딩)
	 * 최소 6개 노드 권장 (3 Master + 3 Slave)
	 *
	 * 대규모 트래픽 최적화:
	 * - Connection Pool 크기 증가 (64개)
	 * - 타임아웃 최적화
	 * - 읽기 부하 분산 (Slave에서 읽기)
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
			// 클러스터 노드 스캔 간격 (밀리초) - 대규모 트래픽에서는 더 자주 스캔
			.setScanInterval(clusterScanInterval)
			// Master 연결 풀 설정 (대규모 트래픽: 64개)
			.setMasterConnectionPoolSize(clusterMasterConnectionPoolSize)
			.setMasterConnectionMinimumIdleSize(clusterMasterConnectionMinimumIdleSize)
			// Slave 연결 풀 설정 (읽기 부하 분산)
			.setSlaveConnectionPoolSize(clusterSlaveConnectionPoolSize)
			.setSlaveConnectionMinimumIdleSize(clusterSlaveConnectionMinimumIdleSize)
			// 읽기 모드: Master에서만 읽기 (일관성) 또는 Slave에서 읽기 (성능)
			// 일반 읽기는 SLAVE에서 읽어도 되지만, 락은 MASTER 필수
			// 현재는 SLAVE로 설정하되, 락은 별도로 MASTER 연결 사용 (Redisson이 자동 처리)
			.setReadMode(org.redisson.config.ReadMode.SLAVE) // 일반 읽기: Slave에서 읽기로 부하 분산
			// 재시도 설정
			.setRetryAttempts(3)
			.setRetryInterval(1500)
			// 타임아웃 설정 (대규모 트래픽: 더 긴 타임아웃)
			.setTimeout(clusterTimeout)
			.setConnectTimeout(clusterConnectTimeout)
			// 대규모 트래픽 최적화
			.setIdleConnectionTimeout(10000)
			.setPingConnectionInterval(30000)
			// Redisson 4.0+에서 모든 슬롯이 커버되지 않아도 연결 시도
			// (클러스터 초기화 중이거나 cluster-announce-ip 설정이 전파되는 동안 발생할 수 있음)
			.setCheckSlotsCoverage(false);

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
			.setConnectionPoolSize(10)
			.setConnectionMinimumIdleSize(2)
			.setRetryAttempts(3)
			.setRetryInterval(1500);

		log.info("Single Server 구성 완료 - Address: {}", address);
	}
}

