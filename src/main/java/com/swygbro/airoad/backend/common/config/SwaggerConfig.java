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


                ## WebSocket (STOMP) AI 1:1 채팅 API

                ### 연결 정보
                - **WebSocket 엔드포인트**: `ws://localhost:8080/ws-stomp` (개발환경)
                - **프로덕션**: `wss://api.example.com/ws-stomp` (HTTPS 환경에서는 반드시 WSS 사용)

                ### 채팅 구조
                - AI와의 1:1 채팅
                - 한 사용자는 여러 채팅방(대화 세션)을 가질 수 있음
                - 각 채팅방은 독립적인 AI 대화 컨텍스트 유지

                ### 메시지 송신 (Client → Server)
                - **메시지 전송**: `/pub/chat/{chatRoomId}/message`
                  - Payload: `{"content": "메시지 내용", "messageType": "TEXT"}`
                  - `chatRoomId`: 채팅방 ID (Long)

                ### 메시지 구독 (Server → Client)
                실시간 메시징은 **경로 구분 방식**을 사용합니다. 각 메시지 타입은 독립적인 구독 경로를 가지며, 클라이언트는 필요한 경로만 선택적으로 구독할 수 있습니다.

                #### 1. 채팅 메시지
                - **경로**: `/user/sub/chat/{chatRoomId}`
                - **페이로드**: `ChatMessageResponse`
                - **용도**: AI와의 1:1 대화 메시지

                #### 2. 일정 dto (향후 구현)
                - **경로**: `/user/sub/schedule`
                - **페이로드**: `TripPlanDto(아직 미정)`
                - **용도**: ai로부터 받아오는 여행 일정 전송(1일 단뒤)


                **참고**: `/user` prefix는 Spring이 자동으로 현재 사용자에게만 메시지를 전달합니다.

                ### 연결 예시 (JavaScript/React)
                ```javascript
                // 1. 연결
                const socket = new SockJS('http://localhost:8080/ws-stomp');
                const stompClient = Stomp.over(socket);

                stompClient.connect({}, (frame) => {
                  console.log('연결됨. 사용자 ID:', frame.headers['user-name']);

                  // 2. 채팅 메시지 구독
                  stompClient.subscribe('/user/sub/chat/1', (message) => {
                    const chatMsg = JSON.parse(message.body);
                    displayChatMessage(chatMsg);  // ChatMessageResponse 처리
                  });

                  // 3. 일정 받아오기 구독
                  stompClient.subscribe('/user/sub/schedule', (message) => {
                    const schedule = JSON.parse(message.body);
                    showScheduleNotification(schedule);  // ScheduleNotification 처리
                  });

                  // 4. 채팅방 1로 메시지 전송
                  stompClient.send('/pub/chat/1/message', {},
                    JSON.stringify({
                      content: '안녕하세요',
                      messageType: 'TEXT'
                    })
                  );
                });
                ```

                ### 참고
                - 필요한 라이브러리: `sockjs-client`, `@stomp/stompjs`
                - 각 채팅방은 독립적인 구독이 필요 (채팅방 전환 시 새로 구독)
                """)
            .contact(contact);

    Server server = new Server().url(url);

    SecurityRequirement securityRequirement = new SecurityRequirement().addList("bearerAuth");

    return new OpenAPI().info(info).servers(List.of(server)).addSecurityItem(securityRequirement);
  }
}
