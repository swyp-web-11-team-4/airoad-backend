# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 3.5.6 + Java 17 기반의 **여행 일정 추천 AI 서비스** 백엔드 프로젝트입니다.
모놀리식 아키텍처로 구현되며, 도메인별로 계층화된 패키지 구조를 따릅니다.

### Service Description
AI 기술을 활용하여 사용자의 선호도와 여행 조건에 맞는 최적화된 여행 일정을 자동으로 생성하고, 대화형 인터페이스를 통해 쉽게 수정할 수 있는 서비스를 제공합니다.

**핵심 가치**:
- 자동화된 일정 생성: 수동으로 일정을 짜는 시간과 노력 절감
- 최적화된 동선: 이동 시간을 고려한 효율적인 경로 제안
- 유연한 수정: 챗봇 대화 또는 직접 편집을 통한 손쉬운 일정 조정
- 개인화: 테마와 선호도 기반 맞춤형 추천

**주요 기술**:
- PostgreSQL + pgvector (RAG 시스템)
- Redis (캐싱)
- 공공 관광 데이터 API, Naver 거리 측정 API
- LLM API (OpenAI/Claude/Hyperclova 등)

## Essential Commands

### Build & Run
```shell
./gradlew build           # 빌드 및 테스트 실행
./gradlew bootRun         # 애플리케이션 실행
./gradlew test            # 테스트만 실행
```

### Code Quality
```shell
./gradlew spotlessApply   # 코드 포맷팅 적용 (커밋 전 필수!)
./gradlew spotlessCheck   # 코드 스타일 검사
./gradlew jacocoTestReport  # 커버리지 리포트 생성 (build/reports/jacoco/test/html/index.html)
./gradlew check           # 전체 품질 검사 (커버리지 + 포맷팅)
```

## Architecture

**의존성 방향**: `presentation` → `application` → `domain` ← `infrastructure`

### Package Structure
```
com.swygbro.airoad.backend/
├── common/                  # 공통 모듈
│   ├── config/             # 설정 클래스 (Swagger, JPA Auditing)
│   ├── domain/
│   │   ├── dto/           # 공통 DTO (CommonResponse, ErrorResponse, PageResponse)
│   │   └── entity/        # BaseEntity
│   ├── exception/          # 예외 처리 (ErrorCode, BusinessException, CommonErrorCode)
│   └── presentation/       # GlobalExceptionHandler
│
└── <domain>/               # 도메인별 패키지
    ├── presentation/       # 컨트롤러 (REST API 엔드포인트)
    ├── application/        # 유스케이스 인터페이스 및 서비스 클래스
    ├── domain/
    │   ├── dto/           # Request/Response DTO
    │   └── entity/        # 도메인 엔티티
    ├── exception/          # 도메인별 ErrorCode enum
    └── infrastructure/     # Repository
```

## Key Patterns

### 1. Use Case Interface Pattern
각 도메인의 `application` 패키지에 `*UseCase` 인터페이스를 정의하고, `*Service` 클래스에서 구현합니다.

```java
// application/ExampleUseCase.java
public interface ExampleUseCase {
  ExampleResponse getExampleById(Long id);
}

// application/ExampleService.java
@Service
public class ExampleService implements ExampleUseCase {
  @Override
  public ExampleResponse getExampleById(Long id) { ... }
}
```

### 2. Base Entity Pattern
모든 엔티티는 `BaseEntity`를 상속받아 생성일시/수정일시를 자동 관리합니다.

```java
@Entity
public class Example extends BaseEntity {
  // id, createdAt, updatedAt 필드는 BaseEntity에서 상속됨
  // id는 IDENTITY 전략으로 자동 생성됨
}
```

**중요**: JPA Auditing을 사용하므로 `@EnableJpaAuditing`이 `JpaConfig`에 설정되어 있어야 합니다.
- `@CreatedDate`: 엔티티 생성 시 자동으로 현재 시각 저장
- `@LastModifiedDate`: 엔티티 수정 시 자동으로 현재 시각 업데이트

### 3. Structured Exception Handling
`ErrorCode` 인터페이스와 `BusinessException`을 통해 일관된 예외 처리를 제공합니다.

- **ErrorCode**: 에러 코드, HTTP 상태, 메시지를 정의하는 인터페이스
- **CommonErrorCode**: 공통 에러 코드 enum (INTERNAL_SERVER_ERROR, INVALID_INPUT_VALUE 등)
- **도메인별 ErrorCode**: 각 도메인의 `exception` 패키지에 `ErrorCode` 인터페이스를 구현한 `<Domain>ErrorCode` enum 정의
- **BusinessException**: 비즈니스 로직 예외 처리용 공통 클래스
- **GlobalExceptionHandler**: 모든 예외를 `CommonResponse<ErrorResponse>`로 변환

