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
`@SoftDelete`를 통해 논리적 삭제를 지원하며, `@PreRemove`로 `deletedAt` 필드가 자동 설정됩니다.

```java
@Entity
public class Example extends BaseEntity {
  // createdAt, updatedAt, deletedAt 필드는 BaseEntity에서 상속됨
}
```

**중요**: JPA Auditing을 사용하므로 `@EnableJpaAuditing`이 `JpaConfig`에 설정되어 있어야 합니다.

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

```java
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ExampleServiceTest {

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
