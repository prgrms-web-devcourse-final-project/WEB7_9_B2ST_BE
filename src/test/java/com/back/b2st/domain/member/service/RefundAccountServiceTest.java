package com.back.b2st.domain.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.bank.BankCode;
import com.back.b2st.domain.member.dto.request.RefundAccountReq;
import com.back.b2st.domain.member.dto.response.RefundAccountRes;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.entity.RefundAccount;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.member.repository.RefundAccountRepository;

@ExtendWith(MockitoExtension.class)
class RefundAccountServiceTest {

	@InjectMocks
	private RefundAccountService refundAccountService;

	@Mock
	private RefundAccountRepository refundAccountRepository;

	@Mock
	private MemberRepository memberRepository;

	@Nested
	@DisplayName("계좌 등록/수정")
	class SaveAccountTest {

		@Test
		@DisplayName("신규 등록 성공")
		void create_success() {
			Long memberId = 1L;
			RefundAccountReq request = buildRefundAccountReq();
			Member member = Member.builder().build();

			given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
			given(refundAccountRepository.findByMember(member)).willReturn(Optional.empty());

			refundAccountService.saveAccount(memberId, request);

			verify(refundAccountRepository).save(any(RefundAccount.class));
		}

		@Test
		@DisplayName("수정 성공")
		void update_success() {
			Long memberId = 1L;
			RefundAccountReq request = buildRefundAccountReq();
			Member member = Member.builder().build();
			RefundAccount existingAccount = RefundAccount.builder()
					.member(member)
					.bankCode(BankCode.KB)
					.accountNumber("1234567")
					.holderName("홍길동")
					.build();

			given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
			given(refundAccountRepository.findByMember(member)).willReturn(Optional.of(existingAccount));

			refundAccountService.saveAccount(memberId, request);

			assertThat(existingAccount.getBankCode()).isEqualTo(BankCode.SHINHAN);
			assertThat(existingAccount.getAccountNumber()).isEqualTo(request.accountNumber());
		}
	}

	@Nested
	@DisplayName("계좌 조회")
	class GetAccountTest {

		@Test
		@DisplayName("성공")
		void success() {
			Long memberId = 1L;
			Member member = Member.builder().build();
			RefundAccount account = RefundAccount.builder()
					.member(member)
					.bankCode(BankCode.KB)
					.accountNumber("1234567")
					.holderName("홍길동")
					.build();

			given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
			given(refundAccountRepository.findByMember(member)).willReturn(Optional.of(account));

			RefundAccountRes response = refundAccountService.getAccount(memberId);

			assertThat(response).isNotNull();
			assertThat(response.accountNumber()).isEqualTo(account.getAccountNumber());
		}
	}

	// 헬퍼 메서드
	private RefundAccountReq buildRefundAccountReq() {
		return new RefundAccountReq(BankCode.SHINHAN, "5678901", "홍길동");
	}
}
