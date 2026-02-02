package com.back.b2st.domain.member.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	/**
	 * 회원 검색 - 필터링 + 페이징
	 */
	@Query("""
		  SELECT m FROM Member m
		  WHERE (:email IS NULL OR m.email LIKE %:email%)
		  AND (:name IS NULL OR m.name LIKE %:name%)
		  AND (:role IS NULL OR m.role = :role)
		  AND (:isDeleted IS NULL\s
		       OR (:isDeleted = true AND m.deletedAt IS NOT NULL)\s
		       OR (:isDeleted = false AND m.deletedAt IS NULL))
		  ORDER BY m.createdAt DESC
		""")
	Page<Member> searchMembers(
		@Param("email") String email,
		@Param("name") String name,
		@Param("role") Member.Role role,
		@Param("isDeleted") Boolean isDeleted,
		Pageable pageable
	);

	long countByDeletedAtIsNull();

	long countByDeletedAtIsNotNull();

	long countByRole(Member.Role role);

	/**
	 * 특정 시간 이후의 회원 가입 수 조회
	 */
	@Query("SELECT COUNT(m) FROM Member m WHERE m.createdAt >= :since")
	long countByCreatedAtAfter(@Param("since") LocalDateTime since);

}
