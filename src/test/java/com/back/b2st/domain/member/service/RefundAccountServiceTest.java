package com.back.b2st.domain.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.member.dto.RefundAccountReq;
import com.back.b2st.domain.member.dto.RefundAccountRes;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.entity.RefundAccount;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.member.repository.RefundAccountRepository;
import com.back.b2st.global.common.BankCode;

@ExtendWith(MockitoExtension.class)
public class RefundAccountServiceTest {

	@InjectMocks
	private RefundAccountService refundAccountService;

	@Mock
	private RefundAccountRepository refundAccountRepository;

	@Mock
	private MemberRepository memberRepository;

	@Test
	@DisplayName("계좌 신규 등록 성공")
	void saveAccount_create_success() {
		// given
		Long memberId = 1L;
		RefundAccountReq request = RefundAccountReq.builder()
			.bankCode(BankCode.KB)
			.accountNumber("1234")
			.holderName("홍길동")
			.build();

		Member member = Member.builder().build();

		given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
		given(refundAccountRepository.findByMember(member)).willReturn(Optional.empty()); // 기존 계좌 없음

		// when
		refundAccountService.saveAccount(memberId, request);

		// then
		verify(refundAccountRepository).save(any(RefundAccount.class));
	}

	@Test
	@DisplayName("계좌 수정 성공")
	void saveAccount_update_success() {
		// given
		Long memberId = 1L;
		RefundAccountReq request = RefundAccountReq.builder()
			.bankCode(BankCode.SHINHAN)
			.accountNumber("5678")
			.holderName("홍길동")
			.build();

		Member member = Member.builder().build();
		// 기존 계좌 존재 (국민/1234)
		RefundAccount existingAccount = RefundAccount.builder()
			.member(member)
			.bankCode(BankCode.KB)
			.accountNumber("1234")
			.holderName("홍길동")
			.build();

		given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
		given(refundAccountRepository.findByMember(member)).willReturn(Optional.of(existingAccount));

		// when
		refundAccountService.saveAccount(memberId, request);

		// then
		// 객체 내부 값이 변경되었는지 확인 (Dirty Checking)
		assertThat(existingAccount.getBankCode()).isEqualTo(BankCode.SHINHAN);
		assertThat(existingAccount.getAccountNumber()).isEqualTo("5678");
	}

	@Test
	@DisplayName("계좌 조회 성공")
	void getAccount_success() {
		// given
		Long memberId = 1L;
		Member member = Member.builder().build();
		RefundAccount account = RefundAccount.builder()
			.member(member)
			.bankCode(BankCode.KB)
			.accountNumber("1234")
			.holderName("홍길동")
			.build();

		given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
		given(refundAccountRepository.findByMember(member)).willReturn(Optional.of(account));

		// when
		RefundAccountRes response = refundAccountService.getAccount(memberId);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getAccountNumber()).isEqualTo("1234");
	}
}
