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


### 





```test
인덱스(INDEX)란 검색 속도를 높이기 위한 색인 기술이다.
보통 인덱스는 일반적으로 SELECT 쿼리의 WHERE에 사용할 컬럼에 대해 효율적인 검색을 위해 사용하거나, 다른 테이블과의 JOIN에 사용된다.
(주로 효율적인 검색을 위해 사용된다.)

일반적으로 SQL 서버에 데이터를 저장할 때는 내부적으로 아무런 순서없이 저장한다.
이때, 데이터 저장영역은 Heap 이라고 한다.
Heap에서는 인덱스가 없는 테이블의 데이터를 찾을 때
전체 데이터 페이지의 처음 레코드부터 끝 페이지 마지막 레코드까지 모두 조회하게 된다.
이러한 검색 방식을 풀 스캔(Full Scan) 또는 테이블 스캔(Table Scan)이라고 한다.

이러한 검색의 속도 향상을 이유로 인덱스를 사용하게 된다.

0. Sample Data
1) Create Table
아래와 같은 학생(t_student) 테이블이 있다고 가정하겠습니다.

-- t_student 테이블 SCHEMA
CREATE TABLE t_student (
  seq_no INTEGER PRIMARY KEY, -- sequence
  id CHAR(14) NOT NULL, -- 주민번호
  name VARCHAR(255) NOT NULL, -- 학생 이름
  age INTEGER NOT NULL, -- 나이
  grade INTEGER NOT NULL, -- 학년
  ins_timestamp     TIMESTAMP      NOT NULL -- 가입 일시
)
2) Add Index
인덱스는 아래와 같이 하나 혹은 두개 시앙의 컬럼에 대해서 설정할 수 있습니다.

-- single column index for id
CREATE INDEX si_id ON t_student (id);
-- single column index for name
CREATE INDEX si_name ON t_student (name);
-- multi column index for id, name
CREATE INDEX mi_id_name ON t_student (id, name);
-- multi column index for id, name, age
CREATE INDEX mi_id_name_age ON t_student (id, name, age);
...
인덱스가 효율적인 검색에 도움이 된다고 하니,
위와 같이 모든 컬럼에 대해 인덱스를 설정하면 좋을 것 같지만
무조건 많이 인덱스를 설정한다고 해서 검색 속도를 향상시켜주지는 않습니다.


1. 어떤 컬럼에 Index를 설정해야 할까?
1) 핵심적인 기준 4가지
인덱스는 한 테이블당 보통 3~5개가 적당합니다.
(정규화 정도나 테이블의 목적에 따라서는 개수가 달라질 수도 있습니다.)
아래 4가지 기준을 사용하여 기준에 부합하는 컬럼을 인덱스로 설정하는 것이 좋습니다.

카디널리티 (Cardinality)
카디널리티가 높으면(↑) 인덱스 설정에 좋은 컬럼이다. (인덱스를 통해 불필요한 데이터의 대부분을 걸러낼 수 있음.)
카디널리티가 높다 = 한 컬럼이 갖고 있는 값의 중복도가 낮음. (= 값들이 대부분 다른 값을 가짐.)
카디널리티가 낮다 = 한 컬럼이 갖고 있는 값의 중복도가 높음. (= 값들이 거의 같은 값을 가짐 )

선택도 (Selectivity)
선택도가 낮으면(↓) 인덱스 설정에 좋은 컬럼이다. (일반적으로 5~10%가 적당함.)
선택도가 높다 = 한 컬럼이 갖고 있는 값 하나로 여러 row가 찾아진다.
선택도가 낮다 = 한 컬럼이 갖고 있는 값 하나로 적은 row가 찾아진다.

선택도 계산법 (= 컬럼의 특정 값의 row 수 / 테이블의 총 row 수 * 100)
ex) 10개의 데이터에서 고유한 학번(grade) 컬럼, 2명씩 같은 이름(name) 컬럼, 5명씩 같은 나이(age) 컬럼인 경우
① 학번(grade) 컬럼 선택도: 1 / 10 = 10%
② 이름(name) 컬럼 선택도: 2 / 10 = 20%
③ 나이(age) 컬럼 선택도: 5 / 10 = 50%

조회 활용도
조회 활용도가 높으면(↑) 인덱스 설정에 좋은 컬럼이다.
해당 컬럼이 실제 작업에서 얼마나 활용되는지에 대한 값.
(WHERE의 대상 컬럼으로 많이 활용되는지로 판단하면 된다.)

수정 빈도
수정 빈도가 낮으면(↓) 인덱스 설정에 좋은 컬럼이다.
인덱스도 테이블이기 때문에, 인덱스로 지정된 컬럼의 값이 바뀌게 되면 인덱스 테이블도 새롭게 갱신되어야 하기 때문.


2) 그 밖의 Index 명시 사항
WHERE에 자주 사용되는 컬럼에 사용하기.
LIKE와 사용할 경우에는 %가 뒤에 사용되도록 하기. (앞에 사용되면 Full Scan)
ORDER BY에 자주 사용되는 컬럼에 사용하기.
JOIN에 자주 사용되는 컬럼에 사용하기.
데이터의 변경이 잦은 컬럼에는 인덱스를 사용하지 않기.
2. Index를 무조건 많이 설정하면 안되는 이유가 무엇일까?
'위의 (1) 어떤 컬럼에 인덱스를 설정해야 할까?'를 보았다면 인덱스를 많이 설정하면 안되는 이유를 어렴풋하게 이해할 수도 있습니다.

① 인덱스 설정 시, 데이터베이스에 할당된 메모리를 사용하여 테이블 형태로 저장되게 됩니다.
(즉, 인덱스가 많아지면 데이터베이스의 메모리를 많이 잡아먹게 됩니다.)
② 인덱스로 지정된 컬럼의 값이 바뀌게 되면 인덱스 테이블이 갱신되어야 하므로 느려질 수 있습니다.

위와 같은 이유로 인덱스를 계속해서 만드는 것이 하나의 쿼리문을 빠르게 만들 수는 있지만, 전체적인 데이터베이스의 성능 부하를 초래합니다.


3. 설정된 Index가 DML(Data Manipulation Language)에 미치는 영향
Index는 주로 SELECT 쿼리에서는 성능이 잘 나온다.
하지만 INSERT/UPDATE/DELETE에서는 경우에 따라 다르다.

UPDATE, DELETE
인덱스로 설정된 컬럼에 대해 조건(WHERE)을 사용할 수도 있는 UPDATE, DELETE사용 시 조회에서는 성능이 크게 저하되지 않는다.
※ 여기서 수정/삭제할 데이터를 찾는 때의 속도가 빠르다는 것이지 수정이나 삭제 그 자체를 빠르게 처리한다는 뜻은 아니다.

INSERT
반면, INSERT의 경우에는 효율이 좋지 않다.
새로운 데이터를 추가하면서 인덱스가 설정되어 있던 컬럼의 테이블이 수정되어야 하기 때문이다.


4. Single Column Index와 Multi Column Index의 비교
Multi Column Index의 장점
① 질의(SQL) 컬럼이 모두 조합 인덱스에 있는 경우, 물리적인 데이터 블록을 읽을 필요가 없다.
(인덱스 테이블만 읽으면 된다.)

Multi Column Index를 고려해야 하는 경우
① WHERE에 사용될 때 AND 연산자에 의해 자주 같이 질의되는 컬럼들인 경우.


5. Index 생성으로 발생되는 특징 요약
검색 (SELECT) 속도 향상
인덱스 테이블을 위한 추가 공간과 시간 필요.
INSERT, UPDATE, DELETE가 경우에 따라 성능 하락 발생.
6. Clustered Index와 Non Clustered Index
1) Clustered Index
테이블 당 하나의 Clustered Index만 생성이 가능하다. (일반적으로 PK 컬럼으로 자동 생성됨.)
맨 위의 '0. Sample Data'에서 생성한 테이블을 기준으로 'seq_no' 컬럼은 PRIMARY KEY로 지정하였기 때문에, 자동으로 Clustered Index가 된다.
만약, Clustered Index가 아닌 Non Clustered Index로 설정하고 싶다면 아래와 같이 스키마를 수정할 수 있다.
CREATE TABLE t_student (
  seq_no INTEGER PRIMARY KEY NONCLUSTERED, -- sequence (non clustered index)
  ...
)
물리적으로 데이터를 정리하므로 Clustered Index의 Index 테이블은 하나만 존재한다.
데이터 입력 시, 물리적 정렬로 DB에 Clustered Index를 기준으로 입력이 되므로
Heap에 있는 데이터를 꺼내었을 때, 모든 페이지의 데이터가 Clustered Index를 기준으로 정렬이 되어 있는 것을 확인할 수 있다.
물리적으로 정렬되어 있기 때문에 순차적 데이터를 접근할 때 가장 빠른 처리를 한다.

2) Non Clustered Index
테이블당 여러 개를 생성할 수 있다.
데이터 페이지가 물리적으로 정렬되어 있지 않기 때문에 인덱스에 의해 찾아가야 한다.
※ Non Clustered Index에 대해 더 궁금하다면? 이 포스팅을 참고하시면 됩니다.

7. Index와 SlowQuery
이전에 작성했던 슬로우 쿼리에 대처했던 경험에 대한 포스팅이 있습니다.
실행계획을 분석하며 여러 Scan 과정에서 Index가 어떻게 사용되었는지 확인할 수 있어서 추가하여 보았습니다. Postgresql 슬로우 쿼리에 대처하기를 참고해주세요.




서버 개발을 하다보면, 직접 테이블을 생성하고 관련 설정(인덱스)들을 해야 할 때가 있습니다.
데이터베이스에 대해 전문적 지식을 가지고 효율을 생각해서 만들 수 있으면 좋겠지만,
각 분야의 전문적 지식을 가지진 못하더라도
작업 시점에 가지고 있는 지식을 사용한 최선의 선택으로 데이터베이스에 대한 작업을 하는 편입니다.


DB 인덱스를 효과적으로 설정하는 방법 - 고려해야 할 4가지
by Yurim Koo

March 14, 2020in Db
DB에는 여러 개의 테이블이 존재하고, 그 테이블에는 다양한 수의 데이터가 쌓입니다.
만약 천 만 개의 데이터가 쌓였다고 가정할 때, 여러 조건을 조합해서 데이터를 조회하려면 로딩 시간이 아주 오래 걸리겠지요?
이 때 인덱스(index)를 설정하면 속도가 빨라집니다.



인덱스의 개념
책의 색인

“‘찾아보기’로 일컬어지는 색인(索引)은 책에서 중요한 단어나 항목, 고유명사 등을 쉽게 찾을 수 있도록, 그것들을 일정 순서에 따라 배열한 목록이다.” - [표정훈의 호모부커스]<83>색인

인덱스는 테이블의 동작 속도를 높여주는 자료 구조입니다.
인덱스로 데이터의 위치를 빠르게 찾아주는 역할을 합니다.

쉬운 예시로, 책 뒷 편에 ‘색인’이 바로 이 인덱스의 역할이라고 보면 됩니다.
색인을 통해 원하는 키워드에 대한 페이지로 바로 이동할 수 있지요.

책의 색인이 여러 페이지에 기재되어 있는 것처럼
DB의 인덱스도 데이터베이스 메모리에 일정 공간을 사용해 저장이 되고,
데이터를 조회할 때 소모되는 메모리를 효율적으로 사용하게 합니다.

단, 인덱스가 잘 설정되었을 경우에요!



인덱스의 특징
인덱스는 WHERE 절에서 효과가 있다
인덱스는 SELECT - FROM - WHERE 절 중 WHERE 절에 사용할 컬럼에 대한 효율화라고 볼 수 있습니다.
WHERE 절을 사용하지 않고 인덱스가 걸린 컬럼을 조회하는 것은 성능에 아무런 영향이 없습니다.

예를 들어, ‘학생’ 테이블에 ‘학번’, ‘이름’, ‘전화번호’가 있다고 가정해봅시다.
인덱스는 ‘학번’, ‘전화번호’에 걸려 있습니다.
다음 중 인덱스가 영향을 주는 쿼리는 어떤 것일까요?

1번) SELECT '학번' FROM '학생';
2번) SELECT '전화번호' FROM '학생' WHERE '이름' = "김철수";
3번) SELECT * FROM '학생' WHERE '학번' = 1;
정답은 3번)입니다!
WHERE 절에 사용할 때 성능을 향상시킵니다.



무조건 많이 설정하면 좋은걸까?
인덱스는 하나 혹은 여러 개의 컬럼에 대해 설정할 수 있습니다.
단일 인덱스를 여러 개 생성할 수도, 여러 컬럼을 묶어 복합 인덱스를 설정할 수도 있습니다.

그러나 무조건 많이 설정하는게 검색 속도 향상을 높여주지는 않습니다.
인덱스는 데이터베이스 메모리를 사용하여 테이블 형태로 저장되므로 개수와 저장 공간은 비례합니다.
따라서,

조회시 자주 사용하고
고유한 값 위주로
인덱스를 설정하는게 좋습니다.



DML(Data Manipulation Language) 각각에는 어떤 영향을 미칠까?
SELECT 쿼리에서 성능이 잘 나오지만, INSERT, UPDATE, DELETE 쿼리에서는 때에 따라 다릅니다.

UPDATE, DELETE는 WHERE 절에 잘 설정된 인덱스로 조건을 붙여주면 조회할 때 성능은 크게 저하되지 않으나
(업데이트 할 데이터를 찾을 때의 속도가 빨라지는 것이지, 업데이트 자체가 빨라지는 것은 아님!)
INSERT의 경우, 새로운 데이터가 추가되면서 → 기존에 인덱스 페이지에 저장되어 있던 탐색 위치가 수정되어야 하므로 효율이 좋지 않습니다.
즉, 인덱스는 원하는 데이터를 빠르게 찾을 때 빛을 발합니다.



그럼 어떤 컬럼에 인덱스를 설정하는게 좋을까?
인덱스는 한 테이블당 보통 3~5개 정도가 적당합니다.
물론 테이블의 목적 등에 따라 개수는 달라질 수 있습니다.

인덱스는 컬럼을 정해서 설정하는 것이므로 후보 컬럼의 특징을 잘 파악해야 합니다.
아래 4가지 기준을 사용하면 효율적으로 인덱스를 설정할 수 있습니다.

카디널리티 (Cardinality)
선택도 (Selectivity)
활용도
중복도


카디널리티 (Cardinality)
✔️ 카디널리티가 높을 수록 인덱스 설정에 좋은 컬럼입니다.
= 한 컬럼이 갖고 있는 값의 중복 정도가 낮을 수록 좋습니다.

컬럼에 사용되는 값의 다양성 정도, 즉 중복 수치를 나타내는 지표입니다.
후보 컬럼에 따라 상대적으로 중복 정도가 낮다, 혹은 높다로 표현됩니다.

예를 들어, 10개 rows를 가지는 ‘학생’ 테이블에 ‘학번’과 ‘이름’ 컬럼이 있다고 해봅시다.

‘학번’은 학생마다 부여 받으므로 10개 값 모두 고유합니다.
중복 정도가 낮으므로 카디널리티가 높습니다.
‘이름’은 동명이인이 있을 수 있으니 1~10개 사이의 값을 가집니다.
중복 정도가 ‘학번’에 비해 높으므로 카디널리티가 낮다고 표현할 수 있습니다.


선택도 (Selectivity)
✔️ 선택도가 낮을 수록 인덱스 설정에 좋은 컬럼입니다.
5~10% 정도가 적당합니다.

데이터에서 특정 값을 얼마나 잘 선택할 수 있는지에 대한 지표입니다.
선택도는 아래와 같이 계산합니다.

= 컬럼의 특정 값의 row 수 / 테이블의 총 row 수 * 100
= 컬럼의 값들의 평균 row 수 / 테이블의 총 row 수 * 100

예를 들어, 10개 rows를 가지는 ‘학생’ 테이블에 ‘학번’, ‘이름’, ‘성별’ 컬럼이 있다고 해봅시다.
학번은 고유하고, 이름은 2명씩 같고, 성별은 남녀 5:5 비율입니다.

‘학번’의 선택도 = 1/10*100 = 10%
SELECT COUNT(1) FROM '학생' WHERE '학번' = 1; (모두 고유하므로 특정 값: 1)
‘이름’의 선택도 = 2/10*100 = 20%
SELECT COUNT(1) FROM '학생' WHERE '이름' = "김철수"; (2명씩 같으므로 특정 값: 2)
‘성별’의 선택도 = 5/10*100 = 50%
SELECT COUNT(1) FROM '학생' WHERE '성별' = F; (5명씩 같으므로 특정 값: 5)
즉, 선택도는 특정 필드값을 지정했을 때 선택되는 레코드 수를 테이블 전체 레코드 수로 나눈 것입니다.



활용도
✔️ 활용도가 높을 수록 인덱스 설정에 좋은 컬럼입니다.

해당 컬럼이 실제 작업에서 얼마나 활용되는지에 대한 값입니다.
수동 쿼리 조회, 로직과 서비스에서 쿼리를 날릴 때 WHERE 절에 자주 활용되는지를 판단하면 됩니다.



중복도
✔️ 중복도가 없을 수록 인덱스 설정에 좋은 컬럼입니다.

중복 인덱스 여부에 대한 값입니다.

인덱스 성능에 대한 고려 없이 마구잡이로 설정하거나,
다른 부서 다른 작업자의 분리된 요청으로
같은 컬럼에 대해 인덱스가 중복으로 생성된 경우를 볼 수 있습니다.

인덱스도 속성을 가집니다.
인덱스는 테이블 형태로 생성되므로, 속성을 컬럼으로 관리합니다.

(참고) 주요 인덱스 컬럼
- Table: The name of the table.
- Non_unique: 0 if the index cannot contain duplicates, 1 if it can.
- Key_name: The name of the index. If the index is the primary key, the name is always PRIMARY.
- Seq_in_index: The column sequence number in the index, starting with 1.
- Column_name: The column name.
- Collation: How the column is sorted in the index. This can have values A (ascending) or NULL (not sorted).
- Cardinality: An estimate of the number of unique values in the index.
- Index_type: The index method used (BTREE, FULLTEXT, HASH, RTREE)

출처: https://www.fun-coding.org/mysql_advanced5.html

이 속성이 다를 때 같은 컬럼에 대해 중복으로 인덱스 설정이 가능합니다.
같은 컬럼에 대해 중복 인덱스가 설정되어 있다고 하더라도 SQL 자체 연산이 빠른 쪽으로 데이터를 조회합니다만,
인덱스도 결국 메모리의 일부이므로 필요 없는 항목은 삭제하는게 좋겠지요?



인덱스 설정 기준 - 정리!
기준	정도
카디널리티 (Cardinality)	높을 수록 적합
선택도 (Selectivity)	낮을 수록 적합 (5~10% 적정)
활용도	높을 수록 적합
중복도	없을 수록 적합
위 기준을 활용해서 효율적인 인덱스를 설정해보세요! 😊


1) INDEX의 의미
RDBMS에서 검색속도를 높이기 사용하는 하나의 기술입니다.
INDEX는 색인입니다. 해당 TABLE의 컬럼을 색인화(따로 파일로 저장)하여 검색시 해당 TABLE의 레코드를 full scan 하는게 아니라 색인화 되어있는 INDEX 파일을 검색하여 검색속도를 빠르게 합니다.
이런 INDEX는 TREE구조로 색인화합니다. RDBMS 에서 사용하는 INDEX는 Balance Search Tree 를 사용합니다.
실제로는 RDBMS 에서 사용되는 B-Tree 는 B-Tree 에서 파생된 B+ Tree 를 사용한다고 합니다.
 
참고로 ORACLE이나 MSSQL에서는 여러종류의 TREE를 선택하여 사용가능하다.


2) INDEX의 원리
 
INDEX를 해당 컬럼에 주게 되면 초기 TABLE생성시 만들어진 MYD,MYI,FRM 3개의 파일중에서
MYI에 해당 컬럼을 색인화 하여 저장합니다. 물론 INDEX를 사용안할시에는 MYI파일은 비어 있습니다. 그래서 INDEX를 해당컬럼에 만들게 되면 해당컬럼을 따로 인덱싱하여 MYI 파일에 입력합니다. 그래서 사용자가 SELECT쿼리로 INDEX가 사용하는 쿼리를 사용시 해당 TABLE을 검색하는것이 아니라 빠른 TREE로 정리해둔 MYI파일의 내용을 검색합니다.
만약 INDEX를 사용하지 않은 SEELCT쿼리라면 해당 TABLE full scan하여 모두 검색합니다.
이는 책의 뒷부분에 찾아보기와 같은 의미로 정리해둔 단어중에서 원하는 단어를 찾아서 페이지수를 보고 쉽게 찾을수 있는 개념과 같습니다. 만약 이 찾아보기 없다면 처음부터 끝까지 모든 페이지를 보고 찾아야 할것입니다.

3) INDEX의 장점
- 키 값을 기초로 하여 테이블에서 검색과 정렬 속도를 향상시킵니다.
- 질의나 보고서에서 그룹화 작업의 속도를 향상시킵니다.
- 인덱스를 사용하면 테이블 행의 고유성을 강화시킬 수 있습니다.
- 테이블의 기본 키는 자동으로 인덱스 됩니다.
- 필드 중에는 데이터 형식 때문에 인덱스 될 수 없는 필드도 있습니다.
- 여러 필드로 이루어진(다중 필드) 인덱스를 사용하면 첫 필드 값이 같은 레코드도 구분할   수 있습니다.
  참고로 액세스에서 다중 필드 인덱스는 최대 10개의 필드를 포함할 수 있습니다.
 
4) INDEX의 단점
- 인덱스를 만들면 .mdb 파일 크기가 늘어난다.
- 여러 사용자 응용 프로그램에서의 여러 사용자가 한 페이지를 동시에 수정할 수 있는 병행성이 줄어든다.
- 인덱스 된 필드에서 데이터를 업데이트하거나, 레코드를 추가 또는 삭제할 때 성능이 떨어집니다.
- 인덱스가 데이터베이스 공간을 차지해 추가적인 공간이 필요해진다. (DB의 10퍼센트 내외의 공간이 추가로 필요)
- 인덱스를 생성하는데 시간이 많이 소요될 수 있다.
- 데이터 변경 작업이 자주 일어날 경우에 인덱스를 재작성해야 할 필요가 있기에 성능에 영향을 끼칠 수 있다.

따라서 어느 필드를 인덱스 해야 하는지 미리 시험해 보고 결정하는 것이 좋습니다. 인덱스를 추가하면 쿼리 속도가 1초 정도 빨라지지만, 데이터 행을 추가하는 속도는 2초 정도 느려지게 되어 여러 사용자가 사용하는 경우 레코드 잠금 문제가 발생할 수 있습니다.

또, 다른 필드에 대한 인덱스를 만들게 되면 성능이 별로 향상되지 않을 수도 있습니다. 예를 들어, 테이블에 회사 이름 필드와 성 필드가 이미 인덱스 된 경우에 우편 번호 필드를 추가로 인덱스에 포함해도 성능이 거의 향상되지 않습니다. 만드는 쿼리의 종류와 관계 없이 가장 고유한 값을 갖는 필드만 인덱스 해야 합니다.

5) INDEX의 목적
 
RDBMS에는 INDEX가 있습니다. 인덱스의 목적은 해당 RDBMS의 검색 속도를 높이는데 있습니다.
SELECT 쿼리의 WHERE절이나 JOIN 예약어를 사용했을때만 인덱스를 사용되며 SELECT 쿼리의 검색 속도를 빠르게 하는데 목적을 두고 있습니다.
※ DELETE,INSERT,UPDATE쿼리에는 해당 사항없으며 INDEX사용시 좀 느려집니다.
 
6) 인덱스를 생성해야 하는 경우와 그렇지 않은 경우
- 인덱스는 열 단위로 생성된다.
- WHERE절에서 사용되는 컬럼을 인덱스로 만든다.
- 데이터의 중복도가 높은 열은 인덱스로 만들어도 효용이 없다. (예 : 성별, 타입이 별로 없는 경우, 적은경우)
- 외래키가 사용되는 열에는 인덱스를 되도록 생성해주는 것이 좋다.
- JOIN에 자주 사용되는 열에는 인덱스를 생성해주는 것이 좋다.
- INSERT / UPDATE / DELETE가 얼마나 자주 일어나는지를 고려한다.
- 사용하지 않는 인덱스는 제거하자
 
7) 인덱스 주의사항
 * SELECT하는 경우에도 데이터 블록 수와 DB_FILE_MULTIBLOCK_READ_COUNT 값과 분포도 등에 따라 인덱스가 빠를 경우도 있고 full table scan보다 늦어지는 경우도 있음.

 7-1. DML에 취약함 
ⓐ INSERT
: index split 현상이 발생할 수 있음.
index split - 인덱스의 Block들이 하나에서 두 개로 나누어지는 현상 
-> 인덱스는 데이터가 순서대로 정렬 되어야 함. 기존 블록에 여유 공간이 없는 상황에서  그 블록에 새로운 데이터가 입력되어야 할 경우
오라클이 기존 블록의 내용 중 일부를 새 블록에다가 기록한 후 기존 블록에 빈 공간을 만들어서 새로운 데이터를 추가하게 됨.
*성능면에서 매우 불리 
① index split은 새로운 블록을 할당 받고 key를 옮기는 복잡한 작업을 수행. 모든 수행 과정이 Redo에 기록.. 많은 양의 Redo를 유발함
② index split이 이루어지는 동안 해당 블록에 대해 키 값이 변경되면 안되므로 DML이 블로킹됨.
enq:TX-index contention 대기 이벤트 발생(RAC- gc current split)

ⓑ DELETE
테이블에서 데이터가 delete 될 경우 - 지워지고 다른 데이터가 그 공간을 사용 가능
index에서 데이터가 delete 될 경우 - 데이터가 지워지지 않고, 사용 안 됨 표시만 해 둔다.
->즉, 테이블에 데이터가 1만건 있는 경우, 인덱스에는 2만건이 있을 수 있다는 뜻
=> 인덱스를 사용해도 수행속도를 기대하기 힘들다.

ⓒ UPDATE : 인덱스에는 update 개념이 없음!!!
테이블에 update가 발생할 경우 인덱스에서는 delete가 먼저 발생한 후 새로운 작업의 insert 작업이 발생한다. delete와 insert 두 개의 작업이 인덱스에 동시에 일어나 다른 DML보다 더 큰 부하를 주게 됨.

 7-2. 타 SQL 실행에 악영향을 줄 수 있음
갑자기 인덱스를 추가하면 잘 돌아가고 있던 쿼리에 옵티마이저가 실행계획을 바꾸는 경우가 생겨 갑자기 아주 느려지는 경우가 생김
-> 기존의 테이블에 인덱스를 추가할 경우 기존에 있던 SQL 문장들까지 전부 고려한 후 인덱스를 생성해야 함.

 
8) 생성방법
 - 자동생성 : PK나 Unique제약 조건을 정의할 경우 Unique Index가 자동으로 생성됨

9) 인덱스 종류
9-1. B-TREE
 B: binary, balance

   Root
   block : branch block에 대한 정보
      l
  Branch
   block : leaf blcok에 대한 정보
      l
    Leaf
   block : 실제 데이터들의 주소


ⓐ  UNIQUE INDEX : 인덱스 안에 있는 key 값에 중복되는 데이터가 없음 (성능 good)
 - 생성 문법
 SQL > CREATE UNIQUE INDEX 인덱스명
     2  ON 테이블이름(컬럼명 1 ASC|DESC, 컬럼명,....) ;

ASC : 오름차순 정렬 (기본값)
DESC  : 내림차순 정렬

SQL> create table dept2(no number, dname varchar2(10));

Table created.

SQL> create unique index idx_dept2_dname
  2  on dept2(dname);

Index created.


SQL> insert into dept2
  2  values(9100,'임시매장');

1 row created.

SQL> insert into dept2
  2  values(1006,'서울지사');

1 row created.

SQL> insert into dept2
  2  values(9101,'임시매장');
insert into dept2
*
ERROR at line 1:
ORA-00001: unique constraint (SCOTT.IDX_DEPT2_DNAME) violated

// 이미 들어간 dname이라서 에러 발생

ⓑ Non UNIQUE INDEX : 중복되는 데이터가 들어가야 하는 경우
 - 생성 문법
 SQL > CREATE INDEX 인덱스명
     2  ON 테이블이름(컬럼명 1 ASC|DESC, 컬럼명,....) ;

예 - professor table의 position 컬럼에 Non UNIQUE INDEX를 생성
SQL > create index idx_prof_position
    2   on professor(position);

ⓒ Function Based INDEX ( FBI - 함수기반 인덱스 )

* 인덱스는 where절에 오는 조건 컬럼이나 조인에 쓰이는 컬럼으로 만들어야 함
** 인덱스를 사용하려면 where 절의 조건을 절대로 다른 형태로 가공해서 사용하면 안된다.

꼭 써야 할 때
SAL + 100 = 200 이라는 형태로 써야할 때
-> 인덱스도 SAL+100 형태의 인덱스를 생성= 함수기반 인덱스 

SQL> create index idx_prof_pay_fbi
  2  on professor(pay+100);

Index created.

professor table에 pay+100이라는 컬럼이 없지만 인덱스를 만들 때 저 연산을 수행해서 인덱스를 만들어줌.

-> 임시적인 해결책은 될 수 있어도 근본적 처방은 아님
-> pay + 100을 생성했는데 쿼리의 조건이 변경되면 인덱스를 다시 생성해야 함
-> FBI는 기존 인덱스를 활용할 수 없다.(단점)

ⓓ DESCENDING INDEX : 내림차순으로 인덱스를 생성함
큰 값을 많이 조회하는 SQL에 생성하는 것이 좋음!
ex ) 계좌내역 최근 날짜부터 조회, 회사 매출

SQL> create index idx_prof_pay
  2  on professor(pay desc);

Index created.


하나의 메뉴에 오름차순과 내림차순을 한번에 조회 할 경우
: 오름차순, 내림차순 두 개의 인덱스를 만들면 DML의 성능에 악영향을 미침
-> 힌트를 사용 ( 아래나 위에서부터 읽도록 할 수 있음)

ⓔ 결합 인덱스 (Composite INDEX) : 인덱스 생성시 두 개 이상의 컬럼을 합쳐서 인덱스 생성
주로 where 절의 조건 컬럼이 2개 이상이 and로 연결되어 사용되는 경우 많이 사용
-> 잘못 생성하게 되면 성능에 나쁜 영향을 미침!!

예) 사원 테이블에 총 50명이 있고, 남자 25명, 여자 25명
여자 중 이름이 '유관순'인 사람이 2명

사원 테이블에서 성별이 '여자' 이고 이름이 '유관순'인 사람을 찾을 때 :
SQL > SELECT 이름, 성별
FROM 사원
WHERE 성별 = '여자'
AND 이름 = '유관순';

* 결합 인덱스 생성 구문 예 :

SQL > CREATE INDEX idx_사원_성별_이름
   2  ON 사원(성별,이름);

**** 결합 인덱스 생성시 컬럼의 배치 순서 

case 1 : ON 사원(성별, 이름) 

50 명 -> 여자 -> 25명 -> 유관순 -> 2명
        50                      => 25회 검사

case 2 : ON 사원(이름, 성별)

50명 -> 유관순 -> 2명 -> 여자 -> 2명
        50                      => 2회 검사


=> 같은 테이블에 같은 SQL이지만 결합 인덱스를 어떻게 생성하는가에 따라 속도나 검사 횟수가 완전히 달라지게 된다. (신중히 생성...)

9-2. BITMAP INDEX 
: 데이터 값의 종류가 적고 동일한 데이터가 많을 경우에 많이 사용
성별 컬럼 : 남. 여 
 
Bitmap index를 생성하려면 데이터의 변경량이 적어야 하고, 값의 종류도 적은 곳이 좋다.
일반적으로 OLAP환경에서 많이 생성하게 됨. 
무조건 적으로 생성해야 하는 것이 아니고, 테이블 성격이나 데이터를 종합적으로 분석해서 적절한 인덱스를 생성한다. 

BITMAP INDEX는 어떤 데이터가 어디있다는 지도정보(map)를 Bit로 표시한다. 
데이터가 존재하는 곳은 1로, 데이터가 없는 곳은 0으로 표시  정보를 찾을 때 1인 값만 찾음 !!! 

SQL > create BITMAP index dex_사원_성별_bit   2  on 사원(성별);
bitmap index를 생성하면 성별 컬럼 값의 종류대로 map이 생성됨.
남자 : 1   0   1   0   0
여자 : 0   1   0   1   1

bitmap index를 사용하고 있는데 만약 컬럼 값이 새로 하나 더 생길 경우? 
기존의 BITMAP INDEX를 전부 수정해야 함. 
-> B-TREE INDEX는 관련 블록만 변경되면 되지만 BITMAP INDEX는 모든 맵을 다 수정해야 한다는 큰 문제점
-> BITMAP INDEX는 블록 단위로 lock을 설정해서 같은 블록에 들어있는 다른 데이터도 수정작업이 안 되는 경우가 종종 생김 

9-3. 데이터 처리 방법  
- OLTP ( Online Transaction Processing - 실시간 트랜잭션 처리) : 실시간으로 데이터 입력과 수정이 일어나는 환경 - B -TREE 인덱스 많이 사용
- OLAP ( Online Analytical Processing - 온라인 분석 처리) : 대량의 데이터를 한꺼번에 입력한 후 주로 분석이나 통계 정보를 출력할 때 사용하는 환경 - BITMAP 인덱스 많이 사용

10) 인덱스의 종류
  10-1. 고유 인덱스(Unique Index)
- 고유 인덱스는 유일한 값을 갖는 컬럼에 대해서 생성하는 인덱스로 고유 인덱스를 지정하려면 UNIQUE 옵션을 지정해야 합니다.
   SQL> CREATE UNIQUE INDEX  idx_ukempno_emp ON emp(empno);
 10-2. 비고유 인덱스(NonUnique Index)
 10-3. 단일 인덱스(Single Index)
- 단일 인덱스는 한 개의 컬럼으로 구성한 인덱스를 말합니다.
  SQL> CREATE INDEX  idx_ukempno_emp ON emp(empno);
 10-4. 결합 인덱스(Composite Index)
- 결합 인덱스는 두 개 이상의 컬럼으로 인덱스를 구성하는 것을 말합니다. 부서 번호와 부서명을 결합하여 인덱스를 설정 해 보도록 하겠습  니다.
  SQL> CREATE INDEX  idx_dept_com ON index_dept(deptno, dname);
 10-5. 함수 기반 인덱스(Function Based Index)
- 함수 기반 인덱스는 SAL*12와 같이 컬럼에 어떠한 산술식을 수행했을때를 말합니다.
  SAL컬럼에 INDEX가 걸려있다해도 SAL*12은 INDEX를 타지 못합니다. 이럴때 함수 기반 인덱스를 생성합니다.
  SQL> CREATE INDEX  idx_annsal_emp ON emp(sal*12);

11) 인덱스 구조와 작동 원리 (B-TREE 기준)
테이블과 인덱스 비교
- 테이블은 컬럼이 여러 개, 데이터가 정렬되지 않고 입력된 순서대로 들어감
-> 인덱스는 컬럼이 key 컬럼과 ROWID 컬럼 두 개로 이루어져 있음 ( 오름차순, 내림차순 정렬가능)

Key : 인덱스를 생성하라고 지정한 컬럼의 값


select *
from emp
where empno=7902;

데이터 파일의 블록이 10만개 일 때, SQL을 수행시
1. 서버 프로세스가 파싱 과정을 마친 후 DB buffer cache에 empno 가 7902인 정보가 있는지 확인
2. 정보가 없으면 하드 디스크 파일에서 7902정보를 가진 블록을 복사해서 DB buffer cache로 가져온 후 7900 정보만 골라내서 사용자에게 보여줌
이 때
index 없는 경우 -  7902정보가 어떤 블록에 들어 있는지 모르므로 10만개 전 부 db buffer cache로 복사한 후 하나하나 찾음
index 있는 경우 - where 절의 컬럼이 index가 만들어져 있는지 확인 후, 인덱스에 먼저 가서 7902정보가 어떤 ROWID를 가지고 있는지 확인한 후 해당 ROWID에 있는 블록만 찾아가서 db buffer cache에 복사함.

12) 단일 컬럼 인덱스

 12-1. 인덱스를 고려 해야 하는 경우
- WHERE 조건 절에 자주 사용되는 컬럼
- 자주 바뀌는 않는 컬럼

 12-2. 변경 작업에 따른 인덱스에 부하 감소
- 동일 값이 작은 컬럼(1~15% 이하, 5%이하)

 12-3. 인덱스를 피해야 하는 경우
- 함수 및 연산자에 의해 변경되는 컬럼
- 낮은 선택도
- 균일 분포가 아닌 컬럼

 12-4. 조합 인덱스가 단일 인덱스 보다 많은 장점이
있지만 2개의 단일 인덱스 사용이 성능 상 좋다.

13) 조합 컬럼 인덱스

 13-1. 조합 인덱스의 큰 장점
- 선택도가 좋지 않은 두 개 이상의 컬럼을 조합하여,선택도가 좋은 조합 인덱스가 된다.
- 질의 컬럼이 모두 조합 인덱스에 있는 경우, 물리적인 데이터 블록을 읽을 필요가 없다.

 13-2. 조합 인덱스를 고려해야 하는 경우
- AND 연산자에 의해 자주 같이 질의 되는 컬럼들
- 다수의 여러 개의 질의가 특정 컬럼들을 질의

 13-3. 인덱스 가이드라인
- 자주 사용되는 컬럼을 왼쪽
- 동일한 사용 빈도이면 선택도가 좋은 것을 왼쪽

14) 새로운 인덱스 추가 시 문제점

 14-1. 기존 인덱스에 의해 잘 운용되던 환경 가정

 14-2. 새로운 SQL 문장을 위해 새 인덱스 필요성 발생
- 새로운 인덱스에 따른 테이블 관리 비용 상승
- 기존 SQL 문장이 혼동(Upset)될 가능성 커짐

 14-3. 잘 돌던 SQL이 새로운 인덱스를 타는 경우

 14-4. 새로운 인덱스의 추가는 신중해야 한다.
- 기존의 SQL에 미치는 영향 최소화

15) 효율적인 인덱스 및 SQL 활용 방안

 15-1. 각 테이블에 적당한 수의 인덱스 사용
 15-2. 가능한 Unique 인덱스 생성
 15-3. 전체 테이블의 5%이상 되는 SQL 질의 회피
- 인덱스 사용 비용이 더 높음
 15-4. 인덱스가 타지 않는 SQL 문장 실수 주의
 15-5. 정렬 작업의 최소화
- DISTINCT, ORDER BY, GROUP BY
- 정렬이 최소화되는 인덱스 활용

 15-6. IN 보다는 EXISTS(정렬최소화)

 15-7. 선택도가 좋은 선행 테이블을 FROM 절에 맨마지막에
16) INDEX가 동작하지 않는 경우
- 인덱스 컬럼 절의 변형
- 내부적인 데이터 변환
- NULL 조건의 사용
- 부정형 조건의 사용
- LIKE 연산자 사용
- 최적기가 판단


Index
Index는 RDBMS에서 검색 속도를 높이기 위한 기술이다.

TABLE의 컬럼을 색인화(따로 파일로 저장)하여 검색시 해당 TABLE의 레코드를 Full Scan 하는게 아니라 색인화 되어있는 INDEX 파일을 검색하여 검색속도를 빠르게 한다.

RDBMS에서 사용하는 INDEX는 B-Tree 에서 파생된 B+ Tree 를 사용해서 색인화한다.

보통 SELECT 쿼리의 WHERE절이나 JOIN 예약어를 사용했을때 인덱스가 사용되며 SELECT 쿼리의 검색 속도를 빠르게 하는데 목적을 두고 있다.

DELETE, INSERT, UPDATE 쿼리에는 해당 사항이없으며 INDEX 사용시 오히려 느려진다

조금더 자세히 알아보면, SQL서버에서 데이터의 레코드는 내부적으로 아무런 순서없이 저장된다.

이때 데이터 저장영역을 Heap이라고 한다.

Heap에서는 인덱스가 없는 테이블의 데이터를 찾을 때 전체 데이터 페이지의 처음 레코드부터 끝 페이지의 마지막 레코드까지 모두 조회하여 검색조건과 비교하게 된다.

이러한 데이터 검색방법을 테이블 스캔(Table Scan) 또는 풀 스캔(Full Scan)이라고 한다.

이럴 경우 양이 많은 테이블에서 일부분의 데이터만 불러 올 때 풀 스캔을 하면 처리 성능이 떨어진다.

즉 인덱스는 데이터를 SELECT 할 때 빨리 찾기 위해 사용된다.


Index 사용 이유
WHERE 구문과 일치하는 열을 빨리 찾기 위해.

특정 열을 고려 대상에서 빨리 없애 버리기 위해.

조인 (join)을 실행할 때 다른 테이블에서 열을 추출하기 위해.

특정하게 인덱스된 컬럼을 위한 MIN() 또는 MAX() 값을 찾기 위해.

사용할 수 있는 키의 최 좌측 접두사(leftmost prefix)를 가지고 정렬 및 그룹화를 하기 위해.

데이터 열을 참조하지 않는 상태로 값을 추출하기 위해서 쿼리를 최적화 하는 경우.


B-Tree 알고리즘 사용 이유
B+ Tree
Index에 일반적으로 사용되는 알고리즘은 B+ Tree 알고리즘이다.
B+ Tree 인덱스는 컬럼의 값을 변형하지 않고(값의 앞부분만 잘라서 관리), 원래의 값을 이용해 인덱싱하는 알고리즘이다.

Hash
컬럼의 값으로 해시 값을 계산해서 인덱싱하는 알고리즘으로 매우 빠른 검색을 지원한다.
하지만 값을 변형해서 인덱싱하므로, 특정 문자로 시작하는 값으로 검색을 하는 등 전방 일치와 같이 값의 일부만으로 검색하고자 할 때는 해시 인덱스를 사용할 수 없다. 주로 메모리 기반의 데이터베이스에서 많이 사용한다.

데이터 접근에 시간 복잡도가 O(1)인 Hash Table말고 B-Tree를 사용하는 이유는 SELECT 절의 조건에 부등호 연산(>, <)이 포함될 경우 문제가 발생한다.
HashTable은 동등 연산(=)에 특화되어있어 데이터베이스의 자료구조에 적합하지 않다.


Index 구조와 작동 원리
Index 구조
Index는 논리적/물리적으로 테이블과 독립적이다.

테이블은 컬럼에 데이터가 정렬되지 않고 입력된 순서대로 들어가지만, Index는 KEY 컬럼과 ROWID 컬럼 두개로 이루어져 있고 오름차순, 내림차순으로 정렬이 가능하다.

Key : 인덱스를 생성하라고 지정한 컬럼의 값

MySQL에서 테이블 생성 시, 아래와 같은 3가지 파일이 생성된다.

FRM : 테이블 구조 저장 파일
MYD : 실제 데이터 파일
MYI : Index 정보 파일 (Index 사용 시 생성)
사용자가 쿼리를 통해 Index를 사용하는 칼럼을 검색하게 되면, 이때 MYI 파일의 내용을 활용한다.

디스크 공간은 보통 테이블을 저장하는 데 필요한 디스크 공간보다 작다.

왜냐하면 보통 인덱스는 KEY-ROWID만 가지고 있고, 테이블의 세부항목들은 갖고 있지 않기 때문이다.

Index 작동 원리
SELECT *
FROM EMP
WHERE empno=7902;
데이터 파일의 블록이 10만개 일 때, 위 SQL문을 수행시에

서버 프로세스가 파싱 과정을 마친 후 DB buffer cache에 empno 가 7902인 정보가 있는지 확인한다.

정보가 없으면 하드 디스크 파일에서 7902정보를 가진 블록을 복사해서 DB buffer cache로 가져온 후 7900 정보만 골라내서 사용자에게 보여줌

이 때 두 가지 경우로 나눌 수 있는데,

Index 없는 경우 : 7902정보가 어떤 블록에 들어 있는지 모르므로 10만개 전부 db buffer cache로 복사한 후 하나하나 찾는다.

Index 있는 경우 : where 절의 컬럼이 index가 만들어져 있는지 확인 후, 인덱스에 먼저 가서 7902정보가 어떤 ROWID를 가지고 있는지 확인한 후 해당 ROWID에 있는 블록만 찾아가서 db buffer cache에 복사함.

DML이 일어났을 때의 상황
INSERT
기존 Block에 여유가 없을 때, 새로운 Data가 입력된다.

→ 새로운 Block을 할당 받은 후, Key를 옮기는 작업을 수행한다.

→ Index split 작업 동안, 해당 Block의 Key 값에 대해서 DML이 블로킹 된다.
대기 이벤트 발생


DELETE
<Table과 Index 상황 비교>

Table에서 data가 delete 되는 경우
Data가 지워지고, 다른 Data가 그 공간을 사용 가능하다.

Index에서 Data가 delete 되는 경우
Data가 지워지지 않고, 사용 안 됨 표시만 해둔다.
Table의 Data 수와 Index의 Data 수가 다를 수 있다.


UPDATE
Table에서 update가 발생하면 → Index는 Update 할 수 없다.
Index에서는 Delete가 발생한 후, 새로운 작업의 Insert 작업 / 2배의 작업이 소요되어 힘들다.


Index 종류
인덱스에는 크게 Clustered와 NonClustered 인덱스로 나눌 수 있다.

Clustered 인덱스
Clustered 인덱스는 물리적 정렬로 DB에 데이터를 입력 시 Clustered 인덱스를 기준으로 입력이 된다.

따라서 한 테이블에 오직 하나만 존재 할 수 있으며 Table을열었을 때 Order By를 사용하지 않아도 데이터가 Clustered 인덱스에 따라 정렬이 되어 있는 것을 확인 할 수있다.

물리적으로 정렬이 되어 있는 만큼 가장 빠른 처리를 한다.

NonClustered 인덱스
NonClustered 인덱스는 clustered 인덱스와는 달리 중복된 값을 가지면 한 테이블에 여러 개를 생성 할 수 있다.

자동 정렬되지 않고, Index를 생성할 때는 Clustered가 되어있을 때, Index Scan이 유리하다.


Index 장점
키 값을 기초로 하여 테이블에서 검색과 정렬 속도를 향상시킨다.
질의나 보고서에서 그룹화 작업의 속도를 향상시킨다.
인덱스를 사용하면 테이블 행의 고유성을 강화시킬 수 있다.
테이블의 기본 키는 자동으로 인덱스가 된다.

Index 단점
Index 생성시 .mdb 파일 크기가 증가한다.
한 페이지를 동시에 수정할 수 있는 병행성이 줄어든다.
Index 된 Field에서 Data를 업데이트하거나, Record를 추가 또는 삭제시 성능이 떨어진다.
데이터 변경 작업이 자주 일어나는 경우, Index를 재작성해야 하므로 성능에 영향을 미친다.
Index를 생성하는데 시간이 많이 소요될 수 있다.
Index가 데이터베이스 공간을 차지해 추가적인 공간이 필요해진다.
DB의 10퍼센트 내외의 공간이 추가로 필요
Index를 남발하지 말아야 하는 이유
데이터베이스 서버에 성능문제가 발생하면 가장 빨리 생각하는 해결책이 인덱스 추가 생성이다.
문제가 발생할때마다 인덱스를 생성하면서 인덱스가 쌓여가는 것은 하나의 쿼리문을 빠르게는 만들 수 있지만 전체적인 데이터베이스의 성능 부하를 초래한다.

조회 성능을 극대화하려 만든 객체인데 많은 인덱스가 쌓여서 Insert, Delete, Update시에 부하가 발생해 전체적인 데이터베이스 성능을 저하한다.

그렇기에 인덱스를 생성하는것 보다는 SQL문을 좀 더 효율적으로 짜는 방향으로 나가야한다.
인덱스 생성은 마지막 수단으로 강구해야 할 문제이다.


Index 사용 전 명시 사항
where절에서 자주 사용하는 컬럼에 사용한다.

like '%~'는 조심해야 한다. %는 뒤에만 사용하도록 해야한다.
(table scan이여서 성능 감소)

between A and B (Clustered Index가 유리)
범위 쿼리문에서는 클러스터 인덱스가 유리하지만 클러스터 인덱스는 테이블 당 1개만 가질 수 있다는 단점 존재

order by에 항상 또는 자주 사용되는 컬럼에 사용한다.

join으로 자주 사용되는 컬럼에 사용한다.

Foreign key (1:1 매핑)이 많을 때 -> Clustered, NonClustered Index 둘 다 상관 없다.
상황에 따라 Clustered Index사용

Foreign key (1:N 매핑)이 많을 때 -> Clustered Index 사용한다.

100만건의 데이터 중 10건의 데이터 조회 -> 찾는 건이 적은 컬럼에 Index를 사용한다.
상책중복이 많은 컬럼 (EX:성별)에는 Index를 거는 것이 아니다.
조회되는 건 수가 많으면 인덱스를 걸지 않고 Table Scan이 더 나은편이다.

not 연산자는 긍정문으로 변경

Insert, Delete 등 데이터의 변경(DML)이 많은 컬럼은 인덱스를 걸지 않은 편이 좋다.

인덱스를 만드는데 시간과 저장공간이 소비되고 만들고 난 후에도 추가적인 공간이 필요하다..

데이터를 변경(Insert, Update, Delete)를 하면 인덱스를 다시 조정해야하기 때문에 자원이 많이 소모된다.
특히나 Insert 연산


Index 사용 예제
인덱스 생성
CREATE INDEX [인덱스명] ON [테이블명](컬럼1, 컬럼2, 컬럼3.......);
EX> CREATE INDEX EX_INDEX ON CUSTOMERS(NAME,ADDRESS); 
// UNIQUE 키워드를 붙이면 컬럼값에 중복값을 허용하지 않는다는 뜻
EX> CREATE[UNIQUE] INDEX EX_INDEX ON CUSTOMERS(NAME,ADDRESS); 

ALTER TABLE  테이블명 ADD INDEX(필드명(크기));

CREATE TABLE 테이블 명 ( 필드명 데이터타입(데이터크기), INDEX(필드명(크기)) ENGINE MyISAM; 
필드 중에는 데이터 형식 때문에 인덱스가 될 수 없는 필드도 있다.

여러 필드로 이루어진(다중 필드) 인덱스를 사용하면 첫 필드 값이 같은 레코드도 구분할 수 있다.

참고로 액세스에서 다중 필드 인덱스는 최대 10개의 필드를 포함할 수 있다.

인덱스 삭제
DROP INDEX [인덱스 명]
인덱스 확인
SHOW INDEX FROM 테이블이름
Index Rebuild
인덱스를 리빌드하는 이유
인덱스 파일은 생성 후 Insert, Update, Delete등을 반복하다보면 성능이 저하된다.
생성된 인덱스는 트리구조를 가지는데, 삽입,수정,삭제등이 오랫동안 일어나다보면 트리의 한쪽이 무거워져 전체적으로 트리의 깊이가 깊어지기 때문이다.
이러한 현상으로 인해 인덱스의 검색속도가 떨어지므로 주기적으로 리빌딩하는 작업을 거치는것이 좋다.

Index 트리의 깊이가 4이상인 Index를 조회하는 쿼리
SELECT I.TABLESPACE_NAME,I.TABLE_NAME,I.INDEX_NAME, I.BLEVEL,
       DECODE(SIGN(NVL(I.BLEVEL,99)-3),1,DECODE(NVL(I.BLEVEL,99),99,'?','Rebuild'),'Check') CNF
FROM   USER_INDEXES I
WHERE   I.BLEVEL > 4
ORDER BY I.BLEVEL DESC
해당 쿼리문을 실행하여 검색되는 Index는 리빌딩을 하는것이 좋다.

인덱스 리빌드
ALTER INDEX [인덱스명] REBUILD;
전체 인덱스 리빌드 쿼리문 만들기
SELECT 'ALTER INDEX '||INDEX_NAME||' REBUILD; 'FROM USER_INDEXES;
```