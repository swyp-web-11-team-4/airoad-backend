package com.swygbro.airoad.backend.chat.presentation.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReceiptInterceptor 클래스")
class ReceiptInterceptorTest {

  @Mock private MessageChannel mockChannel;

  @InjectMocks private ReceiptInterceptor receiptInterceptor;

  @Nested
  @DisplayName("postSend 메서드는")
  class PostSend {

    @Test
    @DisplayName("SUBSCRIBE 메시지에 receipt 헤더가 있으면 RECEIPT 프레임을 전송한다")
    void sendReceiptWhenSubscribeWithReceiptHeader() {
      // given
      String receiptId = "receipt-123";
      String sessionId = "session-abc";

      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
      accessor.setReceipt(receiptId);
      accessor.setSessionId(sessionId);
      accessor.setDestination("/user/sub/chat/46");

      Message<?> subscribeMessage =
          MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

      // when
      receiptInterceptor.postSend(subscribeMessage, mockChannel, true);

      // then
      then(mockChannel).should(times(1)).send(messageCaptor.capture());

      Message<?> sentMessage = messageCaptor.getValue();
      StompHeaderAccessor sentAccessor = StompHeaderAccessor.wrap(sentMessage);

      assertThat(sentAccessor.getCommand()).isEqualTo(StompCommand.RECEIPT);
      assertThat(sentAccessor.getReceiptId()).isEqualTo(receiptId);
      assertThat(sentAccessor.getSessionId()).isEqualTo(sessionId);
    }

    @Test
    @DisplayName("SUBSCRIBE 메시지에 receipt 헤더가 없으면 RECEIPT를 전송하지 않는다")
    void doNotSendReceiptWhenSubscribeWithoutReceiptHeader() {
      // given
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
      accessor.setSessionId("session-abc");
      accessor.setDestination("/user/sub/chat/46");
      // receipt 헤더 설정하지 않음

      Message<?> subscribeMessage =
          MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      // when
      receiptInterceptor.postSend(subscribeMessage, mockChannel, true);

      // then
      then(mockChannel).should(never()).send(any());
    }

    @Test
    @DisplayName("메시지 전송이 실패하면 RECEIPT를 전송하지 않는다")
    void doNotSendReceiptWhenMessageSendFailed() {
      // given
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
      accessor.setReceipt("receipt-123");
      accessor.setSessionId("session-abc");

      Message<?> subscribeMessage =
          MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      // when
      receiptInterceptor.postSend(subscribeMessage, mockChannel, false); // sent = false

      // then
      then(mockChannel).should(never()).send(any());
    }

    @Test
    @DisplayName("SUBSCRIBE가 아닌 다른 명령어는 무시한다")
    void ignoreNonSubscribeCommands() {
      // given
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
      accessor.setReceipt("receipt-123");
      accessor.setSessionId("session-abc");

      Message<?> messageCommand =
          MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      // when
      receiptInterceptor.postSend(messageCommand, mockChannel, true);

      // then
      then(mockChannel).should(never()).send(any());
    }

    @Test
    @DisplayName("CONNECT 명령어는 무시한다")
    void ignoreConnectCommand() {
      // given
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
      accessor.setReceipt("receipt-connect");
      accessor.setSessionId("session-abc");

      Message<?> connectMessage =
          MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      // when
      receiptInterceptor.postSend(connectMessage, mockChannel, true);

      // then
      then(mockChannel).should(never()).send(any());
    }

    @Test
    @DisplayName("SEND 명령어는 무시한다")
    void ignoreSendCommand() {
      // given
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
      accessor.setReceipt("receipt-send");
      accessor.setSessionId("session-abc");

      Message<?> sendMessage =
          MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      // when
      receiptInterceptor.postSend(sendMessage, mockChannel, true);

      // then
      then(mockChannel).should(never()).send(any());
    }

    @Test
    @DisplayName("STOMP 헤더가 없는 메시지는 무시한다")
    void ignoreNonStompMessage() {
      // given
      Message<?> nonStompMessage = MessageBuilder.withPayload("plain text").build();

      // when
      receiptInterceptor.postSend(nonStompMessage, mockChannel, true);

      // then
      then(mockChannel).should(never()).send(any());
    }

