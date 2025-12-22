package com.back.b2st.domain.member.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.b2st.domain.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
	Optional<Member> findByEmail(String email);

	boolean existsByEmail(String email);

	// 탈퇴 처리 후 일정 기간 지난 회원 조회 (익명화 대상)
	@Query("SELECT m FROM Member m WHERE m.deletedAt IS NOT NULL AND m.deletedAt < :threshold")
	List<Member> findAllByDeletedAtBefore(@Param("threshold") LocalDateTime threshold);

	Optional<Member> findByProviderId(String providerId);
}
