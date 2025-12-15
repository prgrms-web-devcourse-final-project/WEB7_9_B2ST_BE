package com.back.b2st.domain.member.entity;

import com.back.b2st.global.common.BankCode;
import com.back.b2st.global.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "refund_accounts")
@SequenceGenerator(
	name = "refund_account_id_gen",
	sequenceName = "refund_accounts_seq",
	allocationSize = 50
)
public class RefundAccount extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "refund_account_id_gen")
	@Column(name = "account_id")
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, unique = true)
	private Member member;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BankCode bankCode;

	@Column(nullable = false)
	private String accountNumber;

	@Column(nullable = false)
	private String holderName;

	@Builder
	public RefundAccount(Member member, BankCode bankCode, String accountNumber, String holderName) {
		this.member = member;
		this.bankCode = bankCode; // 수정
		this.accountNumber = accountNumber;
		this.holderName = holderName;
	}

	public void updateAccount(BankCode bankCode, String accountNumber, String holderName) {
		this.bankCode = bankCode; // 수정
		this.accountNumber = accountNumber;
		this.holderName = holderName;
	}
}
