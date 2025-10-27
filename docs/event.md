## 📋 이벤트 정의

| 이벤트 | 발행 시점 | 발행자 | 수신자 |
| --- | --- | --- | --- |
| `TripPlanGenerationRequestedEvent` | 사용자가 일정 생성 요청 시 | Chat 서비스 | AI 리스너 |
| `DailyPlanGeneratedEvent` | 일차별 일정 완성 시 | AI 서비스 | Chat 리스너 |
| `TripGenerationCompletedEvent` | 전체 일정 완성 시 | AI 서비스 | Chat 리스너 |
| `TripGenerationErrorEvent` | 일정 생성 오류 발생 시 | AI 서비스 | Chat 리스너 |
| `StreamCancellationRequestedEvent` | WebSocket 연결 해제 시 | Chat 서비스 | AI 리스너 |

---

## 📦 이벤트 데이터

- **TripPlanGenerationRequestedEvent**
    - `sessionId` (String): WebSocket 세션 ID
    - `chatRoomId` (Long): 채팅방 ID
    - `tripPlanRequest` (TripPlanRequest): 여행 일정 생성 요청
        - `tripThemeId` (Long): 여행 테마 ID
        - `title` (String): 여행 제목
        - `startDate` (LocalDate): 여행 시작일
        - `endDate` (LocalDate): 여행 종료일
        - `region` (String): 선호 도시 (서울, 경기, 제주 등)
        - `transportation` (Transportation): 선호 이동 방식 (NONE, WALKING, PUBLIC_TRANSIT, CAR)
        - `budget` (String): 예산 수준
        - `peopleCount` (Integer): 여행 인원 수
        - `startLocation` (Location): 출발지 정보
            - `name` (String): 장소 이름
            - `address` (String): 주소
            - `point` (Point): 좌표
        - `endLocation` (Location): 도착지 정보
            - `name` (String): 장소 이름
            - `address` (String): 주소
            - `point` (Point): 좌표
- **DailyPlanGeneratedEvent**
    - `sessionId` (String): WebSocket 세션 ID
    - `chatRoomId` (Long): 채팅방 ID
    - `tripPlanId` (Long): 여행 계획 ID (TripPlan 엔티티)
    - `dayNumber` (int): 일차 번호 (1, 2, 3, …)
    - `dailyPlanData` (DailyPlanData): 생성된 일정 데이터
        - `date` (LocalDate): 일정 날짜
        - `scheduledPlaces` (List<ScheduledPlaceData>): 방문 장소 목록
            - `placeId` (Long): 방문할 장소 ID
            - `visitOrder` (Integer): 방문 순서
            - `category` (ScheduledCategory): 일정 분류 (MORNING, LUNCH, AFTERNOON, CAFE, DINNER, EVENING)
            - `plannedTime` (LocalTime): 계획된 방문 시간
            - `travelSegment` (TravelSegment): 이동 정보
                - `travelTime` (Integer): 예상 이동 시간 (분)
                - `transportation` (Transportation): 이동 수단
- **TripGenerationCompletedEvent**
    - `sessionId` (String): WebSocket 세션 ID
    - `chatRoomId` (Long): 채팅방 ID
    - `tripPlanId` (Long): 여행 계획 ID (TripPlan 엔티티)
    - `totalDays` (int): 전체 여행 일수 (박수 기준)
- **TripGenerationErrorEvent**
    - `sessionId` (String): WebSocket 세션 ID
    - `chatRoomId` (Long): 채팅방 ID
    - `errorMessage` (String): 에러 메시지
    - `errorCode` (ErrorCode): 에러 코드
- **StreamCancellationRequestedEvent**
    - `sessionId` (String): WebSocket 세션 ID

---

## 🔄 전체 시퀀스 다이어그램

전체 흐름을 한눈에 파악할 수 있는 다이어그램입니다.

```mermaid
sequenceDiagram
    participant User as 사용자
    participant Client as 클라이언트
    participant Controller as ChatMessageController
    participant ChatService as Chat 서비스
    participant AIListener as AI 리스너
    participant AIService as AI 서비스
    participant LLM as LLM Stream
    participant DB as Database
    participant ChatListener as Chat 리스너

    User->>Client: "제주도 3박4일 일정 짜줘"
    Client->>Controller: STOMP 메시지
    Controller->>ChatService: sendMessage()

    Note over ChatService: TripPlanGenerationRequestedEvent 발행
    ChatService->>AIListener: 이벤트 전달

    AIListener->>AIService: generateTripPlan()
    AIService->>DB: TripPlan 저장 (isCompleted=false)
    DB-->>AIService: tripPlanId 반환

    AIService->>LLM: Stream 시작

    loop 각 일차별 (1일차, 2일차, 3일차...)
        LLM-->>AIService: N일차 데이터 완료
        AIService->>DB: DailyPlan + ScheduledPlace 즉시 저장

        Note over AIService: DailyPlanGeneratedEvent 발행
        AIService->>ChatListener: 이벤트 전달

        ChatListener->>Client: WebSocket 전송
        Client->>User: N일차 일정 표시
    end

    LLM-->>AIService: Stream 완료
    AIService->>DB: TripPlan.isCompleted = true 업데이트

    Note over AIService: TripGenerationCompletedEvent 발행
    AIService->>ChatListener: 이벤트 전달

    ChatListener->>Client: 완료 메시지 전송
    Client->>User: "3박 4일 일정 완료" 표시
```

