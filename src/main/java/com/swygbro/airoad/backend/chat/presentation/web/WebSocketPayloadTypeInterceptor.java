package com.swygbro.airoad.backend.chat.presentation.web;

import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import lombok.extern.slf4j.Slf4j;

/** WebSocket/STOMP 통신 시 기본 Content-Type을 보장하기 위한 인터셉터입니다. */
@Slf4j
@Component
public class WebSocketPayloadTypeInterceptor implements ChannelInterceptor {

  @Override
  public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor == null) {
      return message;
    }

    if (accessor.getCommand() == StompCommand.SEND && accessor.getContentType() == null) {
      accessor.setContentType(MimeTypeUtils.APPLICATION_JSON);
    }

    accessor.setLeaveMutable(true);
    return message;
  }
}
