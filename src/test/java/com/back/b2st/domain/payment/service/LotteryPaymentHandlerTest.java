package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.lottery.entry.entity.LotteryEntry;
import com.back.b2st.domain.lottery.entry.repository.LotteryEntryRepository;
import com.back.b2st.domain.lottery.result.entity.LotteryResult;
import com.back.b2st.domain.lottery.result.repository.LotteryResultRepository;
import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.seat.grade.entity.SeatGrade;
import com.back.b2st.domain.seat.grade.entity.SeatGradeType;
import com.back.b2st.domain.seat.grade.repository.SeatGradeRepository;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class LotteryPaymentHandlerTest {

	@Mock
	private LotteryResultRepository lotteryResultRepository;

	@Mock
	private LotteryEntryRepository lotteryEntryRepository;

	@Mock
	private SeatGradeRepository seatGradeRepository;

	@Mock
	private Clock clock;

	@InjectMocks
	private LotteryPaymentHandler lotteryPaymentHandler;

	private static final Long LOTTERY_RESULT_ID = 10L;
	private static final Long LOTTERY_ENTRY_ID = 20L;
	private static final Long MEMBER_ID = 1L;
	private static final Long PERFORMANCE_ID = 30L;

	@Test
	@DisplayName("supports(): DomainType.LOTTERY 지원")
	void supports_lottery_true() {
		assertThat(lotteryPaymentHandler.supports(DomainType.LOTTERY)).isTrue();
	}

	@Test
	@DisplayName("supports(): 다른 도메인 타입은 미지원")
	void supports_others_false() {
		assertThat(lotteryPaymentHandler.supports(DomainType.RESERVATION)).isFalse();
		assertThat(lotteryPaymentHandler.supports(DomainType.PRERESERVATION)).isFalse();
		assertThat(lotteryPaymentHandler.supports(DomainType.TRADE)).isFalse();
	}

	@Test
	@DisplayName("loadAndValidate(): 정상 케이스 - PaymentTarget 반환")
	void loadAndValidate_success() {
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 12, 0);
		givenFixedClock(now);

		LotteryResult lotteryResult = org.mockito.Mockito.mock(LotteryResult.class);
		given(lotteryResultRepository.findById(LOTTERY_RESULT_ID)).willReturn(Optional.of(lotteryResult));
		given(lotteryResult.getMemberId()).willReturn(MEMBER_ID);
		given(lotteryResult.isPaid()).willReturn(false);
		given(lotteryResult.getPaymentDeadline()).willReturn(now.plusMinutes(10));
		given(lotteryResult.getLotteryEntryId()).willReturn(LOTTERY_ENTRY_ID);

		LotteryEntry lotteryEntry = org.mockito.Mockito.mock(LotteryEntry.class);
		given(lotteryEntryRepository.findById(LOTTERY_ENTRY_ID)).willReturn(Optional.of(lotteryEntry));
		given(lotteryEntry.getPerformanceId()).willReturn(PERFORMANCE_ID);
		given(lotteryEntry.getGrade()).willReturn(SeatGradeType.VIP);
		given(lotteryEntry.getQuantity()).willReturn(2);

		SeatGrade seatGrade = org.mockito.Mockito.mock(SeatGrade.class);
		given(seatGradeRepository.findTopByPerformanceIdAndGradeOrderByIdDesc(PERFORMANCE_ID, SeatGradeType.VIP))
			.willReturn(Optional.of(seatGrade));
		given(seatGrade.getPrice()).willReturn(50000);

		PaymentTarget target = lotteryPaymentHandler.loadAndValidate(LOTTERY_RESULT_ID, MEMBER_ID);

		assertThat(target.domainType()).isEqualTo(DomainType.LOTTERY);
		assertThat(target.domainId()).isEqualTo(LOTTERY_RESULT_ID);
		assertThat(target.expectedAmount()).isEqualTo(100000L);
	}

	@Test
	@DisplayName("loadAndValidate(): 추첨 결과가 없으면 DOMAIN_NOT_FOUND 예외")
	void loadAndValidate_resultNotFound_throw() {
		given(lotteryResultRepository.findById(LOTTERY_RESULT_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> lotteryPaymentHandler.loadAndValidate(LOTTERY_RESULT_ID, MEMBER_ID))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_FOUND);
	}

	@Test
	@DisplayName("loadAndValidate(): 다른 사용자의 추첨 결과면 UNAUTHORIZED_PAYMENT_ACCESS 예외")
	void loadAndValidate_unauthorized_throw() {
		LotteryResult lotteryResult = org.mockito.Mockito.mock(LotteryResult.class);
		given(lotteryResultRepository.findById(LOTTERY_RESULT_ID)).willReturn(Optional.of(lotteryResult));
		given(lotteryResult.getMemberId()).willReturn(999L);

		assertThatThrownBy(() -> lotteryPaymentHandler.loadAndValidate(LOTTERY_RESULT_ID, MEMBER_ID))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
	}

	@Test
	@DisplayName("loadAndValidate(): 이미 결제된 경우 DOMAIN_NOT_PAYABLE 예외")
	void loadAndValidate_alreadyPaid_throw() {
		LotteryResult lotteryResult = org.mockito.Mockito.mock(LotteryResult.class);
		given(lotteryResultRepository.findById(LOTTERY_RESULT_ID)).willReturn(Optional.of(lotteryResult));
		given(lotteryResult.getMemberId()).willReturn(MEMBER_ID);
		given(lotteryResult.isPaid()).willReturn(true);

		assertThatThrownBy(() -> lotteryPaymentHandler.loadAndValidate(LOTTERY_RESULT_ID, MEMBER_ID))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_PAYABLE);
	}

	@Test
	@DisplayName("loadAndValidate(): 결제 기한이 지난 경우 DOMAIN_NOT_PAYABLE 예외")
	void loadAndValidate_expired_throw() {
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 12, 0);
		givenFixedClock(now);

		LotteryResult lotteryResult = org.mockito.Mockito.mock(LotteryResult.class);
		given(lotteryResultRepository.findById(LOTTERY_RESULT_ID)).willReturn(Optional.of(lotteryResult));
		given(lotteryResult.getMemberId()).willReturn(MEMBER_ID);
		given(lotteryResult.isPaid()).willReturn(false);
		given(lotteryResult.getPaymentDeadline()).willReturn(now.minusMinutes(1));

		assertThatThrownBy(() -> lotteryPaymentHandler.loadAndValidate(LOTTERY_RESULT_ID, MEMBER_ID))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_PAYABLE);
	}

	@Test
	@DisplayName("loadAndValidate(): 응모 엔티티가 없으면 DOMAIN_NOT_FOUND 예외")
	void loadAndValidate_entryNotFound_throw() {
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 12, 0);
		givenFixedClock(now);

		LotteryResult lotteryResult = org.mockito.Mockito.mock(LotteryResult.class);
		given(lotteryResultRepository.findById(LOTTERY_RESULT_ID)).willReturn(Optional.of(lotteryResult));
		given(lotteryResult.getMemberId()).willReturn(MEMBER_ID);
		given(lotteryResult.isPaid()).willReturn(false);
		given(lotteryResult.getPaymentDeadline()).willReturn(now.plusMinutes(10));
		given(lotteryResult.getLotteryEntryId()).willReturn(LOTTERY_ENTRY_ID);

		given(lotteryEntryRepository.findById(LOTTERY_ENTRY_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> lotteryPaymentHandler.loadAndValidate(LOTTERY_RESULT_ID, MEMBER_ID))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_FOUND);
	}

	@Test
	@DisplayName("loadAndValidate(): 좌석 등급 가격을 찾지 못하면 DOMAIN_NOT_FOUND 예외")
	void loadAndValidate_seatGradeNotFound_throw() {
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 12, 0);
		givenFixedClock(now);

		LotteryResult lotteryResult = org.mockito.Mockito.mock(LotteryResult.class);
		given(lotteryResultRepository.findById(LOTTERY_RESULT_ID)).willReturn(Optional.of(lotteryResult));
		given(lotteryResult.getMemberId()).willReturn(MEMBER_ID);
		given(lotteryResult.isPaid()).willReturn(false);
		given(lotteryResult.getPaymentDeadline()).willReturn(now.plusMinutes(10));
		given(lotteryResult.getLotteryEntryId()).willReturn(LOTTERY_ENTRY_ID);

		LotteryEntry lotteryEntry = org.mockito.Mockito.mock(LotteryEntry.class);
		given(lotteryEntryRepository.findById(LOTTERY_ENTRY_ID)).willReturn(Optional.of(lotteryEntry));
		given(lotteryEntry.getPerformanceId()).willReturn(PERFORMANCE_ID);
		given(lotteryEntry.getGrade()).willReturn(SeatGradeType.VIP);

		given(seatGradeRepository.findTopByPerformanceIdAndGradeOrderByIdDesc(PERFORMANCE_ID, SeatGradeType.VIP))
			.willReturn(Optional.empty());

		assertThatThrownBy(() -> lotteryPaymentHandler.loadAndValidate(LOTTERY_RESULT_ID, MEMBER_ID))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_FOUND);
	}

	private void givenFixedClock(LocalDateTime now) {
		Clock fixedClock = Clock.fixed(
			now.atZone(ZoneId.of("UTC")).toInstant(),
			ZoneId.of("UTC")
		);
		given(clock.instant()).willReturn(fixedClock.instant());
		given(clock.getZone()).willReturn(fixedClock.getZone());
	}
}