---

## 📤 이벤트 발행 상세

각 이벤트가 어떻게 발행되는지 상세한 다이어그램입니다.

### 1️⃣ TripPlanGenerationRequestedEvent 발행

사용자 요청을 받아 Chat 서비스에서 이벤트를 발행합니다.

```mermaid
sequenceDiagram
    participant User as 사용자
    participant Client as 클라이언트
    participant Controller as ChatMessageController
    participant ChatService as Chat 서비스

    User->>Client: "제주도 3박4일 일정 짜줘"

    Client->>Controller: STOMP 메시지 전송<br/>/pub/chat/{chatRoomId}/message
    Note over Client,Controller: ChatMessageRequest

    Controller->>ChatService: sendMessage(chatRoomId, request, sessionId)

    ChatService->>ChatService: TripPlanRequest 생성
    Note over ChatService: 사용자 메시지 파싱<br/>여행 정보 추출

    ChatService->>ChatService: eventPublisher.publishEvent()
    Note over ChatService: new TripPlanGenerationRequestedEvent(<br/>sessionId, chatRoomId, tripPlanRequest)

    Note over ChatService: 이벤트 발행 완료<br/>AI 리스너가 수신 대기 중
```

---

### 2️⃣ DailyPlanGeneratedEvent 발행

LLM이 일차별 데이터를 생성하면 즉시 DB에 저장하고 이벤트를 발행합니다.

```mermaid
sequenceDiagram
    participant LLM as LLM Stream
    participant AIService as AI 서비스
    participant DB as Database

    Note over LLM: N일차 데이터 스트리밍 완료

    LLM->>AIService: N일차 응답 청크

    AIService->>AIService: bufferUntil()로 일차별 파싱
    Note over AIService: DailyPlanData 생성

    AIService->>DB: DailyPlan 엔티티 저장
    Note over AIService,DB: tripPlanId, date

    DB-->>AIService: dailyPlanId 반환

    AIService->>DB: ScheduledPlace 엔티티 저장 (여러 개)
    Note over AIService,DB: dailyPlanId, placeId, visitOrder,<br/>category, plannedTime, travelSegment

    AIService->>AIService: eventPublisher.publishEvent()
    Note over AIService: new DailyPlanGeneratedEvent(<br/>sessionId, chatRoomId, tripPlanId,<br/>dayNumber, dailyPlanData)

    Note over AIService: 이벤트 발행 완료<br/>Chat 리스너가 수신 대기 중
```

---

### 3️⃣ TripGenerationCompletedEvent 발행

모든 일차 스트리밍이 완료되면 TripPlan을 완료 상태로 업데이트하고 이벤트를 발행합니다.

```mermaid
sequenceDiagram
    participant LLM as LLM Stream
    participant AIService as AI 서비스
    participant DB as Database

    Note over LLM: 모든 일차 스트리밍 완료

    LLM->>AIService: Flux.onComplete()

    AIService->>DB: TripPlan.isCompleted = true 업데이트
    Note over AIService,DB: UPDATE trip_plan<br/>SET is_completed = true<br/>WHERE trip_plan_id = ?

    AIService->>AIService: eventPublisher.publishEvent()
    Note over AIService: new TripGenerationCompletedEvent(<br/>sessionId, chatRoomId,<br/>tripPlanId, totalDays)

    Note over AIService: 이벤트 발행 완료<br/>Chat 리스너가 수신 대기 중
```

---

### 4️⃣ TripGenerationErrorEvent 발행

스트리밍 중 오류가 발생하면 에러 이벤트를 발행합니다.

