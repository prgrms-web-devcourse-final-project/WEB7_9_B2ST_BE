# 1단계: Gradle로 Spring Boot JAR 빌드
FROM gradle:8.10.0-jdk21 AS builder

WORKDIR /app

# Gradle 설정 파일 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle gradle
COPY gradlew .
RUN chmod +x gradlew

# 종속성 설치 (캐시 활용)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드(테스트 제외)
RUN ./gradlew bootJar --no-daemon -x test


# 2단계: 실행용 이미지
FROM eclipse-temurin:21-jre

WORKDIR /app

# 빌드 결과물 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

# 컨테이너는 env로 주입된 값을 그대로 사용해서 실행
# (SPRING_PROFILES_ACTIVE=prod 는 docker-compose.yml에서 이미 설정 중)
CMD ["java", "-jar", "app.jar"]