    @Test
    @DisplayName("RECEIPT 프레임의 body는 비어있다")
    void receiptFrameHasEmptyBody() {
      // given
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
      accessor.setReceipt("receipt-test");
      accessor.setSessionId("session-test");

      Message<?> subscribeMessage =
          MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

      // when
      receiptInterceptor.postSend(subscribeMessage, mockChannel, true);

      // then
      then(mockChannel).should().send(messageCaptor.capture());

      Message<?> sentMessage = messageCaptor.getValue();
      byte[] payload = (byte[]) sentMessage.getPayload();

      assertThat(payload).isEmpty();
    }

    @Test
    @DisplayName("여러 SUBSCRIBE 메시지에 대해 각각 RECEIPT를 전송한다")
    void sendMultipleReceipts() {
      // given
      String sessionId = "session-multi";

      // 첫 번째 구독
      StompHeaderAccessor accessor1 = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
      accessor1.setReceipt("receipt-chat");
      accessor1.setSessionId(sessionId);
      Message<?> subscribe1 =
          MessageBuilder.createMessage(new byte[0], accessor1.getMessageHeaders());

      // 두 번째 구독
      StompHeaderAccessor accessor2 = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
      accessor2.setReceipt("receipt-schedule");
      accessor2.setSessionId(sessionId);
      Message<?> subscribe2 =
          MessageBuilder.createMessage(new byte[0], accessor2.getMessageHeaders());

      // 세 번째 구독
      StompHeaderAccessor accessor3 = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
      accessor3.setReceipt("receipt-errors");
      accessor3.setSessionId(sessionId);
      Message<?> subscribe3 =
          MessageBuilder.createMessage(new byte[0], accessor3.getMessageHeaders());

      ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

      // when
      receiptInterceptor.postSend(subscribe1, mockChannel, true);
      receiptInterceptor.postSend(subscribe2, mockChannel, true);
      receiptInterceptor.postSend(subscribe3, mockChannel, true);

      // then
      then(mockChannel).should(times(3)).send(messageCaptor.capture());

      var sentMessages = messageCaptor.getAllValues();
      assertThat(sentMessages).hasSize(3);

      // 각 RECEIPT의 receipt-id 검증
      assertThat(StompHeaderAccessor.wrap(sentMessages.get(0)).getReceiptId())
          .isEqualTo("receipt-chat");
      assertThat(StompHeaderAccessor.wrap(sentMessages.get(1)).getReceiptId())
          .isEqualTo("receipt-schedule");
      assertThat(StompHeaderAccessor.wrap(sentMessages.get(2)).getReceiptId())
          .isEqualTo("receipt-errors");
    }

    @Test
    @DisplayName("서로 다른 세션의 SUBSCRIBE에 대해 각각 올바른 세션으로 RECEIPT를 전송한다")
    void sendReceiptToCorrectSession() {
      // given
      String session1 = "session-user1";
      String session2 = "session-user2";

      StompHeaderAccessor accessor1 = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
      accessor1.setReceipt("receipt-1");
      accessor1.setSessionId(session1);
      Message<?> subscribe1 =
          MessageBuilder.createMessage(new byte[0], accessor1.getMessageHeaders());

      StompHeaderAccessor accessor2 = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
      accessor2.setReceipt("receipt-2");
      accessor2.setSessionId(session2);
      Message<?> subscribe2 =
          MessageBuilder.createMessage(new byte[0], accessor2.getMessageHeaders());

      ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

      // when
      receiptInterceptor.postSend(subscribe1, mockChannel, true);
      receiptInterceptor.postSend(subscribe2, mockChannel, true);

      // then
      then(mockChannel).should(times(2)).send(messageCaptor.capture());

      var sentMessages = messageCaptor.getAllValues();

      // 첫 번째 RECEIPT는 session1으로
      assertThat(StompHeaderAccessor.wrap(sentMessages.get(0)).getSessionId()).isEqualTo(session1);

      // 두 번째 RECEIPT는 session2로
      assertThat(StompHeaderAccessor.wrap(sentMessages.get(1)).getSessionId()).isEqualTo(session2);
    }
  }
}
