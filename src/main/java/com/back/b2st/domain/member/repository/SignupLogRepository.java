package com.back.b2st.domain.member.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.b2st.domain.member.entity.SignupLog;

public interface SignupLogRepository extends JpaRepository<SignupLog, Long> {

	/**
	 * 특정 이메일 최근 가입 기록 조회
	 */
	List<SignupLog> findTop10ByEmailOrderByCreatedAtDesc(String email);

	/**
	 * 특정 IP에서 일정 시간 내 가입 횟수 조회
	 * - 복합 인덱스
	 * - 봇, 다중 계정 생성 탐지차원
	 */
	@Query("SELECT COUNT(s) FROM SignupLog s " +
		"WHERE s.clientIp = :ip AND s.createdAt >= :since")
	Long countByClientIpSince(@Param("ip") String clientUp, @Param("since") LocalDateTime since);

	/**
	 * 특정 IP에서 일정 시간 내 생성된 이메일 목록 조회
	 * - 다중 계정 분석
	 * - 복합 인덱스
	 */
	@Query("SELECT DISTINCT s.email FROM SignupLog s " +
		"WHERE s.clientIp = :ip AND s.createdAt >= :since")
	List<String> findDistinctEmailsByIpSince(@Param("ip") String clientIp, @Param("since") LocalDateTime since);
}
