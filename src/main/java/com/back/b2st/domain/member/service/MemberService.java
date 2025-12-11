package com.back.b2st.domain.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.member.dto.MyInfoResponse;
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

		// 비밀번호 암호화 및 엔티티 생성
		Member member = Member.builder()
			.email(request.getEmail())
			.password(passwordEncoder.encode(request.getPassword()))
			.name(request.getName())
			.phone(request.getPhone())
			.birth(request.getBirth())
			.role(Member.Role.MEMBER) // 기본 가입은 MEMBER
			.provider(Member.Provider.EMAIL)
			.isVerified(false) // 초기엔 미인증
			.build();

		// 저장
		return memberRepository.save(member).getId();
	}

	@Transactional(readOnly = true)
	public MyInfoResponse getMyInfo(Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new IllegalArgumentException("해당하는 회원 찾을 수 없습니다."));

		return MyInfoResponse.from(member);
	}
}