```java
// 도메인별 ErrorCode 정의
public enum ExampleErrorCode implements ErrorCode {
  EXAMPLE_NOT_FOUND("EXAMPLE001", HttpStatus.NOT_FOUND, "Example을 찾을 수 없습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String defaultMessage;
}

// 서비스에서 예외 발생
throw new BusinessException(ExampleErrorCode.EXAMPLE_NOT_FOUND);
```

### 4. Common Response Pattern
모든 API 응답은 `CommonResponse<T>` 래퍼를 사용합니다.

```java
// 성공 응답
return CommonResponse.success(HttpStatus.OK, data);

// 에러 응답 (GlobalExceptionHandler가 자동 처리)
return CommonResponse.error(HttpStatus.BAD_REQUEST, errorResponse);
```

## Testing Standards

- **Framework**: JUnit 5 + Mockito + AssertJ
- **Structure**: `@Nested` 클래스로 메서드별 테스트 그룹화
- **Naming**: BDD 스타일 (given-when-then)
- **Profile**: `@ActiveProfiles("test")` 사용
- **Coverage**: 전체 60% 이상 (LINE/BRANCH), 개별 클래스 60% 이상 (LINE)

### Service Test Example
```java
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ExampleServiceTest {

  @Mock
  private ExampleRepository repository;

  @InjectMocks
  private ExampleService service;

  @Nested
  @DisplayName("getExampleById 메서드는")
  class GetExampleById {

    @Test
    @DisplayName("ID로 Example을 조회하여 반환한다")
    void shouldReturnExampleById() {
      // given
      given(repository.findById(1L)).willReturn(Optional.of(example));

      // when
      ExampleResponse response = service.getExampleById(1L);

      // then
      assertThat(response).isNotNull();
      verify(repository).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 시 예외를 발생시킨다")
    void shouldThrowExceptionWhenNotFound() {
      // given
      given(repository.findById(1L)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> service.getExampleById(1L))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ExampleErrorCode.EXAMPLE_NOT_FOUND);
    }
  }
}
```

### Controller Test Example
```java
@WebMvcTest(ExampleController.class)
@ActiveProfiles("test")
class ExampleControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ExampleUseCase exampleUseCase;

  @Nested
  @DisplayName("GET /api/examples/{id}")
  class GetExampleById {

    @Test
    @WithMockUser
    @DisplayName("Example을 조회하여 반환한다")
    void shouldReturnExample() throws Exception {
      // given
      ExampleResponse response = ExampleResponse.builder()
          .id(1L)
          .name("Test Example")
          .build();
      given(exampleUseCase.getExampleById(1L)).willReturn(response);

      // when & then
      mockMvc.perform(get("/api/examples/{id}", 1L)
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.id").value(1L))
          .andExpect(jsonPath("$.data.name").value("Test Example"));

      verify(exampleUseCase).getExampleById(1L);
    }
  }
}
```

## Code Quality Standards

### Coverage Exclusions
다음 항목은 커버리지 측정에서 제외됩니다:
- `**/config/**`, `**/domain/**`, `**/exception/**`
- `*Application*`, `*Config*`, `*Dto*`, `*Entity*`, `*Exception*`, `*ErrorCode*`, `*Handler*`

### Spotless (Code Formatting)
- **커밋 전 필수**: `./gradlew spotlessApply` 실행
- Google Java Format 사용
- CI/CD에서 자동 검사

## Commit Conventions

Conventional Commits + Semantic Release 사용:

```
<type>: <subject>
```

**주요 타입**:
- `feat`: 새로운 기능 추가
- `fix`: 버그 수정
- `design`: CSS 등 사용자 UI 디자인 변경
- `docs`: 문서 변경 (README, 가이드, 주석 등)
- `chore`: 빌드 설정, 의존성, 환경 설정 변경
- `refactor`: 코드 리팩토링 혹은 성능 개선
- `test`: 테스트 코드 추가 또는 수정
- `comment`: 필요한 주석 추가 및 수정
- `style`: 코드 포맷팅, 세미콜론 등 (기능 변경 없음)
- `remove`: 파일, 기능, 의존성 제거
- `ci`: CI 설정 변경
- `cd`: CD 설정 변경

## CI/CD Pipeline

1. **Test & Build**: 테스트 실행 및 커버리지 생성, PR에 자동 리포트
2. **SonarQube**: 코드 품질 분석 (main/develop PR 및 push 시)
3. **Semantic Release**: main 브랜치 푸시 시 자동 버전 관리, CHANGELOG 생성, gradle.properties 버전 업데이트
4. **Docker Build & Push**: 릴리즈 생성 시 ghcr.io에 이미지 푸시

