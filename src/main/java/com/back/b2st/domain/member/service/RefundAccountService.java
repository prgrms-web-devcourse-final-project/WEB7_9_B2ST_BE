package com.back.b2st.domain.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.member.dto.RefundAccountReq;
import com.back.b2st.domain.member.dto.RefundAccountRes;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.entity.RefundAccount;
import com.back.b2st.domain.member.error.MemberErrorCode;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.member.repository.RefundAccountRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefundAccountService {

	private final RefundAccountRepository refundAccountRepository;
	private final MemberRepository memberRepository;

	@Transactional
	public void saveAccount(Long memberId, RefundAccountReq request) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

		refundAccountRepository.findByMember(member).ifPresentOrElse(
			existingAccount -> existingAccount.updateAccount(
				request.getBankCode(),
				request.getAccountNumber(),
				request.getHolderName()
			),
			() -> {
				RefundAccount newAccount = RefundAccount.builder()
					.member(member)
					.bankCode(request.getBankCode())
					.accountNumber(request.getAccountNumber())
					.holderName(request.getHolderName())
					.build();
				refundAccountRepository.save(newAccount);
			}
		);
	}

	@Transactional(readOnly = true)
	public RefundAccountRes getAccount(Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

		return refundAccountRepository.findByMember(member)
			.map(RefundAccountRes::from)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.REFUND_ACCOUNT_NOT_FOUND));
	}
}
