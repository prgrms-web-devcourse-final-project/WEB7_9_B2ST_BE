package com.back.b2st.domain.member.service;

import static com.back.b2st.global.util.MaskingUtil.*;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.auth.repository.RefreshTokenRepository;
import com.back.b2st.domain.member.dto.request.PasswordChangeReq;
import com.back.b2st.domain.member.dto.request.SignupReq;
import com.back.b2st.domain.member.dto.request.WithdrawReq;
import com.back.b2st.domain.member.dto.response.MyInfoRes;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.error.MemberErrorCode;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.member.repository.RefundAccountRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenRepository refreshTokenRepository;
	private final RefundAccountRepository refundAccountRepository;

	/**
	 * 회원가입 처리 - 이메일 중복 검사 + BCrypt 암호화 + 기본 Role 설정 + 개인정보 마스킹 로그
	 *
	 * @param request 회원가입 요청 정보
	 * @return 생성된 회원 ID
	 */
	@Transactional
	public Long signup(SignupReq request) {
		validateEmail(request);

		Member member = Member.builder()
			.email(request.email())
			.password(passwordEncoder.encode(request.password()))
			.name(request.name())
			.phone(request.phone())
			.birth(request.birth())
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(false)
			.isIdentityVerified(false)
			.build();

		log.info("새로운 회원 가입: ID={}, Email={}", member.getId(), maskEmail(member.getEmail()));

		return memberRepository.save(member).getId();
	}

	/**
	 * 회원 정보 조회 - 회원 조회 + DTO 변환(from 팩토리)
	 *
	 * @param memberId 회원 ID
	 * @return 회원 정보
	 */
	@Transactional(readOnly = true)
	public MyInfoRes getMyInfo(Long memberId) {
		Member member = validateMember(memberId);

		return MyInfoRes.from(member);
	}

	/**
	 * 비밀번호 변경 - 현재 비밀번호 검증 + 동일 비밀번호 방지 + BCrypt 재암호화
	 *
	 * @param memberId 회원 ID
	 * @param request  비밀번호 변경 요청 정보
	 */
	@Transactional
	public void changePassword(Long memberId, PasswordChangeReq request) {
		Member member = validateMember(memberId);
		validatePasswordChange(request, member);

		member.updatePassword(passwordEncoder.encode(request.newPassword()));
		log.info("비밀번호 변경 완료: MemberID={}", memberId);
	}

	/**
	 * 회원 탈퇴 처리 - Soft Delete + Redis 토큰 삭제 + 환불 계좌 삭제 + 마스킹 로그
	 *
	 * @param memberId 회원 ID
	 * @param request  탈퇴 요청 정보 (비밀번호)
	 */
	@Transactional
	public void withdraw(Long memberId, WithdrawReq request) {
		Member member = validateMember(memberId);

		validateNotWithdrawn(member);
		validateCurrentPassword(request.password(), member);

		refreshTokenRepository.deleteById(member.getEmail());
		refundAccountRepository.findByMember(member).ifPresent(refundAccountRepository::delete);

		member.softDelete();

		log.info("회원 탈퇴 처리 완료: MemberID={}, Email={}", memberId, maskEmail(member.getEmail()));
	}

	// 밑으로 validate 모음
	private Member validateMember(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	private void validateEmail(SignupReq request) {
		if (memberRepository.existsByEmail(request.email())) {
			throw new BusinessException(MemberErrorCode.DUPLICATE_EMAIL);
		}
	}

	private void validateCurrentPassword(String currentPassword, Member member) {
		if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
			throw new BusinessException(MemberErrorCode.PASSWORD_MISMATCH);
		}
	}

	private void ensurePasswordDiffer(PasswordChangeReq request) {
		if (request.newPassword().equals(request.currentPassword())) {
			throw new BusinessException(MemberErrorCode.SAME_PASSWORD);
		}
	}

	private void validatePasswordChange(PasswordChangeReq request, Member member) {
		validateCurrentPassword(request.currentPassword(), member);
		ensurePasswordDiffer(request);
	}

	private void validateNotWithdrawn(Member member) {
		if (member.isDeleted()) {
			throw new BusinessException(MemberErrorCode.ALREADY_WITHDRAWN);
		}
	}
}
