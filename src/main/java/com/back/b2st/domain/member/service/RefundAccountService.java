package com.back.b2st.domain.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.member.dto.request.RefundAccountReq;
import com.back.b2st.domain.member.dto.response.RefundAccountRes;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.entity.RefundAccount;
import com.back.b2st.domain.member.error.MemberErrorCode;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.member.repository.RefundAccountRepository;
import com.back.b2st.global.error.exception.BusinessException;
import com.back.b2st.global.util.MaskingUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundAccountService {

	private final RefundAccountRepository refundAccountRepository;
	private final MemberRepository memberRepository;

	// Upsert 패턴(ifPresentOrElse) + 계좌번호 마스킹 로그
	@Transactional
	public void saveAccount(Long memberId, RefundAccountReq request) {
		Member member = validateMember(memberId);

		refundAccountRepository.findByMember(member).ifPresentOrElse(
				existingAccount -> existingAccount.updateAccount(
						request.bankCode(),
						request.accountNumber(),
						request.holderName()),
				() -> {
					RefundAccount newAccount = RefundAccount.builder()
							.member(member)
							.bankCode(request.bankCode())
							.accountNumber(request.accountNumber())
							.holderName(request.holderName())
							.build();
					refundAccountRepository.save(newAccount);
				});
		log.info("환불 계좌 등록/수정: MemberID={}, BankCode={}, AccountNum={}", memberId, request.bankCode(),
				MaskingUtil.maskAccountNumber(request.accountNumber()));
	}

	// 회원 검증 + DTO 변환(from 팩토리) + 예외 로깅
	@Transactional(readOnly = true)
	public RefundAccountRes getAccount(Long memberId) {

		try {
			Member member = validateMember(memberId);

			return refundAccountRepository.findByMember(member)
					.map(RefundAccountRes::from)
					.orElseThrow(() -> new BusinessException(MemberErrorCode.REFUND_ACCOUNT_NOT_FOUND));
		} catch (Exception e) {
			log.error("환불 계좌 조회 중 오류 발생: MemberID={}", memberId, e);
			throw e;
		}
	}

	// 밑으로 validate 모음
	private Member validateMember(Long memberId) {
		return memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
	}
}
