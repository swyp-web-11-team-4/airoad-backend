package com.swygbro.airoad.backend.common.config;

import java.util.List;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.swygbro.airoad.backend.chat.domain.dto.response.ChatMessageResponse;
import com.swygbro.airoad.backend.chat.domain.dto.response.ChatStreamDto;
import com.swygbro.airoad.backend.common.domain.dto.ErrorResponse;
import com.swygbro.airoad.backend.trip.domain.dto.TripPlanProgressMessage;
import com.swygbro.airoad.backend.trip.domain.dto.response.ChannelIdResponse;
import com.swygbro.airoad.backend.trip.domain.dto.response.DailyPlanResponse;
import com.swygbro.airoad.backend.trip.domain.dto.response.TripPlanDetailResponse;
import com.swygbro.airoad.backend.trip.domain.dto.response.TripPlanResponse;

import io.swagger.v3.core.converter.ModelConverters;
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
                - **인증 방식**: STOMP CONNECT 프레임에 `Authorization: Bearer <JWT>` 헤더 필수

                ### 채팅 구조
                - AI와의 1:1 채팅
                - 한 사용자는 여러 채팅방(대화 세션)을 가질 수 있음
                - 각 채팅방은 독립적인 AI 대화 컨텍스트 유지
                - 각 채팅방은 하나의 여행 계획(TripPlan)과 연결됨

                ### 메시지 송신 (Client → Server)
                - **메시지 전송**: `/pub/chat/{chatRoomId}/message`
                  - Payload: `{"content": "메시지 내용", "messageType": "TEXT"}`
                  - `chatRoomId`: 채팅방 ID (Long)

                ### 메시지 구독 (Server → Client)
                실시간 메시징은 **경로 구분 방식**을 사용합니다. 각 메시지 타입은 독립적인 구독 경로를 가지며, 클라이언트는 필요한 경로만 선택적으로 구독할 수 있습니다.

                #### 1. 채팅 메시지
                - **경로**: `/user/sub/chat/{chatRoomId}`
                - **페이로드**: `ChatStreamDto`
                - **용도**: AI와의 1:1 대화 메시지

                #### 2. 일정 데이터
                - **경로**: `/user/sub/schedule/{tripPlanId}`
                - **페이로드**: `TripPlanProgressMeesage`
                - **용도**: AI로부터 받아오는 여행 일정 전송 (여행 계획별로 구분)

                #### 3. 에러 메시지
                - **경로**: `/user/sub/errors/{chatRoomId}`
                - **페이로드**: `ErrorResponse`
                  ```json
                  {
                    "code": "WS999",
                    "message": "메시지 처리 중 오류가 발생했습니다.",
                    "path": "/websocket"
                  }
                  ```
                - **용도**: 특정 채팅방에서 발생한 WebSocket 예외를 사용자에게 전달
                - **에러 코드**:
                  - `WS999`: 일반 예외 (서버 내부 오류)
                  - 기타 비즈니스 예외 코드 (도메인별 ErrorCode 참조)

                **참고**: `/user` prefix는 Spring이 자동으로 현재 사용자에게만 메시지를 전달합니다.

                ### 에러 처리 방식

                WebSocket 연결 단계에 따라 두 가지 에러 처리 방식을 사용합니다:

                #### 1. STOMP ERROR 프레임 (연결 단계 에러)
                **발생 시점**: CONNECT, SUBSCRIBE 단계
                - JWT 인증 실패 (CONNECT)
                - 허용되지 않은 경로 구독 시도 (SUBSCRIBE)

                **특징**:
                - STOMP 프로토콜 레벨 에러 (프로토콜 위반)
                - 클라이언트의 `onError` 콜백으로 전달됨
                - **대부분의 라이브러리에서 자동으로 연결 종료**
                - 재연결 필요

                **에러 형식**:
                ```
                ERROR
                error-code: WS001
                session: abc123

                WebSocket 연결 인증에 실패했습니다.
                ```

                **클라이언트 처리**:
                ```javascript
                stompClient.connect(
                  { Authorization: 'Bearer ' + token },
                  onConnect,
                  function onError(error) {
                    console.error('STOMP ERROR:', error);
                    // 연결이 끊어짐 - 재연결 필요함
                    handleReconnect();
                  }
                );
                ```

                #### 2. 에러 채널 메시지 (런타임 에러)
                **발생 시점**: SEND 단계 및 메시지 처리 중
                - 허용되지 않은 경로로 메시지 전송 시도 (SEND)
                - AI 서비스 장애
                - 메시지 저장 실패
                - 기타 비즈니스 로직 에러

                **특징**:
                - 일반 MESSAGE 프레임 (애플리케이션 레벨 에러)
                - 구독한 에러 채널로 전달됨
                - **연결 유지됨** (복구 가능한 에러)
                - 재시도, 에러 알림 등 유연한 처리 가능

                **클라이언트 처리**:
                ```javascript
                // 에러 채널 구독 (필수!)
                stompClient.subscribe('/user/sub/errors/123', (message) => {
                  const error = JSON.parse(message.body);

                  if (error.code === 'WS005') {
                    // 잘못된 전송 경로 - 올바른 경로로 재전송
                    showToast('잘못된 경로입니다. 다시 시도해주세요.');
                  } else if (error.code === 'WS301') {
                    // AI 서비스 일시 장애 - 재시도 버튼 표시
                    showRetryButton();
                  }

                  // 연결은 유지됨 - 계속 사용 가능
                });
                ```

                #### 주요 에러 코드
                | 코드 | 설명 | 처리 방식 |
                |------|------|----------|
                | **WS001** | 인증 실패 | STOMP ERROR (연결 끊김) |
                | **WS004** | 구독 권한 없음 | STOMP ERROR (연결 끊김) |
                | **WS005** | 전송 권한 없음 | 에러 채널 (연결 유지) |
                | **WS201** | 메시지 전송 실패 | 에러 채널 (연결 유지) |
                | **WS301** | AI 서비스 장애 | 에러 채널 (연결 유지) |
                | **WS999** | 서버 내부 오류 | 에러 채널 (연결 유지) |

                ### 연결 예시 (JavaScript/React)
                ```javascript
                // 1. 연결 (JWT 인증 포함)
                const socket = new SockJS('http://localhost:8080/ws-stomp');
                const stompClient = Stomp.over(socket);

                stompClient.connect({
                  Authorization: 'Bearer ' + accessToken  // JWT 토큰 추가 (필수)
                }, (frame) => {
                  console.log('연결됨. 사용자 ID:', frame.headers['user-name']);

                  // 2. 채팅방 1의 에러 메시지 구독 (필수)
                  stompClient.subscribe('/user/sub/errors/1', (message) => {
                    const error = JSON.parse(message.body);
                    console.error('WebSocket 에러:', error);
                    showErrorNotification(error.message);  // ErrorResponse 처리
                  });

                  // 3. 채팅방 1의 채팅 메시지 구독
                  stompClient.subscribe('/user/sub/chat/1', (message) => {
                    const chatMsg = JSON.parse(message.body);
                    displayChatMessage(chatMsg);  // ChatMessageResponse 처리
                  });

                  // 4. 여행 계획 1의 일정 구독
                  stompClient.subscribe('/user/sub/schedule/1', (message) => {
                    const schedule = JSON.parse(message.body);
                    displaySchedule(schedule);  // TripPlanDto 처리
                  });

                  // 5. 채팅방 1로 메시지 전송
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

  @Bean
  public OpenApiCustomizer webSocketSchemaCustomizer() {
    return openApi -> {
      // WebSocket 메시지 스키마 등록
      var tripPlanProgressSchemas =
          ModelConverters.getInstance().read(TripPlanProgressMessage.class);
      var chatStreamSchemas = ModelConverters.getInstance().read(ChatStreamDto.class);
      var errorSchemas = ModelConverters.getInstance().read(ErrorResponse.class);

      openApi.getComponents().getSchemas().putAll(tripPlanProgressSchemas);
      openApi.getComponents().getSchemas().putAll(chatStreamSchemas);
      openApi.getComponents().getSchemas().putAll(errorSchemas);

      // API 응답 스키마 등록
      var tripPlanResponseSchemas = ModelConverters.getInstance().read(TripPlanResponse.class);
      var tripPlanDetailSchemas = ModelConverters.getInstance().read(TripPlanDetailResponse.class);
      var dailyPlanSchemas = ModelConverters.getInstance().read(DailyPlanResponse.class);
      var channelIdSchemas = ModelConverters.getInstance().read(ChannelIdResponse.class);
      var chatMessageSchemas = ModelConverters.getInstance().read(ChatMessageResponse.class);

      openApi.getComponents().getSchemas().putAll(tripPlanResponseSchemas);
      openApi.getComponents().getSchemas().putAll(tripPlanDetailSchemas);
      openApi.getComponents().getSchemas().putAll(dailyPlanSchemas);
      openApi.getComponents().getSchemas().putAll(channelIdSchemas);
      openApi.getComponents().getSchemas().putAll(chatMessageSchemas);
    };
  }
}
