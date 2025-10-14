# SWYP Web 11기 Team 4 Backend

Spring Boot 3.5.6 + Java 17 기반의 백엔드 프로젝트입니다.

## 🚀 기술 스택 (Technology Stack)

- **Language**: Java 17
- **Framework**: Spring Boot 3.5.6
- **Build Tool**: Gradle
- **Database**: JPA, H2 (Test), PostgreSQL 16.x
- **Documentation**: Swagger/OpenAPI
- **Code Quality**:
  - Spotless (Code Formatting)
  - Jacoco (Code Coverage)
  - SonarQube (Code Analysis)

## 🏗️ 아키텍처 (Architecture)

이 프로젝트는 **모놀리식 아키텍처**로 구현되며, 도메인별로 계층화된 패키지 구조를 따릅니다:

```
com.swygbro/airoad/backend/
├── common/                  # 공통 모듈
│   ├── config/             # 설정 클래스 (Swagger, JPA Auditing 등)
│   ├── domain/
│   │   ├── dto/           # 공통 DTO
│   │   └── entity/        # BaseEntity 등 공통 엔티티
│   ├── exception/          # 예외 처리
│   │   ├── ErrorCode.java           # 에러 코드 인터페이스
│   │   ├── BusinessException.java   # 비즈니스 예외 클래스
│   │   └── CommonErrorCode.java     # 공통 에러 코드 enum
│   └── presentation/       # GlobalExceptionHandler 등 공통 컨트롤러
│
└── <domain>/               # 도메인별 패키지 (예: example)
    ├── presentation/       # 컨트롤러 (REST API 엔드포인트)
    ├── application/        # 유스케이스 및 비즈니스 로직 서비스
    ├── domain/
    │   ├── dto/           # 데이터 전송 객체
    │   └── entity/        # 도메인 엔티티
    ├── exception/          # 도메인별 에러 코드
    │   └── <Domain>ErrorCode.java  # ErrorCode 인터페이스 구현 enum
    └── infrastructure/     # 리포지토리 및 외부 시스템 연동
```

**의존성 방향**: `presentation` → `application` → `domain` ← `infrastructure`

### 주요 패턴 (Key Patterns)

- **Use Case Interface Pattern**: 각 도메인의 `application` 패키지에 `*UseCase` 인터페이스를 정의하고, `*Service` 클래스에서 구현합니다.
- **공통 Base Entity**: 모든 엔티티는 `BaseEntity`를 상속받아 생성일시/수정일시 필드를 자동 관리합니다.
- **구조화된 예외 처리**: `ErrorCode` 인터페이스와 `BusinessException`을 통해 일관된 예외 처리를 제공합니다.
  - `ErrorCode`: 에러 코드, HTTP 상태, 메시지를 정의하는 인터페이스
  - `BusinessException`: 비즈니스 로직 예외를 처리하는 공통 예외 클래스
  - `CommonErrorCode`: 애플리케이션 전반에서 사용되는 공통 에러 코드 enum
  - 각 도메인은 `exception` 패키지에 `ErrorCode` 인터페이스를 구현한 `<Domain>ErrorCode` enum을 정의하여 도메인별 에러 코드 관리

## 🏁 시작하기 (Getting Started)

### 사전 요구 사항 (Prerequisites)

- Java 17
- IDE (IntelliJ IDEA 권장)

### 설치 및 실행 (Installation and Run)

1. **저장소 복제 (Clone the repository)**
   ```shell
   git clone https://github.com/swyp-web-11-team-4/airoad-backend.git
   cd backend
   ```

2. **프로젝트 빌드 (Build the project)**
   ```shell
   ./gradlew build
   ```

3. **애플리케이션 실행 (Run the application)**
   ```shell
   ./gradlew bootRun
   ```

4. **Swagger UI 접속 (개발 환경)**
   - 애플리케이션 실행 후 `http://localhost:8080/swagger-ui.html` 접속

## 🛠️ 주요 명령어 (Available Commands)

### 빌드 및 실행
```shell
# 프로젝트 빌드 및 테스트 실행
./gradlew build

# 애플리케이션 로컬 실행
./gradlew bootRun

# 테스트만 실행
./gradlew test
```

### 코드 품질 관리
```shell
# 코드 포맷팅 적용 (커밋 전 필수!)
./gradlew spotlessApply

# 코드 스타일 검사
./gradlew spotlessCheck

# 코드 커버리지 리포트 생성
./gradlew jacocoTestReport
# 리포트 위치: build/reports/jacoco/test/html/index.html

# 커버리지 임계값 검증
./gradlew jacocoTestCoverageVerification

# 전체 품질 검사 (커버리지 + 포맷팅)
./gradlew check

# SonarQube 분석
./gradlew sonar
```

## ✨ 코드 스타일 (Code Style)

이 프로젝트는 [Spotless](https://github.com/diffplug/spotless)와 **Google Java Format**을 사용하여 코드 스타일을 강제합니다.

**⚠️ 커밋 전 필수**: CI/CD에서 코드 스타일을 검사하므로, 커밋 전 반드시 `./gradlew spotlessApply`를 실행하세요.

## 📝 커밋 규칙 (Commit Conventions)

이 프로젝트는 [Conventional Commits](https://www.conventionalcommits.org/ko/v1.0.0/)와 **Semantic Release**를 사용합니다.

### 커밋 메시지 형식
```
<type>: <subject>
```

### 주요 타입
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

**참고**: `feat`, `fix` 타입은 자동으로 버전을 업데이트하고 CHANGELOG.md에 기록됩니다.

자세한 내용은 [CONTRIBUTING.md](docs/CONTRIBUTING.md)를 참조하세요.

## 🔄 CI/CD Pipeline

`.github/workflows/ci.yml`에 정의된 자동화 파이프라인:

1. **Test & Build**: 테스트 실행 및 커버리지 생성, PR에 커버리지 리포트 자동 코멘트
2. **SonarQube Analysis**: 코드 품질 분석 및 Quality Gate 체크
3. **Semantic Release**: `main` 브랜치 푸시 시 자동 버전 관리, CHANGELOG 생성, gradle.properties 버전 업데이트
4. **Docker Build & Push**: 릴리즈 생성 시 Docker 이미지 빌드 및 ghcr.io에 푸시

## 🧪 테스트 (Testing)

- **테스트 위치**: `src/test/java/com/swygbro/airoad/backend/`
- **테스트 DB**: H2 (in-memory)
- **커버리지 리포트**: `build/reports/jacoco/test/html/index.html`
- **커버리지 기준**:
  - 전체 프로젝트: 60% 이상 (LINE/BRANCH)
  - 개별 클래스: 60% 이상 (LINE)

## 📦 빌드 구성 (Build Configuration)

### 커스텀 Gradle 플러그인
- `plugin.coverage`: Jacoco 설정 및 커버리지 검증
- `plugin.spotless`: 코드 포맷팅 규칙
- `plugin.sonar`: SonarQube 연동

### 의존성 관리
모든 의존성은 `gradle/libs.versions.toml`에서 중앙 관리됩니다.
