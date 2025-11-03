# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 3.5.6 + Java 17 based AI-powered travel itinerary recommendation service backend. The service generates personalized travel plans using AI (Spring AI with Naver ClovaX) and RAG (pgvector), optimizes routes considering travel time, and provides conversational editing through WebSocket-based chat.

## Essential Commands

### Build and Run
```shell
# Build project (includes tests, coverage, and formatting check)
./gradlew build

# Run application locally
./gradlew bootRun

# Run tests only
./gradlew test

# Run a single test class
./gradlew test --tests ClassName

# Run a single test method
./gradlew test --tests ClassName.methodName
```

### Code Quality (MANDATORY before commits)
```shell
# Apply code formatting (REQUIRED before committing!)
./gradlew spotlessApply

# Check code style without applying
./gradlew spotlessCheck

# Generate coverage report (output: build/reports/jacoco/test/html/index.html)
./gradlew jacocoTestReport

# Verify coverage thresholds (60% for LINE/BRANCH)
./gradlew jacocoTestCoverageVerification

# Run all quality checks (coverage + formatting)
./gradlew check
```

## Architecture

### Package Structure (Domain-Driven Layered Architecture)

```
com.swygbro.airoad.backend/
├── common/                      # Cross-cutting concerns
│   ├── config/                  # Application-wide configurations
│   ├── domain/
│   │   ├── dto/                # Common response types (CommonResponse, PageResponse, etc.)
│   │   ├── entity/             # BaseEntity (auto-managed id, createdAt, updatedAt)
│   │   └── event/              # Common domain events
│   ├── exception/               # Exception handling infrastructure
│   │   ├── ErrorCode           # Interface for error codes
│   │   ├── BusinessException   # Base exception class
│   │   └── CommonErrorCode     # Common error codes enum
│   └── presentation/            # GlobalExceptionHandler
│
└── <domain>/                    # Domain-specific packages (ai, auth, chat, member, trip, content)
    ├── presentation/            # REST controllers and WebSocket handlers
    │   ├── web/                # REST API controllers
    │   └── message/            # Event listeners (WebSocket, async processing)
    ├── application/             # Business logic orchestration
    │   ├── *UseCase            # Interface defining use cases
    │   └── *Service            # UseCase implementation
    ├── domain/
    │   ├── dto/                # Data Transfer Objects
    │   ├── entity/             # Domain entities (extend BaseEntity)
    │   └── event/              # Domain events
    ├── exception/
    │   └── *ErrorCode          # Domain-specific error codes (implements ErrorCode)
    └── infrastructure/          # External systems integration
        └── *Repository         # Data access (JPA repositories)
```

**Dependency Flow**: `presentation` → `application` → `domain` ← `infrastructure`

### Key Domains

- **ai**: Spring AI integration, AI agents (ChatAgent, TripAgent), stream processing
- **auth**: OAuth2 + JWT authentication, token management, Spring Security filters
- **chat**: WebSocket-based real-time messaging, conversation management
- **member**: User profile management
- **trip**: Travel itinerary CRUD, daily plan management
- **content**: External tourism API integration, place data with embeddings (pgvector)

## Critical Patterns

### 1. UseCase Interface Pattern
Each domain's `application` package defines `*UseCase` interfaces implemented by `*Service` classes:

```java
// Interface
public interface AiUseCase {
    void agentCall(String agentName, Object request);
}

// Implementation
@Service
public class AiService implements AiUseCase {
    @Override
    public void agentCall(String agentName, Object request) { ... }
}
```

### 2. Structured Exception Handling

```java
// Domain-specific error code (implements ErrorCode interface)
@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {
    MEMBER_NOT_FOUND("MEMBER001", HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    MEMBER_ALREADY_EXISTS("MEMBER002", HttpStatus.CONFLICT, "이미 존재하는 회원입니다.");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}

// Usage in service
throw new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND);
```

### 3. Event-Driven Communication
Use Spring ApplicationEvents for decoupling domains:

- Domain events are published via `ApplicationEventPublisher`
- Event listeners in `presentation/message/` handle async processing
- Common pattern: AI request → Event → Listener → WebSocket response

Examples:
- `AiChatGenerationRequestedEvent` → `AiChatGenerationListener` → streams AI responses via WebSocket
- `DailyPlanGeneratedEvent` → `TripPlanPersistenceListener` → saves plan to database

### 4. Base Entity Pattern
All entities extend `BaseEntity` for automatic timestamp management:

```java
@Entity
public class Member extends BaseEntity {
    // id, createdAt, updatedAt inherited from BaseEntity
}
```

## Testing Standards

