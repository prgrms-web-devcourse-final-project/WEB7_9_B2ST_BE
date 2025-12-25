package com.back.b2st.domain.auth.service;

import static com.back.b2st.global.util.MaskingUtil.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.back.b2st.domain.auth.error.AuthErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 로그인 보안 서비스
 * - 로그인 시도 Rate Limiting
 * - 계정 잠금/해제
 * - 로그인 감사 로깅
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginSecurityService {

	// 세팅 상수
	private static final int MAX_ATTEMPTS = 5; // 최대 로그인 시도 횟수
	private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(10); // 계정 잠금 시간
	private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(10); // 시도 횟수 윈도우
	// Redis 키 접두사
	private static final String ATTEMPT_KEY_PREFIX = "login:attempt:";
	private static final String LOCK_KEY_PREFIX = "login:lock:";
	// Lua 스크립트
	private static final String INCREMENT_ATTEMPT_SCRIPT = "local count = redis.call('INCR', KEYS[1]) " +
		"if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " + // 첫 시도면 만료시간 설정
		"return count";
	private final StringRedisTemplate redisTemplate;
	private final DefaultRedisScript<Long> incrementScript = createIncrementScript();

	private DefaultRedisScript<Long> createIncrementScript() {
		DefaultRedisScript<Long> script = new DefaultRedisScript<>();
		script.setScriptText(INCREMENT_ATTEMPT_SCRIPT);
		script.setResultType(Long.class);
		return script;
	}

	/**
	 * 로그인 전 계정 잠금 상태 확인
	 * 잠겨있으면 BusinessException 발생
	 *
	 * @param email 확인할 이메일
	 * @throws BusinessException ACCOUNT_LOCKED
	 */
	public void checkAccountLock(String email) {
		String lockKey = LOCK_KEY_PREFIX + email;

		if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) { // null 방지
			Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
			int remainingMinutes = ttl != null ? (int)Math.ceil(ttl / 60.0) : 0;
			// 내부 로그에만 잠금 정보 기록 (운영용)
			log.warn("🔒 잠긴 계정 로그인 시도: email={}, 남은시간={}분", maskEmail(email), remainingMinutes);
			// 클라이언트에는 일반 로그인 실패로 응답 (보안: 계정 잠금 상태 노출 방지)
			throw new BusinessException(AuthErrorCode.LOGIN_FAILED);
		}
	}

	/**
	 * 로그인 실패 기록
	 * - 시도 횟수 증가
	 * - 최대 시도 초과 시 계정 잠금
	 *
	 * @param email    실패한 이메일
	 * @param clientIp 클라이언트 IP
	 */
	public void recordFailedAttempt(String email, String clientIp) {
		String attemptKey = ATTEMPT_KEY_PREFIX + email;

		// 원자적으로 시도 횟수 증가 + TTL 설정
		Long attempts = redisTemplate.execute(
			incrementScript,
			List.of(attemptKey),
			String.valueOf(ATTEMPT_WINDOW.getSeconds()));

		if (attempts == null) {
			attempts = 1L;
		}

		log.info("로그인 실패: email={}, IP={}, 시도횟수={}/{}", maskEmail(email), clientIp, attempts, MAX_ATTEMPTS);

		// 최대 시도 초과 시 계정 잠금
		if (attempts >= MAX_ATTEMPTS) {
			lockAccount(email);
			// 내부 로그에만 잠금 정보 기록 (운영용)
			log.warn("🔒 계정 잠금 발생: email={}, IP={}, 잠금시간={}분", maskEmail(email), clientIp,
				LOCKOUT_DURATION.toMinutes());
			// 클라이언트에는 일반 로그인 실패로 응답 (보안: 계정 잠금 상태 노출 방지)
			throw new BusinessException(AuthErrorCode.LOGIN_FAILED);
		}
	}

	/**
	 * 로그인 성공 처리
	 * - 시도 횟수 초기화
	 *
	 * @param email    성공한 이메일
	 * @param clientIp 클라이언트IP
	 */
	public void onLoginSuccess(String email, String clientIp) {
		String attemptKey = ATTEMPT_KEY_PREFIX + email;
		redisTemplate.delete(attemptKey);
		log.info("로그인 성공: email={}, IP={}", maskEmail(email), clientIp);
	}

	/**
	 * 현재 실패 시도 횟수 조회
	 *
	 * @param email 조회할 이메일
	 * @return 현재 실패 횟수(없으면 0)
	 */
	public int getFailedAttemptCount(String email) {
		String attemptKey = ATTEMPT_KEY_PREFIX + email;
		String value = redisTemplate.opsForValue().get(attemptKey);
		return value != null ? Integer.parseInt(value) : 0;
	}

	/**
	 * 남은 시도 횟수 조회
	 *
	 * @param email 조회할 이메일
	 * @return 남은 시도횟수
	 */
	public int getRemainingAttempts(String email) {
		return Math.max(0, MAX_ATTEMPTS - getFailedAttemptCount(email));
	}

	/**
	 * 계정 잠금 여부 확인
	 *
	 * @param email 확인할 이메일
	 * @return 잠금 상태
	 */
	public boolean isAccountLocked(String email) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_KEY_PREFIX + email));
	}

	// 밑으로 헬퍼 메서드

	/**
	 * 계정 잠금 처리
	 */
	private void lockAccount(String email) {
		String lockKey = LOCK_KEY_PREFIX + email;
		redisTemplate.opsForValue().set(lockKey, "locked", LOCKOUT_DURATION.getSeconds(), TimeUnit.SECONDS);
	}

}
