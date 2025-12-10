package com.back.b2st.domain.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.member.dto.SignupRequest;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public Long signup(SignupRequest request) {
		if (memberRepository.existsByEmail(request.getEmail())) {
			throw new IllegalArgumentException("이미 가입된 이메일입니다.");
		}
		if (memberRepository.existsByNickname(request.getNickname())) {
			throw new IllegalArgumentException("이미 존재하는 닉네임입니다.");
		}

		// 비밀번호 암호화 및 엔티티 생성
		Member member = Member.builder()
			.email(request.getEmail())
			.password(passwordEncoder.encode(request.getPassword()))
			.name(request.getName())
			.nickname(request.getNickname())
			.phone(request.getPhone())
			.birth(request.getBirth())
			.role(Member.Role.MEMBER) // 기본 가입은 MEMBER
			.provider(Member.Provider.EMAIL)
			.isVerified(false) // 초기엔 미인증
			.build();

		// 저장
		return memberRepository.save(member).getId();
	}
}