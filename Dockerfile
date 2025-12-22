# Gradle로 Spring Boot JAR 빌드
FROM gradle:8.10.0-jdk21 AS builder

WORKDIR /app

# Gradle 설정 파일 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle gradle
COPY gradlew .
RUN chmod +x gradlew

# 종속성 설치
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드(테스트 제외)
RUN ./gradlew bootJar --no-daemon -x test

# 2단계: 실행용 이미지
FROM eclipse-temurin:21-jre
WORKDIR /app

# --- 도플러 CLI 설치 추가 ---
RUN apt-get update && apt-get install -y curl gnupg && \
    (curl -Ls https://cli.doppler.com/install.sh || wget -qO- https://cli.doppler.com/install.sh) | sh
# -------------------------

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

CMD ["sh", "-c", "doppler run -- java -Dspring.profiles.active=prod -jar app.jar"]