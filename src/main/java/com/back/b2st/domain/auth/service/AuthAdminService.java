package com.back.b2st.domain.auth.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.auth.dto.response.LockedAccountRes;
import com.back.b2st.domain.auth.dto.response.LoginLogAdminRes;
import com.back.b2st.domain.auth.dto.response.SignupLogAdminRes;
import com.back.b2st.domain.auth.error.AuthErrorCode;
import com.back.b2st.domain.auth.repository.LoginLogRepository;
import com.back.b2st.domain.member.repository.SignupLogRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthAdminService {

	private static final String LOCK_KEY_PREFIX = "login:lock:";
	private static final String ATTEMPT_KEY_PREFIX = "login:attempt:";

	private final LoginLogRepository loginLogRepository;
	private final SignupLogRepository signupLogRepository;
	private final StringRedisTemplate redisTemplate;

	/**
	 * 관리자용 로그인 로그 조회
	 * - 이메일, 클라이언트 IP, 성공 여부, 최근 N시간 필터링 가능
	 * - 페이지네이션
	 * @param email
	 * @param clientIp
	 * @param success 성공 여부
	 * @param hours
	 * @param pageable
	 * @return 로그인 로그 페이지
	 */
	public Page<LoginLogAdminRes> getLoginLogs(String email, String clientIp, Boolean success, int hours,
		Pageable pageable) {
		// hours 시간 전부터 현재까지의 로그인 로그 조회
		LocalDateTime since = LocalDateTime.now().minusHours(hours);
		return loginLogRepository.searchLogs(email, clientIp, success, since, pageable)
			.map(LoginLogAdminRes::from);
	}

	/**
	 * 관리자용 회원가입 로그 조회
	 * - 최근 N시간 필터링 가능
	 * - 페이지네이션
	 * @param hours
	 * @param pageable
	 * @return 회원가입 로그 페이지
	 */
	public Page<SignupLogAdminRes> getSignupLogs(int hours, Pageable pageable) {
		// hours 시간 전부터 현재까지의 회원가입 로그 조회
		LocalDateTime since = LocalDateTime.now().minusHours(hours);
		return signupLogRepository.findByCreatedAtAfter(since, pageable)
			.map(SignupLogAdminRes::from);
	}

	/**
	 * 잠긴 계정 목록 조회
	 * - Redis SCAN 명령어 사용
	 * @return 잠긴 계정 목록
	 */
	public List<LockedAccountRes> getLockedAccounts() {
		List<LockedAccountRes> lockedAccountRes = new ArrayList<>();

		// Redis SCAN 명령어로 잠긴 계정 키 조회
		ScanOptions scanOptions = ScanOptions.scanOptions()
			.match(LOCK_KEY_PREFIX + "*")
			.count(100) // 100개씩
			.build();

		// 커서 사용하여 키 스캔
		try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
			// 스캔된 각 키에 대해 처리
			while (cursor.hasNext()) {
				// 잠긴 계정 키
				String key = cursor.next();
				// 이메일 추출
				String email = key.substring(LOCK_KEY_PREFIX.length());
				// 남은 잠금 시간 조회
				Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

				// 잠긴 계정 정보 추가
				if (ttl != null && ttl > 0) {
					lockedAccountRes.add(LockedAccountRes.of(email, ttl));
				}
			}
		}

		return lockedAccountRes;
	}

	/**
	 * 관리자용 계정 잠금 해제
	 * @param adminId
	 * @param email
	 */
	public void unlockAccount(Long adminId, String email) {
		String lockKey = LOCK_KEY_PREFIX + email;
		String attemptKey = ATTEMPT_KEY_PREFIX + email;

		// 잠금 상태 확인
		if (Boolean.FALSE.equals(redisTemplate.hasKey(lockKey))) {
			throw new BusinessException(AuthErrorCode.ACCOUNT_NOT_LOCKED);
		}

		// 잠금 키 삭제
		redisTemplate.delete(lockKey);
		// 시도 횟수 키 삭제
		redisTemplate.delete(attemptKey);

		log.info("[Admin] 계정 잠금 해제: adminId={}, email={}", adminId, email);
	}

	/**
	 * 잠긴 계정 수 조회
	 */
	public int getLockedAccountCount() {
		return getLockedAccounts().size();
	}
}
