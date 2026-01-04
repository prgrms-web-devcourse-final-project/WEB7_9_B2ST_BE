package com.back.b2st.domain.auth.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.b2st.domain.auth.entity.LoginLog;

public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {

	/**
	 * 특정 이메일 최근 로그인 기록 조회
	 */
	List<LoginLog> findTop10ByEmailOrderByAttemptedAtDesc(String email);

	/**
	 * 특정 IP에서 일정 시간 내 실패 횟수 조회
	 */
	@Query("SELECT COUNT(l) FROM LoginLog l " +
		"WHERE l.clientIp = :ip AND l.success = false " +
		"AND l.attemptedAt > :since")
	long countFailedAttemptsByIpSince(@Param("ip") String clientIp, @Param("since") LocalDateTime since);

	/**
	 * 특정 IP에서 일정 시간 내 시도한 이메일 수 조회
	 * (Credential stuffing 탐지용)
	 */
	@Query("SELECT COUNT(DISTINCT l.email) FROM LoginLog l " +
		"WHERE l.clientIp = :ip AND l.attemptedAt > :since")
	long countDistinctEmailsByIpSince(@Param("ip") String clientIp, @Param("since") LocalDateTime since);

	/**
	 * 로그인 로그 검색 - 필터링 + 시간 범위 + 페이징
	 */
	@Query("""
		  SELECT l FROM LoginLog l
		  WHERE (:email IS NULL OR l.email LIKE %:email%)
		  AND (:clientIp IS NULL OR l.clientIp = :clientIp)
		  AND (:success IS NULL OR l.success = :success)
		  AND l.attemptedAt >= :since
		  ORDER BY l.attemptedAt DESC
		""")
	Page<LoginLog> searchLogs(
		@Param("email") String email,
		@Param("clientIp") String clientIp,
		@Param("success") Boolean success,
		@Param("since") LocalDateTime since,
		Pageable pageable
	);

	/**
	 * 특정 시간 이후의 로그인 시도 횟수 조회
	 */
	@Query("SELECT COUNT(l) FROM LoginLog l WHERE l.attemptedAt >= :since")
	long countByAttemptedAtAfter(@Param("since") LocalDateTime since);

	/**
	 * 특정 시간 이후의 로그인 실패 횟수 조회
	 */
	@Query("SELECT COUNT(l) FROM LoginLog l WHERE l.success = false AND l.attemptedAt >= :since")
	long countFailuresByAttemptedAtAfter(@Param("since") LocalDateTime since);
	
	/**
	 * 특정 시간 이후 활성 IP 목록 조회
	 */
	@Query("SELECT DISTINCT l.clientIp FROM LoginLog l WHERE l.attemptedAt >= :since")
	List<String> findDistinctClientIpsSince(@Param("since") LocalDateTime since);
}
