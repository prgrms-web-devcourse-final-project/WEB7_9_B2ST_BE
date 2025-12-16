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
import com.back.b2st.global.util.MaskingUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public Long signup(SignupReq request) {
		validateEmail(request);

		// 비밀번호 암호화 및 엔티티 생성
		Member member = Member.builder()
			.email(request.email())
			.password(passwordEncoder.encode(request.password()))
			.name(request.name())
			.phone(request.phone())
			.birth(request.birth())
			.role(Member.Role.MEMBER) // 기본 가입은 MEMBER
			.provider(Member.Provider.EMAIL)
			.isVerified(false) // 초기엔 미인증
			.build();

		log.info("새로운 회원 가입: ID={}, Email={}", member.getId(), MaskingUtil.maskEmail(member.getEmail()));

		return memberRepository.save(member).getId();
	}

	@Transactional(readOnly = true)
	public MyInfoRes getMyInfo(Long memberId) {
		Member member = validateMember(memberId);

		return MyInfoRes.from(member);
	}

	@Transactional
	public void changePassword(Long memberId, PasswordChangeReq request) {
		Member member = validateMember(memberId);
		validatePasswordChange(request, member);

		member.updatePassword(passwordEncoder.encode(request.newPassword()));
		log.info("비밀번호 변경 완료: MemberID={}", memberId);
	}

	private Member validateMember(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	private void validateEmail(SignupReq request) {
		if (memberRepository.existsByEmail(request.email())) {
			throw new BusinessException(MemberErrorCode.DUPLICATE_EMAIL);
		}
	}

	private void validateCurrentPassword(PasswordChangeReq request, Member member) {
		if (!passwordEncoder.matches(request.currentPassword(), member.getPassword())) {
			throw new BusinessException(MemberErrorCode.PASSWORD_MISMATCH);
		}
	}

	private void ensurePasswordDiffer(PasswordChangeReq request) {
		if (request.newPassword().equals(request.currentPassword())) {
			throw new BusinessException(MemberErrorCode.SAME_PASSWORD);
		}
	}

	// 비번변경 api 검증 상위 메서드. 쓸데없지만 퍼사드 패턴 숙달을 위해 묶어봄
	private void validatePasswordChange(PasswordChangeReq request, Member member) {
		validateCurrentPassword(request, member);
		ensurePasswordDiffer(request);
	}
}
