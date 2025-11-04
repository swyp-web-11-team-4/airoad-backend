package com.swygbro.airoad.backend.chat.presentation.web;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.swygbro.airoad.backend.auth.application.JwtTokenProvider;
import com.swygbro.airoad.backend.auth.application.UserDetailsServiceImpl;
import com.swygbro.airoad.backend.auth.domain.dto.UserPrincipal;
import com.swygbro.airoad.backend.auth.infrastructure.RefreshTokenRepository;
import com.swygbro.airoad.backend.auth.presentation.web.OAuth2AuthenticationSuccessHandler;
import com.swygbro.airoad.backend.chat.application.AiMessageService;
import com.swygbro.airoad.backend.chat.domain.dto.ChatMessageResponse;
import com.swygbro.airoad.backend.chat.domain.dto.MessageContentType;
import com.swygbro.airoad.backend.common.domain.dto.CursorPageResponse;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AiConversationController.class)
@ActiveProfiles("test")
@Import(AiConversationControllerTest.TestConfig.class)
class AiConversationControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private AiMessageService aiMessageService;

  private UserPrincipal userPrincipal;

  @TestConfiguration
  static class TestConfig {
    @Bean
    public AiMessageService aiMessageService() {
      return mock(AiMessageService.class);
    }

    @Bean
    public JwtTokenProvider jwtTokenProvider() {
      return mock(JwtTokenProvider.class);
    }

    @Bean
    public UserDetailsServiceImpl userDetailsService() {
      return mock(UserDetailsServiceImpl.class);
    }

    @Bean
    public OAuth2AuthenticationSuccessHandler oAuthLoginSuccessHandler() {
      return mock(OAuth2AuthenticationSuccessHandler.class);
    }

    @Bean
    public SimpMessagingTemplate simpMessagingTemplate() {
      return mock(SimpMessagingTemplate.class);
    }

    @Bean
    public RefreshTokenRepository refreshTokenRepository() {
      return mock(RefreshTokenRepository.class);
    }
  }

  @BeforeEach
  void resetMocks() {

    reset(aiMessageService); // 스텁/호출기록 모두 초기화
    userPrincipal = mock(UserPrincipal.class);
    when(userPrincipal.getUsername()).thenReturn("user1234");
  }

  private RequestPostProcessor auth() {
    return SecurityMockMvcRequestPostProcessors.authentication(
        new UsernamePasswordAuthenticationToken(userPrincipal, null, List.of()));
  }

  @Nested
  @DisplayName("GET /api/v1/chats/{chatRoomId}/messages 는")
  class GetMessages {

    @Test
    @DisplayName("커서 없이 요청 시 최신 메시지부터 조회한다")
    void shouldReturnLatestMessagesWithoutCursor() throws Exception {
      // given
      Long chatRoomId = 1L;
      int size = 50;
      String username = "user1234";

      List<ChatMessageResponse> messages =
          List.of(
              new ChatMessageResponse(
                  2L,
                  MessageType.ASSISTANT,
                  "서울 3박 4일 여행 계획을 짜드리겠습니다.",
                  MessageContentType.TEXT,
                  null,
                  LocalDateTime.of(2025, 1, 15, 10, 31, 0)),
              new ChatMessageResponse(
                  1L,
                  MessageType.USER,
                  "서울 3박 4일 여행 계획을 짜주세요",
                  MessageContentType.TEXT,
                  null,
                  LocalDateTime.of(2025, 1, 15, 10, 30, 0)));

      CursorPageResponse<ChatMessageResponse> response = CursorPageResponse.of(messages, 3L, true);

      given(aiMessageService.getMessageHistory(chatRoomId, username, null, size))
          .willReturn(response);

      // when & then
      mockMvc
          .perform(
              get("/api/v1/chats/{chatRoomId}/messages", chatRoomId)
                  .with(auth())
                  .param("size", String.valueOf(size))
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.status").value(200))
          .andExpect(jsonPath("$.data.content").isArray())
          .andExpect(jsonPath("$.data.content.length()").value(2))
          .andExpect(jsonPath("$.data.content[0].id").value(2))
          .andExpect(jsonPath("$.data.content[0].messageType").value("ASSISTANT"))
          .andExpect(jsonPath("$.data.content[0].content").value("서울 3박 4일 여행 계획을 짜드리겠습니다."))
          .andExpect(jsonPath("$.data.content[1].id").value(1))
          .andExpect(jsonPath("$.data.content[1].messageType").value("USER"))
          .andExpect(jsonPath("$.data.nextCursor").value(3))
          .andExpect(jsonPath("$.data.hasNext").value(true))
          .andExpect(jsonPath("$.data.size").value(2));

      verify(aiMessageService).getMessageHistory(chatRoomId, username, null, size);
    }

    @Test
    @DisplayName("커서를 포함하여 요청 시 해당 커서 이전의 메시지를 조회한다")
    void shouldReturnMessagesBeforeCursor() throws Exception {
      // given
      Long chatRoomId = 1L;
      Long cursor = 5L;
      String username = "user1234";
      int size = 50;

      List<ChatMessageResponse> messages =
          List.of(
              new ChatMessageResponse(
                  4L,
                  MessageType.ASSISTANT,
                  "이전 메시지입니다.",
                  MessageContentType.TEXT,
                  null,
                  LocalDateTime.of(2025, 1, 15, 10, 25, 0)),
              new ChatMessageResponse(
                  3L,
                  MessageType.USER,
                  "더 이전 메시지입니다.",
                  MessageContentType.TEXT,
                  null,
                  LocalDateTime.of(2025, 1, 15, 10, 20, 0)));

      CursorPageResponse<ChatMessageResponse> response =
          CursorPageResponse.of(messages, null, false);

      given(aiMessageService.getMessageHistory(chatRoomId, username, cursor, size))
          .willReturn(response);

      // when & then
      mockMvc
          .perform(
              get("/api/v1/chats/{chatRoomId}/messages", chatRoomId)
                  .with(auth())
                  .param("size", String.valueOf(size))
                  .param("cursor", String.valueOf(cursor))
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.content.length()").value(2))
          .andExpect(jsonPath("$.data.content[0].id").value(4))
          .andExpect(jsonPath("$.data.content[1].id").value(3))
          .andExpect(jsonPath("$.data.hasNext").value(false));

      verify(aiMessageService).getMessageHistory(chatRoomId, username, cursor, size);
    }

    @Test
    @DisplayName("size 파라미터 없이 요청 시 기본값 50으로 조회한다")
    void shouldUseDefaultSizeWhenSizeParameterIsNotProvided() throws Exception {
      // given
      Long chatRoomId = 1L;
      int defaultSize = 50;
      String username = "user1234";

      List<ChatMessageResponse> messages =
          List.of(
              new ChatMessageResponse(
                  1L,
                  MessageType.USER,
                  "테스트 메시지",
                  MessageContentType.TEXT,
                  null,
                  LocalDateTime.of(2025, 1, 15, 10, 30, 0)));

      CursorPageResponse<ChatMessageResponse> response = CursorPageResponse.last(messages);

      given(aiMessageService.getMessageHistory(chatRoomId, username, null, defaultSize))
          .willReturn(response);

      // when & then
      mockMvc
          .perform(
              get("/api/v1/chats/{chatRoomId}/messages", chatRoomId)
                  .with(auth())
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.content.length()").value(1));

      // size 파라미터를 전달하지 않았으므로 기본값 50으로 호출되었는지 검증
      verify(aiMessageService).getMessageHistory(chatRoomId, username, null, defaultSize);
    }

    @Test
    @DisplayName("메시지가 없는 경우 빈 배열을 반환한다")
    void shouldReturnEmptyArrayWhenNoMessages() throws Exception {
      // given
      Long chatRoomId = 1L;
      int size = 50;
      String username = "user1234";

      CursorPageResponse<ChatMessageResponse> response = CursorPageResponse.last(List.of());

      given(aiMessageService.getMessageHistory(chatRoomId, username, null, size))
          .willReturn(response);

      // when & then
      mockMvc
          .perform(
              get("/api/v1/chats/{chatRoomId}/messages", chatRoomId)
                  .with(auth())
                  .param("size", String.valueOf(size))
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.content").isArray())
          .andExpect(jsonPath("$.data.content.length()").value(0))
          .andExpect(jsonPath("$.data.hasNext").value(false))
          .andExpect(jsonPath("$.data.size").value(0));

      verify(aiMessageService).getMessageHistory(chatRoomId, username, null, size);
    }

    @Test
    @DisplayName("사용자 정의 size로 요청 시 해당 개수만큼 조회한다")
    void shouldReturnMessagesWithCustomSize() throws Exception {
      // given
      Long chatRoomId = 1L;
      int customSize = 10;
      String username = "user1234";

      List<ChatMessageResponse> messages =
          List.of(
              new ChatMessageResponse(
                  1L,
                  MessageType.USER,
                  "테스트 메시지",
                  MessageContentType.TEXT,
                  null,
                  LocalDateTime.of(2025, 1, 15, 10, 30, 0)));

      CursorPageResponse<ChatMessageResponse> response = CursorPageResponse.of(messages, 2L, true);

      given(aiMessageService.getMessageHistory(chatRoomId, username, null, customSize))
          .willReturn(response);

      // when & then
      mockMvc
          .perform(
              get("/api/v1/chats/{chatRoomId}/messages", chatRoomId)
                  .with(auth())
                  .param("size", String.valueOf(customSize))
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.size").value(1));

      verify(aiMessageService).getMessageHistory(chatRoomId, username, null, customSize);
    }
  }
}
