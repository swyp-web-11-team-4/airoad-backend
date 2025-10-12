# 개발 환경 세팅 가이드

## 필수 요구사항

### 소프트웨어 버전
- **Java**: 17 이상
- **IDE**: IntelliJ IDEA 권장
- **Git**: 최신 버전

## 프로젝트 클론

```bash
git clone https://github.com/swyp-web-11-team-4/airoad-backend.git
cd airoad-backend
```

## IDE 설정

### IntelliJ IDEA

1. **프로젝트 열기**
   - `File` → `Open` → 프로젝트 루트 디렉토리 선택
   - Gradle 프로젝트로 자동 인식됨

2. **Java SDK 설정**
   - `File` → `Project Structure` → `Project`
   - SDK를 Temurin Java 17로 설정
   - Language level을 17로 설정

3. **Gradle 설정**
   - `File` → `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Gradle`
   - Gradle JVM을 Java 17로 설정

4. **Code Style 설정**
   - 프로젝트에서 Google Java Format을 사용합니다
   - `File` → `Settings` → `Editor` → `Code Style` → `code-style/intellij-java-google-style.xml` 선택 후 적용

## 프로젝트 빌드

### 첫 빌드
```bash
./gradlew clean build
```

빌드가 성공하면 다음 작업이 수행됩니다:
- 의존성 다운로드
- 소스 컴파일
- 테스트 실행
- JAR 파일 생성 (`build/libs/`)

### 빌드 문제 해결
- **의존성 오류**: `./gradlew --refresh-dependencies clean build`
- **캐시 문제**: `rm -rf ~/.gradle/caches` 후 재빌드

## 애플리케이션 실행

### Gradle로 실행
```bash
./gradlew bootRun
```

### IDE에서 실행
1. `src/main/java/com/swygbro/airoad/backend/AiroadBackendApplication.java` 열기
2. `main` 메서드 왼쪽의 실행 버튼 클릭
3. `Run 'AiroadBackendApplication'` 선택

### 실행 확인
- 애플리케이션이 정상적으로 시작되면: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health Check: `http://localhost:8080/actuator/health` (설정된 경우)

## 개발 워크플로우

### 1. 코드 작성

### 2. 코드 포맷팅 (커밋 전 필수!)
```bash
./gradlew spotlessApply
```

### 3. 테스트 실행
```bash
./gradlew test
```

### 4. 커버리지 확인
```bash
./gradlew jacocoTestReport
```
리포트 위치: `build/reports/jacoco/test/html/index.html`

### 5. 전체 품질 검사
```bash
./gradlew check
```

### 작업 시작
```bash
git checkout main
git pull origin main
git checkout -b feature/your-feature-name
```

### 작업 완료
```bash
./gradlew spotlessApply
./gradlew check
git add .
git commit -m "feat: your feature description"
git push origin feature/your-feature-name
```

## 커밋 메시지 규칙

Conventional Commits 형식을 따릅니다:

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

**예시**:
```bash
git commit -m "feat: 사용자 로그인 API 추가"
git commit -m "fix: 비밀번호 검증 로직 수정"
git commit -m "test: UserService 테스트 추가"
```

## 테스트 커버리지 목표
- 전체: 60% 이상 (LINE/BRANCH)
- 개별 클래스: 60% 이상 (LINE)

## 유용한 Gradle 명령어

```bash
# 빌드 및 테스트
./gradlew build              # 전체 빌드
./gradlew build -x test      # 테스트 제외 빌드
./gradlew test               # 테스트만 실행
./gradlew clean              # 빌드 결과 삭제

# 코드 품질
./gradlew spotlessApply      # 코드 포맷팅
./gradlew spotlessCheck      # 포맷팅 검사
./gradlew check              # 전체 품질 검사
./gradlew jacocoTestReport   # 커버리지 리포트

# 애플리케이션 실행
./gradlew bootRun            # 애플리케이션 실행

# 의존성 관리
./gradlew dependencies       # 의존성 트리 확인
./gradlew dependencyUpdates  # 업데이트 가능한 의존성 확인
```

## 자주 발생하는 문제

### 빌드 실패
- **Java 버전 불일치**: Java 17인지 확인 (`java -version`)
- **Gradle 캐시 문제**: `./gradlew clean` 실행
- **의존성 문제**: `./gradlew --refresh-dependencies`

### 테스트 실패
- **DB 관련 오류**: H2 in-memory DB를 사용하는지 확인
- **Mock 설정 오류**: `@ExtendWith(MockitoExtension.class)` 확인

### 포맷팅 오류
- CI에서 실패 시: `./gradlew spotlessApply` 실행 후 커밋

## 추가 참고 문서

- [기여 가이드](CONTRIBUTING.md)