```mermaid
sequenceDiagram
    participant LLM as LLM Stream
    participant AIService as AI 서비스

    Note over LLM: Stream 처리 중 오류 발생<br/>(API 타임아웃, 토큰 한도 등)

    LLM->>AIService: Flux.onError(Exception)

    AIService->>AIService: 에러 로깅 및 분석
    Note over AIService: log.error("일정 생성 실패", exception)<br/>에러 타입 분류

    AIService->>AIService: eventPublisher.publishEvent()
    Note over AIService: new TripGenerationErrorEvent(<br/>sessionId, chatRoomId,<br/>errorMessage, errorCode)

    Note over AIService: 이벤트 발행 완료<br/>Chat 리스너가 수신 대기 중
```

---

## 📥 이벤트 수신 및 처리 상세

각 이벤트를 수신한 후 어떻게 처리하는지 상세한 다이어그램입니다.

### 1️⃣ TripPlanGenerationRequestedEvent 수신 및 처리

AI 리스너가 이벤트를 수신하여 여행 일정 생성을 시작합니다.

```mermaid
sequenceDiagram
    participant ChatService as Chat 서비스
    participant AIListener as AI 리스너
    participant AIService as AI 서비스
    participant DB as Database
    participant LLM as LLM Stream

    Note over ChatService: TripPlanGenerationRequestedEvent 발행

    ChatService->>AIListener: 이벤트 전달

    AIListener->>AIListener: @EventListener 트리거
    Note over AIListener: handleTripPlanGenerationRequested(<br/>TripPlanGenerationRequestedEvent event)

    AIListener->>AIListener: 이벤트 데이터 추출
    Note over AIListener: sessionId, chatRoomId, tripPlanRequest

    AIListener->>AIService: generateTripPlan(request, sessionId, chatRoomId)

    AIService->>DB: TripPlan 엔티티 저장
    Note over AIService,DB: isCompleted = false<br/>여행 계획 생성 시작

    DB-->>AIService: tripPlanId 반환

    AIService->>LLM: chatClient.stream().chatResponse()
    Note over AIService,LLM: Flux<ChatResponse> 스트리밍 시작<br/>RAG Advisor + Memory Advisor 실행

    Note over LLM: 비동기 스트리밍 시작<br/>일차별 데이터 생성 중...
```

---

### 2️⃣ DailyPlanGeneratedEvent 수신 및 처리

Chat 리스너가 이벤트를 수신하여 클라이언트에게 일차별 일정을 전송합니다.

```mermaid
sequenceDiagram
    participant AIService as AI 서비스
    participant ChatListener as Chat 리스너
    participant Client as 클라이언트
    participant User as 사용자

    Note over AIService: DailyPlanGeneratedEvent 발행

    AIService->>ChatListener: 이벤트 전달

    ChatListener->>ChatListener: @EventListener 트리거
    Note over ChatListener: handleDailyPlanGenerated(<br/>DailyPlanGeneratedEvent event)

    ChatListener->>ChatListener: 이벤트 데이터 추출
    Note over ChatListener: sessionId, chatRoomId,<br/>dayNumber, dailyPlanData

    ChatListener->>ChatListener: ChatMessageResponse 생성
    Note over ChatListener: sender = AI<br/>messageContentType = TRIP_PLAN<br/>content = N일차 일정 포맷팅

    ChatListener->>Client: SimpMessagingTemplate.convertAndSendToUser()
    Note over ChatListener,Client: /user/{sessionId}/sub/chat/{chatRoomId}<br/>일차별 일정 JSON 전송

    Client->>Client: UI 업데이트

    Client->>User: N일차 일정 화면에 표시
```

---

### 3️⃣ TripGenerationCompletedEvent 수신 및 처리

Chat 리스너가 완료 이벤트를 수신하여 완료 메시지를 전송합니다.

```mermaid
sequenceDiagram
    participant AIService as AI 서비스
    participant ChatListener as Chat 리스너
    participant Client as 클라이언트
    participant User as 사용자

    Note over AIService: TripGenerationCompletedEvent 발행

    AIService->>ChatListener: 이벤트 전달

    ChatListener->>ChatListener: @EventListener 트리거
    Note over ChatListener: handleTripGenerationCompleted(<br/>TripGenerationCompletedEvent event)

    ChatListener->>ChatListener: 이벤트 데이터 추출
    Note over ChatListener: sessionId, chatRoomId,<br/>tripPlanId, totalDays

    ChatListener->>ChatListener: ChatMessageResponse 생성
    Note over ChatListener: sender = AI<br/>messageContentType = TEXT<br/>content = "{totalDays}박 {totalDays+1}일<br/>일정이 완료되었습니다!"

    ChatListener->>Client: SimpMessagingTemplate.convertAndSendToUser()
    Note over ChatListener,Client: /user/{sessionId}/sub/chat/{chatRoomId}

    Client->>User: 완료 메시지 표시
    Note over Client,User: "3박 4일 일정이 완료되었습니다!"
```

---

