package com.back.b2st.domain.member.entity;

import static com.back.b2st.domain.member.fixture.MemberTestFixture.*;
import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MemberTest {

	private Member member;

	@BeforeEach
	void setUp() {
		member = createMember("test@test.com", "encodedPassword", "테스트");
	}

	@Nested
	@DisplayName("Soft Delete 관련")
	class SoftDeleteTest {

		@Test
		@DisplayName("softDelete 호출 시 deletedAt이 설정된다")
		void softDelete_setsDeletedAt() {
			member.softDelete();

			assertThat(member.isDeleted()).isTrue();
			assertThat(member.getDeletedAt()).isNotNull();
		}

		@Test
		@DisplayName("삭제되지 않은 회원은 isDeleted가 false")
		void isDeleted_returnsFalse_whenNotDeleted() {
			assertThat(member.isDeleted()).isFalse();
		}
	}

	@Nested
	@DisplayName("탈퇴 철회 관련")
	class CancelWithdrawalTest {

		@Test
		@DisplayName("cancelWithdrawal 호출 시 deletedAt이 null이 된다")
		void cancelWithdrawal_clearsDeletedAt() {
			member.softDelete();

			member.cancelWithdrawal();

			assertThat(member.isDeleted()).isFalse();
			assertThat(member.getDeletedAt()).isNull();
		}
	}

	@Nested
	@DisplayName("익명화 관련")
	class AnonymizeTest {

		@Test
		@DisplayName("anonymize 호출 시 개인정보가 익명화된다")
		void anonymize_clearsPersonalInfo() {
			member.anonymize();

			assertThat(member.getEmail()).startsWith("withdrawn_");
			assertThat(member.getPassword()).isNull();
			assertThat(member.getName()).isEqualTo("탈퇴회원");
			assertThat(member.getPhone()).isNull();
			assertThat(member.getBirth()).isNull();
		}
	}

	@Nested
	@DisplayName("기타 메서드")
	class OtherMethodsTest {

		@Test
		@DisplayName("verifyEmail 호출 시 이메일 인증 완료")
		void verifyEmail_success() {
			member.verifyEmail();

			assertThat(member.isEmailVerified()).isTrue();
		}

		@Test
		@DisplayName("updatePassword 호출 시 비밀번호 변경")
		void updatePassword_success() {
			member.updatePassword("newEncodedPassword");

			assertThat(member.getPassword()).isEqualTo("newEncodedPassword");
		}
	}
}
