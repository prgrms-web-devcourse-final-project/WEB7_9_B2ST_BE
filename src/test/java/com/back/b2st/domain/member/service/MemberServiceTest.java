package com.back.b2st.domain.member.service;

import com.back.b2st.domain.member.dto.SignupRequest;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class) // Mockito 환경 사용
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService; // 가짜 객체들이 주입될 타겟 서비스

    @Mock
    private MemberRepository memberRepository; // 가짜 리포지토리

    @Mock
    private PasswordEncoder passwordEncoder; // 가짜 암호화기

    @Test
    @DisplayName("회원가입 성공 테스트")
    void signup_success() {
        // given (준비)
        SignupRequest request = createSignupRequest("test@email.com", "validPw123!", "tester", "nickname");

        // Mocking: 중복된 이메일/닉네임이 없다고 가정
        given(memberRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(memberRepository.existsByNickname(request.getNickname())).willReturn(false);
        given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPassword");

        // Mocking: save 호출 시 ID가 1인 Member 반환하도록 설정
        Member savedMember = Member.builder()
                .email(request.getEmail())
                .role(Member.Role.MEMBER)
                .build();
        ReflectionTestUtils.setField(savedMember, "id", 1L); // ID 강제 주입
        given(memberRepository.save(any(Member.class))).willReturn(savedMember);

        // when (실행)
        Long memberId = memberService.signup(request);

        // then (검증)
        assertThat(memberId).isEqualTo(1L);
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_fail_duplicate_email() {
        // given
        SignupRequest request = createSignupRequest("duplicate@email.com", "pw", "name", "nick");

        // Mocking: 이메일이 이미 존재한다고 가정
        given(memberRepository.existsByEmail(request.getEmail())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 가입된 이메일입니다.");
    }

    // 테스트용 DTO 생성 헬퍼 메서드
    private SignupRequest createSignupRequest(String email, String pw, String name, String nick) {
        SignupRequest request = new SignupRequest();
        // Reflection을 사용하여 private 필드에 값 주입 (Setter가 없는 경우 유용)
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", pw);
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "nickname", nick);
        ReflectionTestUtils.setField(request, "birth", LocalDate.of(2000, 1, 1));
        return request;
    }
}
