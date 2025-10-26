package com.swygbro.airoad.backend.websocket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Value("${cors.allowed-origins}")
  private String allowedOrigins;

  private final JwtWebSocketInterceptor jwtWebSocketInterceptor;
  private final WebSocketPayloadTypeInterceptor webSocketPayloadTypeInterceptor;

  private static final int MESSAGE_SIZE_LIMIT = 2 * 1024 * 1024; // 2MB
  private static final int SEND_BUFFER_SIZE_LIMIT = 2 * 1024 * 1024; // 2MB
  private static final int TIME_TO_FIRST_LIMIT_MS = 30_000;
  private static final int SEND_TIME_LIMIT_MS = 20_000;

  @Bean("wsHeartbeatScheduler")
  public ThreadPoolTaskScheduler wsHeartbeatScheduler() {
    var ts = new ThreadPoolTaskScheduler();
    ts.setPoolSize(1);
    ts.setThreadNamePrefix("ws-heartbeat-");
    ts.initialize();
    return ts;
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws-stomp").setAllowedOrigins(allowedOrigins);

    registry.addEndpoint("/ws-stomp").setAllowedOrigins(allowedOrigins).withSockJS();
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    // Simple Broker 사용 (AI와의 1:1 채팅을 위한 /sub prefix)
    // TaskScheduler는 Spring Boot가 자동으로 생성하므로 명시적 설정 불필요
    registry
        .enableSimpleBroker("/sub")
        .setTaskScheduler(wsHeartbeatScheduler())
        .setHeartbeatValue(new long[] {10000, 10000}); // 10초마다 ping/pong
    registry.setApplicationDestinationPrefixes("/pub");
    registry.setUserDestinationPrefix("/user");
  }

  @Override
  public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
    registry
        .setMessageSizeLimit(MESSAGE_SIZE_LIMIT) // 클라이언트→서버로 들어오는 단일 WebSocket 메세지의 최대 크기
        .setSendBufferSizeLimit(
            SEND_BUFFER_SIZE_LIMIT) // 서버→클라이언트로 보내려다 아직 소켓으로 못 보낸 누적 버퍼(출력 큐)의 상한
        .setSendTimeLimit(SEND_TIME_LIMIT_MS) // 서버가 한 세션에 메시지를 보내는 데 허용하는 총 시간
        .setTimeToFirstMessage(TIME_TO_FIRST_LIMIT_MS); // 웹 소켓이 connect되고 첫 메세지가 오기 전까지 허용하는 총 시간
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(jwtWebSocketInterceptor, webSocketPayloadTypeInterceptor);
  }
}
