package com.back.b2st.domain.queue.service;

import com.back.b2st.domain.queue.dto.response.QueuePositionRes;
import com.back.b2st.domain.queue.dto.response.StartBookingRes;
import com.back.b2st.domain.queue.entity.Queue;
import com.back.b2st.domain.queue.error.QueueErrorCode;
import com.back.b2st.domain.queue.repository.QueueRepository;
import com.back.b2st.global.error.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * QueueService 통합 테스트
 *
 * 대기열 전체 플로우 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("QueueService 통합 테스트")
class QueueServiceIntegrationTest {

	@Autowired
	private QueueService queueService;

	@Autowired
	private QueueRepository queueRepository;

	private Queue testQueue;
	private Long queueId;
	private Long userId;
	private Long scheduleId;

	@BeforeEach
	void setup() {
		// 테스트용 대기열 생성
		testQueue = Queue.builder()
			.performanceId(1L)
			.maxActiveUsers(100)
			.entryTtlMinutes(10)
			.build();
		testQueue = queueRepository.save(testQueue);

		queueId = testQueue.getId();
		userId = 100L;
		scheduleId = 1L;
	}

	@Test
	@DisplayName("예매 시작 - 정상 플로우")
	void testStartBookingSuccess() {
		// When
		StartBookingRes response = queueService.startBooking(scheduleId, userId);

		// Then
		assertThat(response).isNotNull();
		assertThat(response.queueId()).isEqualTo(queueId);
		assertThat(response.performanceId()).isEqualTo(1L);
		assertThat(response.scheduleId()).isEqualTo(scheduleId);
		assertThat(response.entry()).isNotNull();
		assertThat(response.entry().userId()).isEqualTo(userId);
		assertThat(response.entry().status()).isEqualTo("WAITING");
	}

	@Test
	@DisplayName("예매 시작 - 멱등성 (중복 호출 시 현재 상태 반환)")
	void testStartBookingIdempotent() {
		// Given: 첫 번째 호출
		StartBookingRes firstResponse = queueService.startBooking(scheduleId, userId);

		// When: 같은 사용자가 다시 호출
		StartBookingRes secondResponse = queueService.startBooking(scheduleId, userId);

		// Then: 예외 발생하지 않고 현재 상태 반환
		assertThat(secondResponse).isNotNull();
		assertThat(secondResponse.queueId()).isEqualTo(firstResponse.queueId());
		assertThat(secondResponse.entry().status()).isIn("WAITING", "ENTERABLE");
	}

	@Test
	@DisplayName("대기 순번 조회 - WAITING 상태")
	void testGetMyPositionWaiting() {
		// Given
		queueService.startBooking(scheduleId, userId);

		// When
		QueuePositionRes position = queueService.getMyPosition(queueId, userId);

		// Then
		assertThat(position).isNotNull();
		assertThat(position.queueId()).isEqualTo(queueId);
		assertThat(position.userId()).isEqualTo(userId);
		assertThat(position.status()).isEqualTo("WAITING");
		assertThat(position.myRank()).isGreaterThan(0);
	}

	@Test
	@DisplayName("대기 순번 조회 - 대기열에 없는 경우")
	void testGetMyPositionNotInQueue() {
		// When
		QueuePositionRes position = queueService.getMyPosition(queueId, 999L);

		// Then
		assertThat(position).isNotNull();
		assertThat(position.status()).isEqualTo("NOT_IN_QUEUE");
	}

	@Test
	@DisplayName("입장 가능으로 이동")
	void testMoveToEnterable() {
		// Given
		queueService.startBooking(scheduleId, userId);

		// When
		queueService.moveToEnterable(queueId, userId);

		// Then
		QueuePositionRes position = queueService.getMyPosition(queueId, userId);
		assertThat(position.status()).isEqualTo("ENTERABLE");
	}

	@Test
	@DisplayName("예매 완료 처리")
	void testCompleteEntry() {
		// Given
		queueService.startBooking(scheduleId, userId);
		queueService.moveToEnterable(queueId, userId);

		// When
		queueService.completeEntry(queueId, userId);

		// Then
		QueuePositionRes position = queueService.getMyPosition(queueId, userId);
		assertThat(position.status()).isEqualTo("COMPLETED");
	}