## Development Notes

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **Test DB**: H2 (in-memory)
- **Dependency Management**: `gradle/libs.versions.toml`에서 중앙 관리
- **Custom Gradle Plugins**: `buildSrc/src/main/kotlin/plugin/`에 정의 (coverage, spotless, sonar)
- **Test Profile**: `@ActiveProfiles("test")` 사용하여 H2 데이터베이스로 테스트 실행

## Security & Authentication

### OAuth2 + JWT Pattern
프로젝트는 OAuth2 소셜 로그인과 JWT 토큰 기반 인증을 사용합니다.

**인증 흐름**:
1. OAuth2 로그인 → Google OAuth2 인증
2. `OAuthLoginSuccessHandler`에서 인증 성공 처리
3. JWT 액세스 토큰 + 리프레시 토큰 발급
4. `JwtAuthenticationFilter`에서 요청마다 토큰 검증
5. `UserPrincipal`로 인증된 사용자 정보 관리

**주요 컴포넌트**:
- `JwtTokenProvider`: JWT 토큰 생성/검증
- `JwtAuthenticationFilter`: 요청마다 토큰 검증 및 SecurityContext 설정
- `UserPrincipal`: Spring Security UserDetails 구현
- `RefreshToken`: Redis에 저장되는 리프레시 토큰 엔티티
- `SecurityConfig`: Spring Security 설정 (CORS, OAuth2, JWT 필터 체인)

**토큰 갱신**:
```java
// POST /api/auth/refresh
// RefreshToken으로 새로운 AccessToken 발급
```

### 테스트에서 인증 처리
```java
@WithMockUser(username = "test@example.com", roles = "USER")
@Test
void authenticatedTest() {
  // SecurityContext에 인증된 사용자가 자동 설정됨
}
```

## WebSocket & Chat

### STOMP over WebSocket
실시간 채팅 기능을 위해 STOMP 프로토콜을 사용합니다.

**연결 설정**:
- Endpoint: `/ws` (SockJS fallback 지원)
- Message Broker: `/topic` (구독), `/app` (메시지 전송)
- Redis Pub/Sub을 통한 메시지 브로커

**주요 구성**:
- `WebSocketConfig`: STOMP 설정 및 인터셉터 등록
- `StompPrincipalHandshakeHandler`: WebSocket 연결 시 사용자 인증
- `WebSocketPayloadTypeInterceptor`: 메시지 타입 검증
- `WebSocketEventListener`: 연결/해제 이벤트 처리

**메시지 흐름**:
```
Client → /app/chat.sendMessage → ChatMessageController
       → /topic/chatroom/{roomId} → Subscribed Clients
```

## Domain Relationships

프로젝트는 다음 도메인들로 구성되며, 각 도메인은 명확한 책임을 가집니다:

```
member (회원)
  ↓ 1:N
auth (인증) - OAuth2UserInfo, RefreshToken
  ↓
trip (여행 일정)
  ↓ 1:N
  ├─ TripPlan (여행 계획)
  │   ↓ 1:N
  │   └─ DailyPlan (일별 계획)
  │       ↓ 1:N
  │       └─ ScheduledPlace (방문 장소)
  └─ chat (채팅방)
      ↓ 1:N
      └─ AiConversation (AI 대화 이력)

content (관광지 정보) - RAG 시스템에서 참조
```

**도메인 간 의존성**:
- `member` → `auth`: 회원 정보로 인증 토큰 생성
- `member` → `trip`: 회원의 여행 일정 관리
- `trip` → `content`: 여행 일정에 관광지 정보 참조 (RAG 검색)
- `trip` → `chat`: 여행 일정 수정을 위한 채팅방 연결

## Additional Patterns and Features

### 5. Event-Driven Listener Architecture
프로젝트는 도메인 간 느슨한 결합을 위해 Spring Application Events를 활용합니다.

**핵심 원칙: 리스너는 소속 도메인의 `presentation/listener/` 패키지에 위치**

```
ai/presentation/listener/
├── TripPlanGenerationListener    # AI 서비스 호출 및 도메인 이벤트 발행
└── AiResponseListener             # AI 응답 처리

websocket/presentation/listener/
└── TripPlanProgressListener       # WebSocket 메시지 전송 전담
```