### Test Structure (JUnit5 + Mockito + Nested)
```java
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ServiceTest {

    @Mock private Repository repository;
    @InjectMocks private Service service;

    @Nested
    @DisplayName("methodName 메서드는")
    class MethodName {

        @Test
        @DisplayName("given 조건 when 행위 then 결과")
        void test() {
            // given
            given(repository.method()).willReturn(value);

            // when
            var result = service.method();

            // then
            assertThat(result).isEqualTo(expected);
            verify(repository).method();
        }
    }
}
```

### Coverage Requirements
- Overall project: 60% LINE/BRANCH coverage
- Individual classes: 60% LINE coverage
- Excluded from coverage: `config/`, `dto/`, `entity/`, `exception/`, `agent/`, `*Config`, `*Exception`, `*ErrorCode`, `*Handler`

### Test Database
- Uses H2 in-memory database (configured in `application-test.yml`)
- Production uses PostgreSQL 16.x with pgvector extension

## Code Style Enforcement

This project uses **Spotless** with **Google Java Format**. All code must be formatted before committing.

### Import Order
```
java
javax
jakarta
org
com
(blank line)
static imports
```

### Formatting Rules
- Google Java Format for Java files
- ktfmt for Kotlin files
- prettier for YAML files
- All files: 4 spaces indentation, trim trailing whitespace, end with newline

**⚠️ CI will fail if code is not properly formatted. Always run `./gradlew spotlessApply` before committing.**

## Commit Message Conventions

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>: <subject>
```

**Types**:
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

**Breaking Changes**: Include `BREAKING CHANGE:` in body/footer (bumps MAJOR version)

## Build Configuration

### Custom Gradle Plugins (in `buildSrc/`)
- `plugin.coverage`: Jacoco configuration and coverage thresholds
- `plugin.spotless`: Code formatting rules (Google Java Format, ktfmt, prettier)
- `plugin.sonar`: SonarQube integration

### Dependency Management
All dependencies centrally managed in `gradle/libs.versions.toml` (Gradle Version Catalog).

Key dependencies:
- Spring Boot 3.5.6
- Spring AI 1.0.3 (with Naver ClovaX)
- JWT (jjwt 0.12.6)
- PostgreSQL + pgvector
- Redis (Lettuce)
- Hibernate Spatial
- Swagger/OpenAPI

## Technology-Specific Notes

### Spring AI Agent Pattern
AI agents (`ai/agent/`) implement `AiroadAgent` interface for specialized AI tasks:
- `ChatAgent`: Conversational editing of travel plans
- `TripAgent`: Initial travel itinerary generation

### WebSocket Communication
- Configuration: `chat/config/WebSocketConfig`
- Real-time AI streaming responses via WebSocket
- Error handling: `WebSocketErrorEvent` → `WebSocketErrorEventListener`

### RAG (Retrieval-Augmented Generation)
- Place embeddings stored in PostgreSQL with pgvector extension
- `content` domain handles tourism data with vector similarity search

### OAuth2 + JWT Authentication
- OAuth2 login flow: `CustomOAuth2UserService` → `OAuthLoginSuccessHandler`
- JWT-based session management via `JwtTokenProvider` and `JwtAuthenticationFilter`
- Access token + Refresh token pattern with Redis storage

## Profile Management

Profiles are configured via `application-{profile}.yml`:
- `local`: Local development
- `dev`: Development server
- `prod`: Production
- `test`: Test execution (H2 database)
- `ai`: AI configuration (included by default)

## CI/CD Pipeline

GitHub Actions workflow (`.github/workflows/ci.yml`):
1. Test & Build with coverage report
2. SonarQube analysis
3. Semantic Release (auto versioning on `main` branch)
4. Docker image build & push to ghcr.io

## Common Development Patterns

### Adding a New Feature
1. Create domain package if needed (follow layered structure)
2. Define DTOs in `domain/dto/`
3. Create entities extending `BaseEntity` in `domain/entity/`
4. Define domain-specific error codes in `exception/*ErrorCode`
5. Create `*UseCase` interface and `*Service` implementation in `application/`
6. Implement REST controller in `presentation/web/`
7. Add repository in `infrastructure/` if needed
8. Write tests following nested structure with `@DisplayName`
9. Run `./gradlew spotlessApply` before committing
10. Verify coverage with `./gradlew jacocoTestReport`

### Working with Events
1. Define event in `domain/event/`
2. Publish via `ApplicationEventPublisher` in service layer
3. Create listener in `presentation/message/` with `@EventListener` or `@TransactionalEventListener`
4. Handle WebSocket communication or async processing in listener

### Database Schema Changes
- Entities use `@EntityListeners(AuditingEntityListener.class)` via `BaseEntity`
- JPA Auditing configured in `common/config/JpaConfig`
- Use Hibernate Spatial for location data (`common/domain/embeddable/Location`)
