package com.swygbro.airoad.backend.chat.application;

/**
 * AI 메시지 처리 통합 테스트 (추후 구현 예정)
 *
 * <p>Service → EventPublisher → EventListener → WebSocket 전체 흐름을 검증합니다.
 *
 * <p>TODO: @Nested 클래스의 @Transactional 이슈 해결 후 활성화 예정
 */
/*

@SpringBootTest
@ActiveProfiles("test")
@RecordApplicationEvents
class AiMessageIntegrationTest {

  @TestConfiguration
  static class TestConfig {
    @Bean
    @Primary
    public SimpMessagingTemplate simpMessagingTemplate() {
      return mock(SimpMessagingTemplate.class);
    }
  }

  @Autowired private AiMessageService aiMessageService;

  @Autowired private AiConversationRepository aiConversationRepository;

  @Autowired private AiMessageRepository aiMessageRepository;

  @Autowired private SimpMessagingTemplate messagingTemplate;

  @Autowired private EntityManager entityManager;

  @Autowired private ApplicationEvents applicationEvents;

  @BeforeEach
  void setUp() {
    reset(messagingTemplate);
  }

  @AfterEach
  void tearDown() {
    reset(messagingTemplate);
  }

  @Nested
  @DisplayName("메시지 처리 및 이벤트 발행 통합 테스트")
  class MessageProcessingAndEventPublishing {

    @Test
    @Transactional
    @DisplayName("사용자 메시지 처리 시 USER 메시지가 DB에 저장된다")
    void shouldSaveUserMessageToDatabase() {
      // given
      AiConversation conversation = createAndSaveConversation();
      String userId = conversation.getMember().getEmail();
      String messageContent = "서울 3박 4일 여행 계획을 짜주세요";
      ChatMessageRequest request = new ChatMessageRequest(messageContent, MessageContentType.TEXT);

      // when
      aiMessageService.processAndSendMessage(conversation.getId(), userId, request);
      entityManager.flush();
      entityManager.clear();

      // then
      AiMessage savedMessage =
          aiMessageRepository.findById(1L).orElseThrow(() -> new AssertionError("메시지가 저장되지 않았습니다"));
      assertThat(savedMessage.getMessageType()).isEqualTo(MessageType.USER);
      assertThat(savedMessage.getContent()).isEqualTo(messageContent);
      assertThat(savedMessage.getConversation().getId()).isEqualTo(conversation.getId());
    }

    @Test
    @Transactional
    @DisplayName("AI 응답 이벤트 발행 시 이벤트가 정상적으로 기록된다")
    void shouldRecordAiResponseReceivedEvent() {
      // given
      AiConversation conversation = createAndSaveConversation();
      String userId = conversation.getMember().getEmail();
      String messageContent = "부산 맛집 추천해줘";
      ChatMessageRequest request = new ChatMessageRequest(messageContent, MessageContentType.TEXT);

      // when
      aiMessageService.processAndSendMessage(conversation.getId(), userId, request);

      // then - AiResponseReceivedEvent가 발행되었는지 확인
      long eventCount =
          applicationEvents.stream(AiResponseReceivedEvent.class)
              .filter(event -> event.chatRoomId().equals(conversation.getId()))
              .count();

      // TODO: AI 응답 이벤트 발행 로직이 구현되면 검증
      // assertThat(eventCount).isGreaterThan(0);
    }

    @Test
    @Transactional
    @DisplayName("사용자 메시지가 여러 건 연속으로 처리되어도 모두 DB에 저장된다")
    void shouldSaveMultipleUserMessages() {
      // given
      AiConversation conversation = createAndSaveConversation();
      String userId = conversation.getMember().getEmail();

      ChatMessageRequest request1 = new ChatMessageRequest("서울 여행 계획 짜줘", MessageContentType.TEXT);
      ChatMessageRequest request2 = new ChatMessageRequest("부산은 어때?", MessageContentType.TEXT);
      ChatMessageRequest request3 = new ChatMessageRequest("제주도 추천해줘", MessageContentType.TEXT);

      // when
      aiMessageService.processAndSendMessage(conversation.getId(), userId, request1);
      aiMessageService.processAndSendMessage(conversation.getId(), userId, request2);
      aiMessageService.processAndSendMessage(conversation.getId(), userId, request3);

      entityManager.flush();
      entityManager.clear();

      // then
      long messageCount = aiMessageRepository.count();
      assertThat(messageCount).isEqualTo(3);
    }
  }

  @Nested
  @DisplayName("이벤트 리스너 및 WebSocket 전송 통합 테스트")
  class EventListenerAndWebSocketIntegration {

    @Test
    @Transactional
    @DisplayName("완료된 CHAT 메시지는 DB에 ASSISTANT 메시지로 저장된다")
    void shouldSaveCompletedChatMessageAsAssistant() {
      // given
      AiConversation conversation = createAndSaveConversation();
      String aiResponse = "서울 3박 4일 여행 일정을 생성했습니다.";

      // AiResponseReceivedEvent를 직접 발행하여 리스너 동작 검증
      AiResponseReceivedEvent event =
          new AiResponseReceivedEvent(
              conversation.getId(),
              conversation.getTripPlan().getId(),
              conversation.getMember().getEmail(),
              aiResponse,
              com.swygbro.airoad.backend.ai.domain.dto.AiResponseContentType.CHAT,
              true);

      // when
      // TODO: 이벤트를 발행하는 실제 로직이 구현되면 테스트
      // applicationEventPublisher.publishEvent(event);
      // entityManager.flush();
      // entityManager.clear();

      // then
      // TODO: ASSISTANT 메시지가 DB에 저장되었는지 검증
      // List<AiMessage> assistantMessages = aiMessageRepository.findAll();
      // assertThat(assistantMessages).hasSize(1);
      // assertThat(assistantMessages.get(0).getMessageType()).isEqualTo(MessageType.ASSISTANT);
      // assertThat(assistantMessages.get(0).getContent()).isEqualTo(aiResponse);
    }

    @Test
    @Transactional
    @DisplayName("CHAT 타입 응답은 /sub/chat/{chatRoomId} 채널로 전송된다")
    void shouldSendChatResponseToCorrectChannel() {
      // given
      AiConversation conversation = createAndSaveConversation();
      String aiResponse = "여행 일정을 생성 중입니다...";
      String expectedChannel = "/sub/chat/" + conversation.getId();

      // when
      // TODO: WebSocket 전송 로직이 통합되면 검증
      // AiResponseReceivedEvent를 발행하고 리스너가 WebSocket으로 전송하는지 확인

      // then
      // verify(messagingTemplate)
      //     .convertAndSendToUser(
      //         eq(conversation.getMember().getEmail()), eq(expectedChannel), eq(aiResponse));
    }

    @Test
    @Transactional
    @DisplayName("SCHEDULE 타입 응답은 /sub/schedule/{tripPlanId} 채널로 전송된다")
    void shouldSendScheduleResponseToCorrectChannel() {
      // given
      AiConversation conversation = createAndSaveConversation();
      String scheduleJson = "{\"day\": 1, \"activities\": [...]}";
      String expectedChannel = "/sub/schedule/" + conversation.getTripPlan().getId();

      // when
      // TODO: SCHEDULE 타입 이벤트 발행 및 전송 로직이 구현되면 검증

      // then
      // verify(messagingTemplate)
      //     .convertAndSendToUser(
      //         eq(conversation.getMember().getEmail()), eq(expectedChannel),
      // eq(scheduleJson));
    }

    @Test
    @Transactional
    @DisplayName("스트리밍 청크가 순차적으로 WebSocket을 통해 전송된다")
    void shouldSendStreamingChunksSequentially() {
      // given
      AiConversation conversation = createAndSaveConversation();
      String chunk1 = "서울";
      String chunk2 = " 3박";
      String chunk3 = " 4일";
      String chunk4 = " 여행 계획";

      // when
      // TODO: 스트리밍 청크를 순차적으로 발행하는 로직이 구현되면 검증

      // then
      // verify(messagingTemplate, times(4))
      //     .convertAndSendToUser(
      //         eq(conversation.getMember().getEmail()),
      //         eq("/sub/chat/" + conversation.getId()),
      //         contentCaptor.capture());
      //
      // List<String> capturedChunks = contentCaptor.getAllValues();
      // assertThat(capturedChunks).containsExactly(chunk1, chunk2, chunk3, chunk4);
    }
  }

  @Nested
  @DisplayName("예외 처리 및 트랜잭션 통합 테스트")
  class ExceptionHandlingAndTransactionIntegration {

    @Test
    @Transactional
    @DisplayName("WebSocket 전송 실패 시에도 완료된 메시지는 DB에 저장된다")
    void shouldSaveToDatabaseEvenWhenWebSocketFails() {
      // given
      AiConversation conversation = createAndSaveConversation();
      String aiResponse = "부산 여행 일정입니다.";

      // WebSocket 전송 실패 시뮬레이션
      doThrow(new RuntimeException("WebSocket 연결 실패"))
          .when(messagingTemplate)
          .convertAndSendToUser(anyString(), anyString(), anyString());

      // when & then
      // TODO: 이벤트 발행 후 WebSocket 실패해도 DB 저장은 성공하는지 검증
      // assertThatCode(() -> {
      //     // 이벤트 발행
      //     applicationEventPublisher.publishEvent(event);
      //     entityManager.flush();
      // }).doesNotThrowAnyException();
      //
      // List<AiMessage> savedMessages = aiMessageRepository.findAll();
      // assertThat(savedMessages).isNotEmpty();
    }

    @Test
    @Transactional
    @DisplayName("권한이 없는 사용자의 메시지는 처리되지 않는다")
    void shouldRejectUnauthorizedUserMessage() {
      // given
      AiConversation conversation = createAndSaveConversation();
      String ownerId = conversation.getMember().getEmail();
      String unauthorizedUserId = "unauthorized@example.com";
      ChatMessageRequest request = new ChatMessageRequest("안녕하세요", MessageContentType.TEXT);

      // when & then
      assertThatThrownBy(
              () ->
                  aiMessageService.processAndSendMessage(
                      conversation.getId(), unauthorizedUserId, request))
          .isInstanceOf(com.swygbro.airoad.backend.common.exception.BusinessException.class);

      // 메시지가 저장되지 않았는지 확인
      long messageCount = aiMessageRepository.count();
      assertThat(messageCount).isEqualTo(0);
    }
  }

  // ======================== Private Helper Methods ========================

  private Member createAndSaveMember(String email, String name) {
    Member member =
        Member.builder()
            .email(email)
            .name(name)
            .imageUrl("https://example.com/image.png")
            .provider(ProviderType.GOOGLE)
            .role(MemberRole.MEMBER)
            .build();
    entityManager.persist(member);
    entityManager.flush();
    return member;
  }

  private TripPlan createAndSaveTripPlan(Member member, String title) {
    TripPlan tripPlan =
        TripPlan.builder()
            .member(member)
            .title(title)
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(3))
            .isCompleted(false)
            .region("서울")
            .transportation(Transportation.PUBLIC_TRANSIT)
            .peopleCount(2)
            .build();
    entityManager.persist(tripPlan);
    entityManager.flush();
    return tripPlan;
  }

  private AiConversation createAndSaveConversation() {
    Member member = createAndSaveMember("test@example.com", "테스트유저");
    TripPlan tripPlan = createAndSaveTripPlan(member, "서울 여행");
    AiConversation conversation =
        AiConversation.builder().member(member).tripPlan(tripPlan).build();
    entityManager.persist(conversation);
    entityManager.flush();
    return conversation;
  }
}
*/
class AiMessageIntegrationTest {}