**이벤트 흐름 예시**:
```java
// 1. Controller가 도메인 이벤트 발행
eventPublisher.publishEvent(new TripPlanGenerationRequestedEvent(...));

// 2. AI 리스너가 처리 후 결과 이벤트 발행
@EventListener
public void handleTripPlanGenerationRequested(TripPlanGenerationRequestedEvent event) {
  // AI 서비스 호출
  eventPublisher.publishEvent(new DailyPlanGeneratedEvent(...));
}

// 3. WebSocket 리스너가 클라이언트에 전송
@EventListener
public void handleDailyPlanGenerated(DailyPlanGeneratedEvent event) {
  messagingTemplate.convertAndSend(destination, message);
}
```

**리스너 설계 가이드**:
- ✅ 각 리스너는 자기 도메인의 책임만 수행 (단일 책임 원칙)
- ✅ 리스너는 이벤트를 소비하고 필요시 새 이벤트 발행
- ✅ `ApplicationEventPublisher` 주입으로 도메인 이벤트 발행
- ❌ 리스너에서 직접 다른 도메인의 서비스 호출 금지
- ❌ WebSocket 전송 로직을 AI 도메인에 포함 금지

### 6. Spring AI Integration
LLM 통합을 위해 Spring AI 프레임워크를 사용합니다.

```java
// Spring AI 의존성
implementation platform('org.springframework.ai:spring-ai-bom')
implementation 'org.springframework.ai:spring-ai-openai'
```

**구현 시 고려사항**:
- `ChatClient` 또는 `ChatModel` 인터페이스 사용
- Prompt 템플릿과 변수 바인딩 활용
- RAG 구현 시 `VectorStore`와 `EmbeddingModel` 조합

### 7. Authentication & Security Pattern
JWT 기반 인증과 OAuth2 소셜 로그인을 결합한 보안 아키텍처입니다.

**인증 흐름**:
1. OAuth2 로그인 성공 → `OAuthLoginSuccessHandler` 호출
2. JWT Access/Refresh Token 쌍 생성 및 반환
3. 이후 요청마다 `JwtTokenProvider`로 토큰 검증
4. Refresh Token으로 Access Token 재발급 (`AuthService.reissue`)

**WebSocket 인증**:
- `JwtWebSocketInterceptor`가 연결 시 토큰 검증
- STOMP 헤더 또는 쿼리 파라미터로 토큰 전달

## Custom Slash Commands

프로젝트는 개발 워크플로우를 위한 커스텀 슬래시 명령어를 제공합니다 (`.claude/commands/`):

- `/task [description] or [issue-number]`: GitHub 이슈 번호 또는 설명으로 완전한 기능 구현 (API 설계 → 구현 → 테스트 → 품질 검사)
- `/commit [additional-instructions]`: 프로젝트 커밋 컨벤션에 맞춰 커밋 생성
- `/pr-create [issue-number]`: 이슈와 연결된 PR 생성

**사용 예시**:
```bash
/task 27                           # GitHub 이슈 #27의 요구사항 구현
/task 사용자 프로필 조회 API 구현     # 텍스트 설명으로 구현
/commit                            # 변경사항 분석 후 커밋
/pr-create 27                      # 이슈 #27에 대한 PR 생성
```

## Domain-Specific Implementation Guidance

### Trip Planning Domain
여행 일정 생성 및 수정 기능 구현 시 고려사항:

**주요 기능**:
1. **여행 정보 입력**: 도시, 날짜, 기간, 인원, 테마 (체험/액티비티, 문화/역사, 자연/힐링, 맛집 투어, 쇼핑, 복합)
2. **AI 일정 자동 생성**: RAG 기반 관광지 검색 + Naver API 거리 계산 + LLM 일정 생성
3. **일정 수정**:
   - 챗봇 대화 수정: 자연어 요청 → Chat Memory + RAG 재검색 → 부분 수정
   - 직접 편집: UI 변경 → Chat Memory 이력 저장 → 변경 구간만 재계산
4. **일정 확정 및 저장**: 최종 일정 DB 저장 + Chat Memory 전체 이력 저장

**시스템 설계 패턴**:
- **RAG 시스템**: Vector DB (pgvector)에 관광지 설명 임베딩 저장, 코사인 유사도 기반 Top-K 검색
- **Chat Memory**: 직접 수정 및 대화 이력 JSON 형태로 저장, LLM 컨텍스트에 최근 N개 포함
- **Naver 거리 측정 API**: 초기 생성 시 N×N 거리 매트릭스, 수정 시 변경 구간만 재계산
- **토큰 최적화**: 전체 재생성 대신 부분 수정, Chat Memory로 컨텍스트 관리
- **실시간 스트리밍**: WebSocket + Spring Event로 LLM 응답을 청크 단위로 클라이언트에 전달
