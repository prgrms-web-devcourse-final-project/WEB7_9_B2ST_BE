package com.back.b2st.domain.member.listener;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.member.dto.event.SignupEvent;
import com.back.b2st.domain.member.entity.SignupLog;
import com.back.b2st.domain.member.repository.SignupLogRepository;

@ExtendWith(MockitoExtension.class)
class SignupEventListenerTest {

	private static final String TEST_EMAIL = "test@email.com";
	private static final String TEST_IP = "127.0.0.1";

	@Mock
	private SignupLogRepository signupLogRepository;

	@InjectMocks
	private SignupEventListener signupEventListener;

	@Test
	@DisplayName("가입 이벤트 처리 시 로그가 저장된다")
	void handleSignupEvent_savesLog() {
		// given
		SignupEvent event = SignupEvent.of(TEST_EMAIL, TEST_IP);

		// when
		signupEventListener.handleSignupEvent(event);

		// then
		ArgumentCaptor<SignupLog> captor = ArgumentCaptor.forClass(SignupLog.class);
		verify(signupLogRepository).save(captor.capture());

		SignupLog savedLog = captor.getValue();
		assertEquals(TEST_EMAIL, savedLog.getEmail());
		assertEquals(TEST_IP, savedLog.getClientIp());
		assertNotNull(savedLog.getCreatedAt());
	}

	@Test
	@DisplayName("로그 저장 실패 시에도 예외가 전파되지 않는다")
	void handleSignupEvent_exceptionSwallowed() {
		// given
		SignupEvent event = SignupEvent.of(TEST_EMAIL, TEST_IP);
		when(signupLogRepository.save(any())).thenThrow(new RuntimeException("DB Error"));

		// when & then - 예외 없이 정상 종료
		assertDoesNotThrow(() -> signupEventListener.handleSignupEvent(event));
	}

	@Test
	@DisplayName("이벤트의 occurredAt이 로그에 그대로 저장된다")
	void handleSignupEvent_preservesOccurredAt() {
		// given
		SignupEvent event = SignupEvent.of(TEST_EMAIL, TEST_IP);

		// when
		signupEventListener.handleSignupEvent(event);

		// then
		ArgumentCaptor<SignupLog> captor = ArgumentCaptor.forClass(SignupLog.class);
		verify(signupLogRepository).save(captor.capture());

		SignupLog savedLog = captor.getValue();
		assertEquals(event.occurredAt(), savedLog.getCreatedAt());
	}
}
