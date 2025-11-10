package com.swygbro.airoad.backend.chat.presentation.web;

import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * STOMP RECEIPT 프레임을 수동으로 전송하는 인터셉터
 *
 * <p>SimpleBroker는 RECEIPT를 지원하지 않으므로, SUBSCRIBE 시 receipt 헤더가 있으면 수동으로 RECEIPT 프레임을 전송합니다.
 *
 * <p>이 인터셉터는 clientInboundChannel에 등록되어 SUBSCRIBE 메시지를 감지하고, SimpMessagingTemplate을 통해 RECEIPT 프레임을
 * 전송합니다.
 *
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/21848">SPR-17315</a>
 */
@Slf4j
@Component
public class ReceiptInterceptor implements ChannelInterceptor {

  private final SimpMessagingTemplate messagingTemplate;

  public ReceiptInterceptor(@Lazy SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  @Override
  public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
    if (!sent) {
      return;
    }

    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
      String receipt = accessor.getReceipt();

      if (receipt != null) {
        // RECEIPT 프레임 생성
        StompHeaderAccessor receiptAccessor = StompHeaderAccessor.create(StompCommand.RECEIPT);
        receiptAccessor.setReceiptId(receipt);
        receiptAccessor.setSessionId(accessor.getSessionId());
        receiptAccessor.setLeaveMutable(true);

        Message<byte[]> receiptMessage =
            MessageBuilder.createMessage(new byte[0], receiptAccessor.getMessageHeaders());

        // SimpMessagingTemplate의 메시지 채널을 통해 RECEIPT 전송
        messagingTemplate.getMessageChannel().send(receiptMessage);
        log.debug(
            "[WebSocket] RECEIPT 전송 완료 - receiptId: {}, sessionId: {}",
            receipt,
            accessor.getSessionId());
      }
    }
  }
}
