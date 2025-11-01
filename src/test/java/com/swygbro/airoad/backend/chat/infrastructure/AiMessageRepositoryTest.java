package com.swygbro.airoad.backend.chat.infrastructure;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.chat.domain.entity.AiConversation;
import com.swygbro.airoad.backend.chat.domain.entity.AiMessage;
import com.swygbro.airoad.backend.chat.infrastructure.repository.AiConversationRepository;
import com.swygbro.airoad.backend.chat.infrastructure.repository.AiMessageRepository;
import com.swygbro.airoad.backend.fixture.chat.AiConversationFixture;
import com.swygbro.airoad.backend.fixture.member.MemberFixture;
import com.swygbro.airoad.backend.fixture.trip.TripPlanFixture;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.infrastructure.MemberRepository;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;
import com.swygbro.airoad.backend.trip.infrastructure.TripPlanRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AiMessageRepository 테스트
 *
 * <p>Repository 계층 테스트로 실제 데이터베이스와의 상호작용을 검증합니다.
 */
@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class AiMessageRepositoryTest {

  @Autowired private AiMessageRepository aiMessageRepository;

  @Autowired private AiConversationRepository aiConversationRepository;

  @Autowired private MemberRepository memberRepository;

  @Autowired private TripPlanRepository tripPlanRepository;

  private AiConversation testConversation;
  private AiConversation anotherConversation;

  @BeforeEach
  void setUp() {
    // given: Fixture를 사용하여 테스트 데이터 생성 및 저장
    Member testMember = memberRepository.save(MemberFixture.create());
    TripPlan testTripPlan = tripPlanRepository.save(TripPlanFixture.createWithMember(testMember));

    // given: 테스트용 대화 세션 생성 및 저장
    testConversation =
        aiConversationRepository.save(
            AiConversationFixture.createWithMemberAndTripPlan(testMember, testTripPlan));
    anotherConversation =
        aiConversationRepository.save(
            AiConversationFixture.createWithMemberAndTripPlan(testMember, testTripPlan));
  }

  @AfterEach
  void tearDown() {
    aiMessageRepository.deleteAllInBatch();
    aiConversationRepository.deleteAllInBatch();
    tripPlanRepository.deleteAllInBatch();
    memberRepository.deleteAllInBatch();
  }

  @Nested
  @DisplayName("findMessageHistoryByCursor 메서드는")
  class FindMessageHistoryByCursor {

    @Test
    @DisplayName("cursor가 null일 때 최신 메시지부터 조회한다")
    void shouldGetLatestMessagesWhenCursorIsNull() {
      // given: 5개의 메시지 생성 (ID 순서: 1, 2, 3, 4, 5)
      createAndSaveMessages(testConversation, 5);
      Pageable pageable = PageRequest.of(0, 3);

      // when: cursor 없이 조회 실행
      Slice<AiMessage> result =
          aiMessageRepository.findMessageHistoryByCursor(testConversation.getId(), null, pageable);

      // then: 최신 메시지부터 3개 조회 (ID 역순: 5, 4, 3)
      assertThat(result.getContent()).hasSize(3);
      assertThat(result.getContent())
          .extracting(AiMessage::getContent)
          .containsExactly("메시지 내용 5", "메시지 내용 4", "메시지 내용 3");
      assertThat(result.hasNext()).isTrue(); // 더 조회할 메시지 존재
    }

    @Test
    @DisplayName("cursor가 있을 때 해당 ID보다 작은 메시지만 조회한다")
    void shouldGetMessagesSmallerThanCursor() {
      // given: 10개의 메시지 생성
      List<AiMessage> messages = createAndSaveMessages(testConversation, 10);
      Long cursor = messages.get(7).getId(); // 8번째 메시지 (ID가 더 큰 메시지)
      Pageable pageable = PageRequest.of(0, 3);

      // when: cursor 기준으로 조회 실행
      Slice<AiMessage> result =
          aiMessageRepository.findMessageHistoryByCursor(
              testConversation.getId(), cursor, pageable);

      // then: cursor보다 작은 ID만 조회됨
      assertThat(result.getContent()).hasSize(3);
      assertThat(result.getContent()).allMatch(message -> message.getId() < cursor);
      assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("특정 conversationId의 메시지만 조회한다")
    void shouldGetMessagesOnlyForSpecificConversation() {
      // given: 두 개의 대화에 각각 메시지 생성
      createAndSaveMessages(testConversation, 5);
      createAndSaveMessages(anotherConversation, 3);
      Pageable pageable = PageRequest.of(0, 10);

      // when: 첫 번째 대화의 메시지만 조회 실행
      Slice<AiMessage> result =
          aiMessageRepository.findMessageHistoryByCursor(testConversation.getId(), null, pageable);

      // then: 첫 번째 대화의 메시지만 조회됨 (5개)
      assertThat(result.getContent()).hasSize(5);
      assertThat(result.getContent())
          .allMatch(message -> message.getConversation().getId().equals(testConversation.getId()));
      assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("ID 역순으로 정렬된 메시지를 반환한다")
    void shouldReturnMessagesOrderedByIdDescending() {
      // given: 5개의 메시지 생성
      createAndSaveMessages(testConversation, 5);
      Pageable pageable = PageRequest.of(0, 10);

      // when: 전체 메시지 조회 실행
      Slice<AiMessage> result =
          aiMessageRepository.findMessageHistoryByCursor(testConversation.getId(), null, pageable);

      // then: ID 역순으로 정렬됨 (5 → 4 → 3 → 2 → 1)
      List<Long> messageIds = result.getContent().stream().map(AiMessage::getId).toList();
      assertThat(messageIds).isSortedAccordingTo((id1, id2) -> Long.compare(id2, id1));
    }

    @Test
    @DisplayName("Slice 타입으로 반환하며 hasNext를 정확히 계산한다")
    void shouldReturnSliceWithCorrectHasNext() {
      // given: 10개의 메시지 생성
      createAndSaveMessages(testConversation, 10);
      Pageable firstPageRequest = PageRequest.of(0, 3);

      // when: 첫 페이지 조회 실행 (더 조회할 메시지 있음)
      Slice<AiMessage> firstPage =
          aiMessageRepository.findMessageHistoryByCursor(
              testConversation.getId(), null, firstPageRequest);

      // then: hasNext가 true (10개 중 3개 조회, 7개 남음)
      assertThat(firstPage.hasNext()).isTrue();
      assertThat(firstPage.getContent()).hasSize(3);

      // when: 마지막 페이지 조회 실행 (7개 남았는데 10개 요청)
      Long cursor = firstPage.getContent().get(2).getId();
      Pageable largePageRequest = PageRequest.of(0, 10);
      Slice<AiMessage> lastPage =
          aiMessageRepository.findMessageHistoryByCursor(
              testConversation.getId(), cursor, largePageRequest);

      // then: 남은 메시지(7개)가 요청 크기(10개)보다 적으므로 hasNext는 false
      assertThat(lastPage.getContent()).hasSize(7);
      assertThat(lastPage.hasNext()).isFalse();
    }

    @Test
    @DisplayName("메시지가 없는 경우 빈 Slice를 반환한다")
    void shouldReturnEmptySliceWhenNoMessages() {
      // given: 메시지가 없는 대화 세션 (Fixture 사용)
      Member newMember = memberRepository.save(MemberFixture.createWithEmail("new@example.com"));
      TripPlan newTripPlan = tripPlanRepository.save(TripPlanFixture.createWithMember(newMember));
      AiConversation emptyConversation =
          aiConversationRepository.save(
              AiConversationFixture.createWithMemberAndTripPlan(newMember, newTripPlan));
      Pageable pageable = PageRequest.of(0, 10);

      // when: 메시지 조회 실행
      Slice<AiMessage> result =
          aiMessageRepository.findMessageHistoryByCursor(emptyConversation.getId(), null, pageable);

      // then: 빈 Slice 반환
      assertThat(result.getContent()).isEmpty();
      assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("cursor가 가장 작은 메시지 ID보다 작을 때 빈 Slice를 반환한다")
    void shouldReturnEmptySliceWhenCursorSmallerThanAllMessages() {
      // given: 메시지 생성 (ID는 자동 증가)
      List<AiMessage> messages = createAndSaveMessages(testConversation, 5);
      Long smallestId = messages.get(0).getId();
      Long cursorSmallerThanAll = smallestId - 1;
      Pageable pageable = PageRequest.of(0, 10);

      // when: 가장 작은 ID보다 작은 cursor로 조회 실행
      Slice<AiMessage> result =
          aiMessageRepository.findMessageHistoryByCursor(
              testConversation.getId(), cursorSmallerThanAll, pageable);

      // then: 빈 Slice 반환 (cursor보다 작은 ID가 없음)
      assertThat(result.getContent()).isEmpty();
      assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("다양한 MessageType의 메시지를 모두 조회한다")
    void shouldGetMessagesWithDifferentMessageTypes() {
      // given: USER와 ASSISTANT 메시지 혼합 생성
      createAndSaveMessage(testConversation, "사용자 메시지 1", MessageType.USER);
      createAndSaveMessage(testConversation, "AI 응답 1", MessageType.ASSISTANT);
      createAndSaveMessage(testConversation, "사용자 메시지 2", MessageType.USER);
      createAndSaveMessage(testConversation, "AI 응답 2", MessageType.ASSISTANT);
      Pageable pageable = PageRequest.of(0, 10);

      // when: 전체 메시지 조회 실행
      Slice<AiMessage> result =
          aiMessageRepository.findMessageHistoryByCursor(testConversation.getId(), null, pageable);

      // then: 모든 타입의 메시지가 조회됨
      assertThat(result.getContent()).hasSize(4);
      assertThat(result.getContent())
          .extracting(AiMessage::getMessageType)
          .containsExactly(
              MessageType.ASSISTANT, MessageType.USER, MessageType.ASSISTANT, MessageType.USER);
    }

    @Test
    @DisplayName("페이지 크기가 1일 때 정확히 1개만 조회한다")
    void shouldGetExactlyOneMessageWhenPageSizeIsOne() {
      // given: 5개의 메시지 생성
      createAndSaveMessages(testConversation, 5);
      Pageable pageable = PageRequest.of(0, 1);

      // when: 크기 1로 조회 실행
      Slice<AiMessage> result =
          aiMessageRepository.findMessageHistoryByCursor(testConversation.getId(), null, pageable);

      // then: 정확히 1개만 조회됨
      assertThat(result.getContent()).hasSize(1);
      assertThat(result.hasNext()).isTrue();
    }
  }

  // ======================== Private Helper Methods ========================

  /**
   * 테스트용 메시지 생성 및 저장
   *
   * @param conversation 메시지가 속할 대화 세션
   * @param content 메시지 내용
   * @param messageType 메시지 타입
   * @return 저장된 AiMessage 객체
   */
  private AiMessage createAndSaveMessage(
      AiConversation conversation, String content, MessageType messageType) {
    AiMessage message =
        AiMessage.builder()
            .conversation(conversation)
            .content(content)
            .messageType(messageType)
            .build();
    return aiMessageRepository.save(message);
  }

  /**
   * 여러 개의 테스트용 메시지 생성 및 저장
   *
   * @param conversation 메시지가 속할 대화 세션
   * @param count 생성할 메시지 개수
   * @return 저장된 AiMessage 리스트
   */
  private List<AiMessage> createAndSaveMessages(AiConversation conversation, int count) {
    return IntStream.rangeClosed(1, count)
        .mapToObj(
            i ->
                createAndSaveMessage(
                    conversation,
                    "메시지 내용 " + i,
                    i % 2 == 1 ? MessageType.USER : MessageType.ASSISTANT))
        .toList();
  }
}
