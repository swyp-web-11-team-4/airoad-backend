package com.swygbro.airoad.backend.common.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER,
    paramName = "Authorization")
public class SwaggerConfig {

  @Value("${swagger.url:http://localhost:8080}")
  private String url;

  @Bean
  public OpenAPI openAPI() {
    Contact contact =
        new Contact().name("SWYP Web 11기 Team 4").url("https://github.com/swyp-web-11-team-4");

    Info info =
        new Info()
            .title("API Documentation")
            .description(
                """
                ## SWYP Web 11기 Team 4 Backend API

                이 문서는 백엔드 REST API의 전체 엔드포인트를 설명합니다.

                ### 인증 방식
                - JWT Bearer Token 인증을 사용합니다.
                - 대부분의 API는 인증이 필요하며, 우측 상단의 'Authorize' 버튼을 통해 토큰을 설정할 수 있습니다.

                ### 공통 응답 형식
                - 모든 응답: `CommonResponse<T>` 래퍼 사용
                  ```json
                  {
                    "success": true,
                    "status": 200,
                    "data": {
                      ...
                    }
                  }
                  ```

                ### 응답 상태 코드
                - `200 OK`: 요청 성공
                - `201 Created`: 리소스 생성 성공
                - `400 Bad Request`: 잘못된 요청
                - `401 Unauthorized`: 인증 실패
                - `404 Not Found`: 리소스를 찾을 수 없음
                - `500 Internal Server Error`: 서버 내부 오류


                ## WebSocket (STOMP) 실시간 채팅 API

                ### 연결 정보
                - **WebSocket 엔드포인트**: `ws://localhost:8080/ws-stomp` (개발환경)
                - **프로덕션**: `wss://api.example.com/ws-stomp` (HTTPS 환경에서는 반드시 WSS 사용)

                ### 메시지 송신 (Client → Server)
                - **메시지 전송**: `/pub/chatroom/{roomId}/message`
                  - Payload: `{"content": "메시지 내용"}`
                - **채팅방 입장**: `/pub/chatroom/{roomId}/enter`
                - **채팅방 퇴장**: `/pub/chatroom/{roomId}/leave`

                ### 메시지 구독 (Server → Client)
                - **실시간 메시지 수신**: `/sub/chatroom/{roomId}`
                  - 해당 채팅방의 모든 메시지를 실시간으로 수신

                ### 연결 예시 (JavaScript/React)
                ```javascript
                // 1. 연결
                const socket = new SockJS('http://localhost:8080/ws-stomp');
                const stompClient = Stomp.over(socket);

                // 2. 구독
                stompClient.connect({}, () => {
                  stompClient.subscribe('/sub/chatroom/1', (message) => {
                    const data = JSON.parse(message.body);
                    console.log('새 메시지:', data);
                  });

                  // 3. 메시지 전송
                  stompClient.send('/pub/chatroom/1/message', {},
                    JSON.stringify({content: '안녕하세요'})
                  );
                });
                ```

                ### 참고
                - 과거 메시지 조회는 REST API 사용: `GET /api/v1/chatroom/{roomId}/messages`
                - 필요한 라이브러리: `sockjs-client`, `stompjs` (또는 `@stomp/stompjs`)
                - React 구현 가이드: https://khdscor.tistory.com/122
                """)
            .contact(contact);

    Server server = new Server().url(url);

    SecurityRequirement securityRequirement = new SecurityRequirement().addList("bearerAuth");

    return new OpenAPI().info(info).servers(List.of(server)).addSecurityItem(securityRequirement);
  }
}
