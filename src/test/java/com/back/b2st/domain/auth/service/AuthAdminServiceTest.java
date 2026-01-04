package com.back.b2st.domain.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.back.b2st.domain.auth.dto.response.LockedAccountRes;
import com.back.b2st.domain.auth.dto.response.LoginLogAdminRes;
import com.back.b2st.domain.auth.dto.response.SignupLogAdminRes;
import com.back.b2st.domain.auth.entity.LoginLog;
import com.back.b2st.domain.auth.error.AuthErrorCode;
import com.back.b2st.domain.auth.repository.LoginLogRepository;
import com.back.b2st.domain.member.entity.SignupLog;
import com.back.b2st.domain.member.repository.SignupLogRepository;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class AuthAdminServiceTest {

	private static final String TEST_EMAIL = "test@example.com";
	@InjectMocks
	private AuthAdminService authAdminService;
	@Mock
	private LoginLogRepository loginLogRepository;
	@Mock
	private SignupLogRepository signupLogRepository;
	@Mock
	private StringRedisTemplate redisTemplate;
	@Mock
	private Cursor<String> cursor;

	@Nested
	@DisplayName("로그인 로그 조회")
	class GetLoginLogsTest {

		@Test
		@DisplayName("성공 - 필터 없이 조회")
		void success_noFilter() {
			// given
			Pageable pageable = PageRequest.of(0, 50);
			LoginLog log = LoginLog.builder()
				.email(TEST_EMAIL)
				.clientIp("192.168.1.1")
				.success(true)
				.build();
			Page<LoginLog> logPage = new PageImpl<>(List.of(log), pageable, 1);

			given(loginLogRepository.searchLogs(isNull(), isNull(), isNull(), any(LocalDateTime.class), eq(pageable)))
				.willReturn(logPage);

			// when
			Page<LoginLogAdminRes> result = authAdminService.getLoginLogs(null, null, null, 24, pageable);

			// then
			assertThat(result.getContent()).hasSize(1);
			assertThat(result.getContent().get(0).email()).isEqualTo(TEST_EMAIL);
		}

		@Test
		@DisplayName("성공 - 이메일 필터")
		void success_filterByEmail() {
			// given
			Pageable pageable = PageRequest.of(0, 50);
			LoginLog log = LoginLog.builder()
				.email(TEST_EMAIL)
				.clientIp("192.168.1.1")
				.success(true)
				.build();
			Page<LoginLog> logPage = new PageImpl<>(List.of(log), pageable, 1);

			given(loginLogRepository.searchLogs(eq(TEST_EMAIL), isNull(), isNull(), any(LocalDateTime.class),
				eq(pageable)))
				.willReturn(logPage);

			// when
			Page<LoginLogAdminRes> result = authAdminService.getLoginLogs(TEST_EMAIL, null, null, 24, pageable);

			// then
			assertThat(result.getContent()).hasSize(1);
		}

		@Test
		@DisplayName("성공 - 실패 로그만 조회")
		void success_filterByFailure() {
			// given
			Pageable pageable = PageRequest.of(0, 50);
			LoginLog log = LoginLog.builder()
				.email(TEST_EMAIL)
				.clientIp("192.168.1.1")
				.success(false)
				.failReason(LoginLog.FailReason.INVALID_PASSWORD)
				.build();
			Page<LoginLog> logPage = new PageImpl<>(List.of(log), pageable, 1);

			given(loginLogRepository.searchLogs(isNull(), isNull(), eq(false), any(LocalDateTime.class), eq(pageable)))
				.willReturn(logPage);

			// when
			Page<LoginLogAdminRes> result = authAdminService.getLoginLogs(null, null, false, 24, pageable);

			// then
			assertThat(result.getContent()).hasSize(1);
			assertThat(result.getContent().get(0).success()).isFalse();
		}

		@Test
		@DisplayName("성공 - 빈 결과")
		void success_emptyResult() {
			// given
			Pageable pageable = PageRequest.of(0, 50);
			Page<LoginLog> emptyPage = new PageImpl<>(List.of(), pageable, 0);

			given(loginLogRepository.searchLogs(any(), any(), any(), any(LocalDateTime.class), eq(pageable)))
				.willReturn(emptyPage);

			// when
			Page<LoginLogAdminRes> result = authAdminService.getLoginLogs("nonexistent", null, null, 24, pageable);

			// then
			assertThat(result.getContent()).isEmpty();
		}
	}

	@Nested
	@DisplayName("가입 로그 조회")
	class GetSignupLogsTest {

		@Test
		@DisplayName("성공")
		void success() {
			// given
			Pageable pageable = PageRequest.of(0, 50);
			SignupLog log = SignupLog.builder().email(TEST_EMAIL).clientIp("192.168.1.1").build();
			Page<SignupLog> logPage = new PageImpl<>(List.of(log), pageable, 1);

			given(signupLogRepository.findByCreatedAtAfter(any(LocalDateTime.class), eq(pageable)))
				.willReturn(logPage);

			// when
			Page<SignupLogAdminRes> result = authAdminService.getSignupLogs(24, pageable);

			// then
			assertThat(result.getContent()).hasSize(1);
			assertThat(result.getContent().get(0).email()).isEqualTo(TEST_EMAIL);
		}

		@Test
		@DisplayName("성공 - 시간 범위 지정")
		void success_withHours() {
			// given
			Pageable pageable = PageRequest.of(0, 50);
			Page<SignupLog> emptyPage = new PageImpl<>(List.of(), pageable, 0);

			given(signupLogRepository.findByCreatedAtAfter(any(LocalDateTime.class), eq(pageable)))
				.willReturn(emptyPage);

			// when
			Page<SignupLogAdminRes> result = authAdminService.getSignupLogs(1, pageable);

			// then
			assertThat(result.getContent()).isEmpty();
			then(signupLogRepository).should().findByCreatedAtAfter(any(LocalDateTime.class), eq(pageable));
		}
	}

	@Nested
	@DisplayName("잠긴 계정 목록 조회")
	class GetLockedAccountsTest {

		@Test
		@DisplayName("성공 - 잠긴 계정 있음")
		void success_hasLockedAccounts() {
			// given
			given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
			given(cursor.hasNext()).willReturn(true, true, false);
			given(cursor.next()).willReturn("login:lock:user1@test.com", "login:lock:user2@test.com");
			given(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).willReturn(300L, 600L);

			// when
			List<LockedAccountRes> result = authAdminService.getLockedAccounts();

			// then
			assertThat(result).hasSize(2);
			assertThat(result.get(0).email()).isEqualTo("user1@test.com");
			assertThat(result.get(0).remainingSeconds()).isEqualTo(300L);
		}

		@Test
		@DisplayName("성공 - 잠긴 계정 없음")
		void success_noLockedAccounts() {
			// given
			given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
			given(cursor.hasNext()).willReturn(false);

			// when
			List<LockedAccountRes> result = authAdminService.getLockedAccounts();

			// then
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("TTL이 0 이하인 키는 제외")
		void excludeExpiredKeys() {
			// given
			given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
			given(cursor.hasNext()).willReturn(true, false);
			given(cursor.next()).willReturn("login:lock:expired@test.com");
			given(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).willReturn(0L);

			// when
			List<LockedAccountRes> result = authAdminService.getLockedAccounts();

			// then
			assertThat(result).isEmpty();
		}
	}

	@Nested
	@DisplayName("계정 잠금 해제")
	class UnlockAccountTest {

		@Test
		@DisplayName("성공")
		void success() {
			// given
			given(redisTemplate.hasKey("login:lock:" + TEST_EMAIL)).willReturn(true);
			given(redisTemplate.delete("login:lock:" + TEST_EMAIL)).willReturn(true);
			given(redisTemplate.delete("login:attempt:" + TEST_EMAIL)).willReturn(true);

			// when & then
			assertThatCode(() -> authAdminService.unlockAccount(1L, TEST_EMAIL))
				.doesNotThrowAnyException();

			then(redisTemplate).should().delete("login:lock:" + TEST_EMAIL);
			then(redisTemplate).should().delete("login:attempt:" + TEST_EMAIL);
		}

		@Test
		@DisplayName("실패 - 잠금 상태가 아닌 계정")
		void fail_notLocked() {
			// given
			given(redisTemplate.hasKey("login:lock:" + TEST_EMAIL)).willReturn(false);

			// when & then
			assertThatThrownBy(() -> authAdminService.unlockAccount(1L, TEST_EMAIL))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.ACCOUNT_NOT_LOCKED);
		}
	}

	@Nested
	@DisplayName("잠긴 계정 수 조회")
	class GetLockedAccountCountTest {

		@Test
		@DisplayName("성공")
		void success() {
			// given
			given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
			given(cursor.hasNext()).willReturn(true, true, false);
			given(cursor.next()).willReturn("login:lock:user1@test.com", "login:lock:user2@test.com");
			given(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).willReturn(300L, 600L);

			// when
			int count = authAdminService.getLockedAccountCount();

			// then
			assertThat(count).isEqualTo(2);
		}
	}
}