### 4️⃣ TripGenerationErrorEvent 수신 및 처리

Chat 리스너가 에러 이벤트를 수신하여 에러 메시지를 전송합니다.

```mermaid
sequenceDiagram
    participant AIService as AI 서비스
    participant ChatListener as Chat 리스너
    participant Client as 클라이언트
    participant User as 사용자

    Note over AIService: TripGenerationErrorEvent 발행

    AIService->>ChatListener: 이벤트 전달

    ChatListener->>ChatListener: @EventListener 트리거
    Note over ChatListener: handleTripGenerationError(<br/>TripGenerationErrorEvent event)

    ChatListener->>ChatListener: 이벤트 데이터 추출
    Note over ChatListener: sessionId, chatRoomId,<br/>errorMessage, errorCode

    ChatListener->>ChatListener: ChatMessageResponse 생성
    Note over ChatListener: sender = AI<br/>messageContentType = TEXT<br/>content = "일정 생성에 실패했습니다.<br/>다시 시도해주세요."

    ChatListener->>Client: SimpMessagingTemplate.convertAndSendToUser()
    Note over ChatListener,Client: /user/{sessionId}/sub/chat/{chatRoomId}

    Client->>User: 에러 메시지 표시
    Note over Client,User: 재시도 버튼 제공
```

---

## 🔌 WebSocket 연결 해제 시 처리

### 전체 흐름

사용자가 연결을 끊으면 진행 중인 스트리밍을 취소합니다.

```mermaid
sequenceDiagram
    participant User as 사용자
    participant Client as 클라이언트
    participant WSListener as WebSocket 연결 리스너
    participant ChatService as Chat 서비스
    participant AIListener as AI 리스너
    participant AIService as AI 서비스

    User->>Client: 브라우저 닫기/페이지 이동
    Client->>WSListener: WebSocket 연결 해제

    WSListener->>WSListener: @EventListener(SessionDisconnectEvent)

    WSListener->>ChatService: handleDisconnect(sessionId)

    ChatService->>ChatService: eventPublisher.publishEvent()
    Note over ChatService: StreamCancellationRequestedEvent

    ChatService->>AIListener: 이벤트 전달

    AIListener->>AIListener: @EventListener 트리거

    AIListener->>AIService: cancelStream(sessionId)

    AIService->>AIService: activeStreams.remove(sessionId)
    Note over AIService: Disposable.dispose() 호출<br/>Flux 구독 취소

    Note over AIService: LLM HTTP 요청 중단<br/>스트리밍 중단
```

---

### 📤 StreamCancellationRequestedEvent 발행

WebSocket 연결이 해제되면 Chat 서비스가 Stream 취소 이벤트를 발행합니다.

```mermaid
sequenceDiagram
    participant Client as 클라이언트
    participant WSListener as WebSocket 연결 리스너
    participant ChatService as Chat 서비스

    Client->>WSListener: WebSocket 연결 해제

    WSListener->>WSListener: @EventListener 트리거
    Note over WSListener: handleSessionDisconnect(<br/>SessionDisconnectEvent event)

    WSListener->>WSListener: sessionId 추출

    WSListener->>ChatService: handleDisconnect(sessionId)

    ChatService->>ChatService: eventPublisher.publishEvent()
    Note over ChatService: new StreamCancellationRequestedEvent(<br/>sessionId)

    Note over ChatService: 이벤트 발행 완료<br/>AI 리스너가 수신 대기 중
```

---

### 📥 StreamCancellationRequestedEvent 수신 및 처리

AI 리스너가 이벤트를 수신하여 진행 중인 Stream을 취소합니다.

```mermaid
sequenceDiagram
    participant ChatService as Chat 서비스
    participant AIListener as AI 리스너
    participant AIService as AI 서비스
    participant LLM as LLM Stream

    Note over ChatService: StreamCancellationRequestedEvent 발행

    ChatService->>AIListener: 이벤트 전달

    AIListener->>AIListener: @EventListener 트리거
    Note over AIListener: handleStreamCancellation(<br/>StreamCancellationRequestedEvent event)

    AIListener->>AIListener: sessionId 추출

    AIListener->>AIService: cancelStream(sessionId)

    AIService->>AIService: activeStreams.get(sessionId)
    Note over AIService: Map<String, Disposable>

    alt 진행 중인 Stream이 있는 경우
        AIService->>AIService: disposable.dispose()
        Note over AIService: Flux 구독 취소

        AIService->>LLM: HTTP 요청 중단

        Note over LLM: 스트리밍 중단

        AIService->>AIService: activeStreams.remove(sessionId)
    else 진행 중인 Stream 없음
        Note over AIService: 아무 작업 없음
    end
```