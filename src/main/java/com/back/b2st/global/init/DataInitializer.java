package com.back.b2st.global.init;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	public void run(String... args) throws Exception {
		// 서버 재시작시 중복 생성 방지 차
		if (memberRepository.count() > 0) {
			log.info("[DataInit] 이미 계정 존재하여 초기화 스킵");
			return;
		}

		log.info("[DataInit] 테스트 계정 데이터 생성");

		Member admin = Member.builder()
			.email("admin@tt.com")
			.password(passwordEncoder.encode("1234")) // 어드민, 유저 비번 전부 1234입니다
			.name("관리자")
			.role(Member.Role.ADMIN)
			.provider(Member.Provider.EMAIL)
			.isVerified(true)
			.build();

		Member user1 = Member.builder()
			.email("user1@tt.com")
			.password(passwordEncoder.encode("1234"))
			.name("유저1")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isVerified(true)
			.build();

		Member user2 = Member.builder()
			.email("user2@tt.com")
			.password(passwordEncoder.encode("1234"))
			.name("유저2")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isVerified(true)
			.build();

		memberRepository.save(admin);
		memberRepository.save(user1);
		memberRepository.save(user2);

		log.info("[DataInit] 계정 생성 완");
		log.info("   - 관리자: admin@tt.com / 1234");
		log.info("   - 유저1 : user1@tt.com / 1234");
		log.info("   - 유저2 : user2@tt.com / 1234");
	}
}
