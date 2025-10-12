# Stage 1: 빌드 스테이지
FROM gradle:8.14.3-jdk17-alpine AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 설정 파일 복사
COPY build.gradle settings.gradle gradle.properties ./
COPY buildSrc ./buildSrc
COPY gradle ./gradle
COPY gradlew ./

# 코드 스타일 파일 복사
COPY code-style ./code-style

# 실행 권한 부여
RUN chmod +x gradlew

# 의존성 다운로드
RUN ./gradlew dependencies --no-daemon --quiet


# 소스 코드 복사
COPY src ./src

# 애플리케이션 빌드
RUN ./gradlew build -x test -x spotlessCheck --no-daemon --parallel

# Stage 2: 런타임 스테이지
FROM eclipse-temurin:17-jre-jammy

# 애플리케이션 사용자 생성
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 작업 디렉토리 설정
WORKDIR /app

# JAR 파일 복사 (빌드 스테이지에서)
COPY --from=builder /app/build/libs/*.jar app.jar

# 비특권 사용자로 실행
USER appuser

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]