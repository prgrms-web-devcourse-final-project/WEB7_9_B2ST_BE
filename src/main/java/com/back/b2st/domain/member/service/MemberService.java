package com.back.b2st.domain.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.member.dto.request.PasswordChangeReq;
import com.back.b2st.domain.member.dto.request.SignupReq;
import com.back.b2st.domain.member.dto.response.MyInfoRes;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.error.MemberErrorCode;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public Long signup(SignupReq request) {
		if (memberRepository.existsByEmail(request.getEmail())) {
			throw new BusinessException(MemberErrorCode.DUPLICATE_EMAIL);
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
	public MyInfoRes getMyInfo(Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

		return MyInfoRes.from(member);
	}

	public void changePassword(Long memberId, PasswordChangeReq request) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

		if (!passwordEncoder.matches(request.getCurrentPassword(), member.getPassword())) {
			throw new BusinessException(MemberErrorCode.PASSWORD_MISMATCH);
		}

		if (request.getNewPassword().equals(request.getCurrentPassword())) {
			throw new BusinessException(MemberErrorCode.SAME_PASSWORD);
		}

		member.updatePassword(passwordEncoder.encode(request.getNewPassword()));
	}
}
