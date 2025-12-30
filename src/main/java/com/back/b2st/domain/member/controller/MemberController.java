package com.back.b2st.domain.member.controller;

import java.util.List;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.member.dto.request.SignupReq;
import com.back.b2st.domain.member.service.MemberService;
import com.back.b2st.global.common.BaseResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

	private static final List<String> IP_HEADERS = List.of("X-Forwarded-For", // 프록시나 로드밸런서 뒤에 있을 때 원래 클라이언트 IP
		"X-Real-IP", // Nginx 등에서 설정하는 실제 클라이언트 IP
		"Proxy-Client-IP", // Apache 프록시
		"WL-Proxy-Client-IP" // WebLogic 프록시
	);

	private final MemberService memberService;

	/**
	 * 회원가입 처리 - Bean Validation(정규표현식) + BCrypt 암호화 + 이메일 중복 검사 + 기본 Role 설정
	 *
	 * @param request     회원가입 요청 정보
	 * @param httpRequest HTTP 요청(IP 추출용)
	 * @return 생성된 회원 ID
	 */
	@PostMapping("/signup")
	public BaseResponse<Long> signup(@Valid @RequestBody SignupReq request, HttpServletRequest httpRequest) {
		String clientIp = getClientIp(httpRequest);
		Long memberId = memberService.signup(request, clientIp);
		return BaseResponse.created(memberId);
	}

	/**
	 * 클라이언트 IP 추출
	 */
	private String getClientIp(HttpServletRequest httpRequest) {
		return IP_HEADERS.stream()
			.map(httpRequest::getHeader)
			.filter(StringUtils::hasText)
			.map(ip -> ip.split(",")[0].trim()) // 여러 IP가 있을 경우 첫 번째 IP 사용
			.findFirst()
			.orElseGet(httpRequest::getRemoteAddr);
	}
}
