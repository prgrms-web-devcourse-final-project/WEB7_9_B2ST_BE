package com.back.b2st.domain.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.entity.RefundAccount;

public interface RefundAccountRepository extends JpaRepository<RefundAccount, Long> {
	Optional<RefundAccount> findByMember(Member member);
}
