package com.back.b2st.domain.member.service;

import static com.back.b2st.global.util.MaskingUtil.*;

import java.time.LocalDateTime;

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
			.isEmailVerified(false) // 이메일 미인증
			.isIdentityVerified(false) // 본인인증 미완료
			.build();

		log.info("새로운 회원 가입: ID={}, Email={}", member.getId(), maskEmail(member.getEmail()));

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

	public void cancelWithdrawal(Long memberId) {
		Member member = validateMember(memberId);

		validateWithdrawalCancellation(member);

		member.softDelete();
		log.info("탈퇴 철회 완료: MemberID={}", memberId);
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

	// 비번변경 api 검증 상위 메서드. 쓸데없지만 퍼사드 패턴 숙달 차원에서 묶어봄
	private void validatePasswordChange(PasswordChangeReq request, Member member) {
		validateCurrentPassword(request.currentPassword(), member);
		ensurePasswordDiffer(request);
	}

	private void validateNotWithdrawn(Member member) {
		if (member.isDeleted()) {
			throw new BusinessException(MemberErrorCode.ALREADY_WITHDRAWN);
		}
	}

	private void validateAlreadyWithdrawn(Member member) {
		if (!member.isDeleted()) {
			throw new BusinessException(MemberErrorCode.NOT_WITHDRAWN);
		}
	}

	private void validateWithdrawalGracePeriod(Member member) {
		if (member.getDeletedAt().plusDays(30).isBefore(LocalDateTime.now())) {
			throw new BusinessException(MemberErrorCode.WITHDRAWAL_PERIOD_EXPIRED);
		}
	}

	// 탈퇴 철회 검증 상위 메서드
	private void validateWithdrawalCancellation(Member member) {
		validateAlreadyWithdrawn(member);
		validateWithdrawalGracePeriod(member);
	}
}
