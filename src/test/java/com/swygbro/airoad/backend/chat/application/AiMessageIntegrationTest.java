/*
package com.swygbro.airoad.backend.chat.application;


/**
 * AI 메시지 처리 통합 테스트(다른 도메인이 구현되면 진행)
 *
 * <p>Service → EventPublisher → EventListener → WebSocket 전체 흐름을 검증합니다.
 */

/*
@SpringBootTest
@ActiveProfiles("test")
class AiMessageIntegrationTest {

  @TestConfiguration
  static class TestConfig {
    @Bean
    @Primary
    public SimpMessagingTemplate simpMessagingTemplate() {
      return Mockito.mock(SimpMessagingTemplate.class);
    }
  }

  @Autowired private AiMessageService aiMessageService;

  @Autowired private AiConversationRepository aiConversationRepository;

  @Autowired private SimpMessagingTemplate messagingTemplate;

  @Autowired private EntityManager entityManager;


  @Captor ArgumentCaptor<ChatMessageResponse> responseCaptor;

  @BeforeEach
  void setUp() {
    Mockito.reset(messagingTemplate);
  }

  @AfterEach
  void tearDown() {
    Mockito.reset(messagingTemplate);
  }

  @Nested
  @DisplayName("메시지 처리 통합 테스트")
  class MessageProcessingIntegration {

    @Test
    @DisplayName("메시지 처리 후 이벤트가 리스너에게 전달되어 WebSocket으로 전송된다")
    void shouldSendMessageViaWebSocketAfterProcessing() {
      // given
      AiConversation conversation = AiConversationFixture.createConversation();
      Long chatRoomId = conversation.getId();
      String messageContent = "서울 3박 4일 여행 계획을 짜주세요";
      ChatMessageRequest request = new ChatMessageRequest(messageContent, MessageContentType.TEXT);
      AiConversation aiConversation = createAndSaveConversation();

      // when
      aiMessageService.processAndSendMessage(aiConversation.getId(), aiConversation.getMember().getName(), request);

      // then - 실제로 messagingTemplate이 호출되었는지 검증
      verify(messagingTemplate)
          .convertAndSend(eq("/topic/chat/" + chatRoomId), responseCaptor.capture());

      ChatMessageResponse capturedResponse = responseCaptor.getValue();
      assertThat(capturedResponse).isNotNull();
      assertThat(capturedResponse.messageType()).isEqualTo(MessageType.ASSISTANT);
      assertThat(capturedResponse.content()).contains(messageContent);
    }

    @Test
    @DisplayName("트랜잭션 커밋 후 이벤트가 발행되어 WebSocket으로 전송된다")
    void shouldPublishEventAfterTransactionCommit() {
      // given
      AiConversation conversation = AiConversationFixture.createConversation();
      AiConversation savedConversation = aiConversationRepository.save(conversation);
      Long chatRoomId = savedConversation.getId();
      String userId = "user123";
      String messageContent = "부산 맛집 추천해줘";
      ChatMessageRequest request = new ChatMessageRequest(messageContent, MessageContentType.TEXT);

      // when
      aiMessageService.processAndSendMessage(chatRoomId, userId, request);

      // then - 트랜잭션 커밋 후 이벤트가 발행되어 WebSocket 전송이 이루어져야 함
      verify(messagingTemplate, times(1))
          .convertAndSend(eq("/topic/chat/" + chatRoomId), any(ChatMessageResponse.class));
    }
  }


    // ======================== Private Helper Methods ========================

    /**
     * 테스트용 Member 생성 및 저장
     *
     * @param email 이메일
     * @param name 이름
     * @return 저장된 Member 객체
     */
/*
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


    private AiConversation createAndSaveConversation(Member member, TripPlan tripPlan) {
        AiConversation conversation =
                AiConversation.builder().member(member).tripPlan(tripPlan).build();
        return aiConversationRepository.save(conversation);
    }


    private AiConversation createAndSaveConversation() {
        Member member = createAndSaveMember("another@example.com", "다른유저");
        TripPlan tripPlan = createAndSaveTripPlan(member, "제주 여행");
        AiConversation aiConversation = AiConversation.builder()
                .member(member)
                .tripPlan(tripPlan)
                .build();
        entityManager.persist(tripPlan);
        entityManager.flush();
        return aiConversation;
    }
}
*/