	@Test
	@DisplayName("예매 완료 실패 - 입장 가능 상태 아님")
	void testCompleteEntryFailureNotEnterable() {
		// Given: WAITING 상태
		queueService.startBooking(scheduleId, userId);

		// When & Then
		assertThatThrownBy(() -> queueService.completeEntry(queueId, userId))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> {
				BusinessException bex = (BusinessException) ex;
				assertThat(bex.getErrorCode()).isEqualTo(QueueErrorCode.QUEUE_ENTRY_EXPIRED);
			});
	}

	@Test
	@DisplayName("대기열 나가기 - WAITING 상태에서")
	void testExitQueueFromWaiting() {
		// Given
		queueService.startBooking(scheduleId, userId);

		// When
		queueService.exitQueue(queueId, userId);

		// Then
		QueuePositionRes position = queueService.getMyPosition(queueId, userId);
		assertThat(position.status()).isEqualTo("NOT_IN_QUEUE");
	}

	@Test
	@DisplayName("대기열 나가기 - ENTERABLE 상태에서")
	void testExitQueueFromEnterable() {
		// Given
		queueService.startBooking(scheduleId, userId);
		queueService.moveToEnterable(queueId, userId);

		// When
		queueService.exitQueue(queueId, userId);

		// Then
		QueuePositionRes position = queueService.getMyPosition(queueId, userId);
		assertThat(position.status()).isIn("EXPIRED", "NOT_IN_QUEUE");
	}

	@Test
	@DisplayName("존재하지 않는 대기열 조회 - 예외 발생")
	void testQueueNotFound() {
		// Given
		Long invalidQueueId = 999L;

		// When & Then
		assertThatThrownBy(() -> queueService.getMyPosition(invalidQueueId, userId))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> {
				BusinessException bex = (BusinessException) ex;
				assertThat(bex.getErrorCode()).isEqualTo(QueueErrorCode.QUEUE_NOT_FOUND);
			});
	}

	@Test
	@DisplayName("여러 사용자 동시 입장")
	void testMultipleUsersEnterQueue() {
		// Given
		Long user1 = 100L;
		Long user2 = 200L;
		Long user3 = 300L;

		// When
		queueService.startBooking(scheduleId, user1);
		queueService.startBooking(scheduleId, user2);
		queueService.startBooking(scheduleId, user3);

		// Then
		QueuePositionRes pos1 = queueService.getMyPosition(queueId, user1);
		QueuePositionRes pos2 = queueService.getMyPosition(queueId, user2);
		QueuePositionRes pos3 = queueService.getMyPosition(queueId, user3);

		assertThat(pos1.myRank()).isLessThan(pos2.myRank());
		assertThat(pos2.myRank()).isLessThan(pos3.myRank());
	}

	@Test
	@DisplayName("전체 플로우 - 입장 → 대기 → 입장가능 → 완료")
	void testCompleteFlow() {
		// 1. 예매 시작 (입장)
		StartBookingRes startResponse = queueService.startBooking(scheduleId, userId);
		assertThat(startResponse.entry().status()).isEqualTo("WAITING");

		// 2. 순번 확인
		QueuePositionRes waitingPosition = queueService.getMyPosition(queueId, userId);
		assertThat(waitingPosition.status()).isEqualTo("WAITING");
		assertThat(waitingPosition.myRank()).isGreaterThan(0);

		// 3. 입장 가능으로 이동
		queueService.moveToEnterable(queueId, userId);
		QueuePositionRes enterablePosition = queueService.getMyPosition(queueId, userId);
		assertThat(enterablePosition.status()).isEqualTo("ENTERABLE");

		// 4. 예매 완료
		queueService.completeEntry(queueId, userId);
		QueuePositionRes completedPosition = queueService.getMyPosition(queueId, userId);
		assertThat(completedPosition.status()).isEqualTo("COMPLETED");
	}
}