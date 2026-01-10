# TT Backend 배포 프로세스 문서

## 개요

이 문서는 TT Backend 애플리케이션의 자동화된 배포 프로세스를 설명합니다. GitHub Actions를 통해 트리거되며, Docker 컨테이너화된 Spring Boot 애플리케이션과 모니터링 스택을 AWS EC2 인스턴스에 배포합니다.

---

## 배포 트리거

### 자동 배포 조건

배포는 `develop` 브랜치에 다음 경로의 파일이 변경되어 푸시될 때 자동으로 실행됩니다:

- `.github/workflows/**` - GitHub Actions 워크플로우
- `src/**` - 소스 코드
- `build.gradle`, `settings.gradle` - Gradle 빌드 설정
- `build.gradle.kts`, `settings.gradle.kts` - Kotlin DSL 빌드 설정
- `gradle/**` - Gradle wrapper 파일
- `gradlew`, `gradlew.bat` - Gradle wrapper 실행 파일
- `Dockerfile` - Docker 이미지 빌드 설정
- `docker/**` - Docker 관련 파일

---

## 배포 파이프라인

배포 프로세스는 3단계로 구성됩니다:

```
1. makeTagAndRelease (태그 생성 및 릴리스)
   ↓
2. buildImageAndPush (Docker 이미지 빌드 및 푸시)
   ↓
3. deploy (EC2 인스턴스 배포)
```

---

## 1단계: makeTagAndRelease

### 목적
자동으로 버전 태그를 생성하고 GitHub Release를 만듭니다.

### 프로세스
1. **태그 생성**: `mathieudutour/github-tag-action` 사용
    - Conventional Commits 기반 자동 버전 증가
    - 변경 로그 자동 생성

2. **릴리스 생성**: `actions/create-release` 사용
    - 생성된 태그로 GitHub Release 발행
    - 변경 로그를 릴리스 노트로 포함

### 출력
- `tag_name`: 생성된 태그 이름 (다음 단계에서 사용)

---

## 2단계: buildImageAndPush

### 목적
Spring Boot 애플리케이션의 Docker 이미지를 빌드하고 GitHub Container Registry(GHCR)에 푸시합니다.

### Docker 이미지 빌드 과정

#### Dockerfile 구조 (멀티 스테이지 빌드)

**Stage 1: Builder**
```dockerfile
FROM gradle:8.10.0-jdk21-alpine AS builder
```
- Gradle 8.10.0과 JDK 21을 사용한 빌드 환경
- 의존성 캐싱 레이어 최적화
- `bootJar` 태스크로 실행 가능한 JAR 생성 (테스트 제외)

**Stage 2: Runtime**
```dockerfile
FROM eclipse-temurin:21-jre-alpine
```
- 최소화된 JRE 이미지 사용 (빌드 도구 미포함)
- 비-root 유저(`spring`) 생성 및 실행 (보안)
- 컨테이너 최적화된 JVM 옵션 적용

### JVM 최적화 설정

```bash
-XX:+UseContainerSupport        # 컨테이너 환경 인식
-XX:MaxRAMPercentage=75.0       # 최대 힙 메모리 75%
-XX:InitialRAMPercentage=50.0   # 초기 힙 메모리 50%
-XX:+UseG1GC                    # G1 가비지 컬렉터 사용
-XX:+DisableExplicitGC          # 명시적 GC 호출 비활성화
```

### 이미지 푸시

- **레지스트리**: `ghcr.io/chehyeon-kim23/tt_backend`
- **태그**:
    - `<version>`: 릴리스 버전 태그 (예: v1.2.3)
    - `latest`: 최신 이미지 태그
- **캐시 전략**:
    - 레지스트리 기반 캐시 사용 (빌드 속도 향상)
    - Cache tag: `cache`

---

## 3단계: deploy

### 목적
AWS EC2 인스턴스에 애플리케이션과 인프라를 배포합니다.

### 배포 대상 인스턴스
- **인스턴스 태그**: `TT-ec2-1`
- **상태**: `running`
- **명령 실행**: AWS Systems Manager (SSM) Send-Command

### 배포 스크립트 상세

#### 3.1 환경 설정
```bash
export HOME=/root
export PATH=$PATH:/usr/local/bin
git config --global --add safe.directory /dockerProjects/tt-src/WEB7_9_B2ST_BE
```

#### 3.2 소스 코드 동기화
```bash
cd /dockerProjects/tt-src/WEB7_9_B2ST_BE/
git fetch --all
git reset --hard origin/develop
```
- 원격 저장소의 최신 `develop` 브랜치로 강제 동기화

#### 3.3 보안 토큰 관리

**Doppler Token (환경 변수 관리)**
```bash
export DOPPLER_TOKEN="$(sudo tr -d "\r\n" < /etc/tt-secrets/doppler-token)"
export DOPPLER_PROJECT=tt
export DOPPLER_CONFIG=prd
```
- Doppler를 통해 안전하게 환경 변수 주입
- Trailing newline/CRLF 제거로 안정성 확보
- EC2 파일 시스템에 유일하게 저장되는 시크릿 (Doppler 접근용)

**GitHub Token (컨테이너 레지스트리 인증)**
```bash
doppler run --project "$DOPPLER_PROJECT" --config "$DOPPLER_CONFIG" -- bash -c "echo \$GITHUB_TOKEN | docker login ghcr.io -u <actor> --password-stdin"
```
- Doppler에서 GITHUB_TOKEN 환경 변수로 주입
- EC2 파일 시스템에 평문 저장 안 함 (보안 강화)
- SSM 로그에 토큰 값 노출 방지

#### 3.4 Alertmanager 설정 치환
```bash
doppler run -- bash -lc "envsubst < monitoring/alertmanager/alertmanager.yml > /tmp/alertmanager-resolved.yml"
cp /tmp/alertmanager-resolved.yml monitoring/alertmanager/alertmanager.yml
rm -f /tmp/alertmanager-resolved.yml
```
- 환경 변수(예: Slack Webhook)를 설정 파일에 주입
- 임시 파일 사용 후 삭제

#### 3.5 컨테이너 배포
```bash
doppler run -- docker compose pull
doppler run -- docker compose up -d --force-recreate
```
- 모든 Docker Compose 명령을 Doppler 환경에서 실행
- 환경 변수 일관성 보장

#### 3.6 정리 작업
```bash
docker image prune -f
docker logout ghcr.io
```
- 미사용 이미지 제거 (디스크 공간 확보)
- 레지스트리 로그아웃 (보안)

---

## 인프라 구성 (Docker Compose)

배포되는 전체 스택은 다음과 같이 구성됩니다:

### 데이터베이스 계층

#### PostgreSQL
- **이미지**: `postgres:16-alpine`
- **포트**: `5432`
- **데이터 영속성**: `postgres_data` 볼륨
- **Healthcheck**: `pg_isready` 명령
- **환경 변수**:
    - `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`
    - UTF-8 인코딩 강제

#### Redis Cluster (6 노드)
- **이미지**: `redis:7-alpine`
- **노드 구성**:
    - Master: 3개 (7000-7002)
    - Replica: 3개 (7003-7005)
- **클러스터 모드**: 활성화
- **인증**: `REDIS_PASSWORD` 필수
- **데이터 영속성**:
    - AOF(Append Only File) 활성화
    - RDB 스냅샷 (900초/1건, 300초/10건, 60초/10000건)

##### Redis Cluster 초기화 (`init-cluster.sh`)
```bash
redis-cli --cluster create \
  redis-node-1:7000 ... redis-node-6:7005 \
  --cluster-replicas 1 \
  --cluster-yes
```
- 자동으로 마스터/복제본 할당
- 클러스터 상태 확인 후 중복 초기화 방지
- 모든 노드 healthcheck 완료 후 실행

### 애플리케이션 계층

#### Spring Boot App
- **이미지**: `ghcr.io/chehyeon-kim23/tt_backend:latest`
- **포트**: `8080`
- **프로필**: `prod`
- **의존성**:
    - PostgreSQL (healthcheck 대기)
    - Redis Cluster (초기화 완료 대기)

**환경 변수 카테고리**:
1. **데이터베이스**: Spring 표준 + 커스텀 키 (호환성)
2. **Redis**: 클러스터 모드 노드 목록
3. **메일**: SMTP 설정 (Gmail 등)
4. **AWS**: S3 버킷, 리전 정보
5. **보안**: JWT 시크릿, 만료 시간
6. **OAuth**: Kakao 로그인 설정
7. **알림**: Slack Webhook
8. **JPA**: DDL 자동 생성 모드

**Healthcheck**:
```bash
wget --quiet --tries=1 --spider http://localhost:8080/actuator/health
```
- Spring Boot Actuator 헬스 엔드포인트
- 60초 시작 유예 시간

### 모니터링 스택

#### Prometheus
- **이미지**: `prom/prometheus:v3.8.1`
- **포트**: `9090`
- **기능**:
    - 메트릭 수집 및 저장
    - 알림 규칙 평가
    - 15일 데이터 보존
- **스크랩 대상**:
    - Spring Boot App (`/actuator/prometheus`)
    - Redis Exporter
    - PostgreSQL Exporter

#### Grafana
- **이미지**: `grafana/grafana:12.3.0`
- **포트**: `3001` (호스트) → `3000` (컨테이너)
- **기능**:
    - 대시보드 시각화
    - Prometheus 데이터 소스 자동 프로비저닝
    - 사전 구성된 대시보드
- **인증**: Admin 계정 (비밀번호는 환경 변수)

#### Alertmanager
- **이미지**: `prom/alertmanager:v0.30.0`
- **포트**: `9093`
- **기능**:
    - 알림 라우팅 및 그룹화
    - Slack 알림 전송
    - 중복 알림 억제

**설정 파일 주입**:
```yaml
# alertmanager.yml에서 환경 변수 사용
slack_api_url: ${SLACK_WEBHOOK_AUTH}
```
- Doppler 환경에서 `envsubst` 실행
- 민감 정보 파일에 저장 안 함

#### Redis Exporter
- **이미지**: `oliver006/redis_exporter:v1.80.1`
- **포트**: `9121`
- **대상**: `redis-node-1:7000` (대표 노드)
- **메트릭**: 커넥션, 메모리, 키스페이스 등

#### PostgreSQL Exporter
- **이미지**: `prometheuscommunity/postgres-exporter:v0.15.0`
- **포트**: `9187`
- **연결**: `DATA_SOURCE_NAME` (PostgreSQL DSN)
- **메트릭**: 쿼리 성능, 테이블 통계, 연결 풀 등

---

## 네트워크 및 볼륨

### 네트워크
- **이름**: `common`
- **드라이버**: `bridge`
- **목적**: 모든 서비스 간 통신

### 영속 볼륨
```yaml
volumes:
  postgres_data          # PostgreSQL 데이터
  redis-node-1-data      # Redis 노드 1-6 데이터
  redis-node-2-data
  redis-node-3-data
  redis-node-4-data
  redis-node-5-data
  redis-node-6-data
  prometheus_data        # 메트릭 데이터
  grafana_data           # 대시보드 설정
  alertmanager_data      # 알림 상태
```

---

## 환경 변수 관리

### Doppler 사용 이유
1. **중앙 집중화**: 모든 환경 변수를 Doppler에서 관리
2. **버전 관리**: 변경 이력 추적
3. **보안**: 평문 노출 없이 주입
4. **일관성**: 모든 명령에서 동일한 환경 보장

### 환경 변수 흐름
```
Doppler Cloud (모든 시크릿 중앙 관리)
    ↓
EC2 Instance (/etc/tt-secrets/doppler-token) ← 유일한 EC2 파일 시크릿
    ↓
doppler run -- docker compose (환경 변수 주입)
    ↓
Container Environment
```

### Doppler에서 관리하는 시크릿
- **GITHUB_TOKEN**: GitHub Container Registry 인증
- **POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_DB**: 데이터베이스 인증
- **REDIS_PASSWORD**: Redis 클러스터 인증
- **MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD**: 이메일 설정
- **AWS_REGION, AWS_S3_BUCKET**: AWS 리소스 설정
- **JWT_SECRET, JWT_ACCESS_EXPIRATION, JWT_REFRESH_EXPIRATION**: JWT 토큰 설정
- **KAKAO_CLIENT_ID, KAKAO_CLIENT_SECRET**: OAuth 설정
- **SLACK_WEBHOOK_AUTH**: Slack 알림 Webhook
- **GRAFANA_PASSWORD**: Grafana 관리자 비밀번호
- 기타 모든 애플리케이션 환경 변수

### EC2 파일 시스템에 저장되는 시크릿
- **DOPPLER_TOKEN**: Doppler API 접근 토큰 (`/etc/tt-secrets/doppler-token`)
    - 이 토큰만 파일로 저장 (Doppler 접근에 필요)
    - 권한: 400 (root만 읽기 가능)
    - 다른 모든 시크릿은 Doppler에서 관리

---

## 보안 고려사항

### 1. 비-root 컨테이너 실행
```dockerfile
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
```

### 2. 토큰 관리
- **GitHub Token**: Doppler에서 관리, EC2 파일 시스템에 저장 안 함
- **Doppler Token**: EC2 파일에서 읽기 전용 접근 (`/etc/tt-secrets/doppler-token`)
- **환경 변수**: Doppler를 통해 안전하게 주입, 평문 노출 없음
- **SSM 로그**: 실제 토큰 값이 AWS 로그에 기록되지 않음

### 3. 네트워크 격리
- 모든 서비스는 `common` 네트워크 내부에서만 통신
- 필요한 포트만 호스트에 노출

### 4. 인증
- Redis: `requirepass` + `masterauth`
- PostgreSQL: 사용자/비밀번호
- Grafana: Admin 계정 비밀번호

---

## 헬스체크 전략

모든 서비스는 헬스체크를 통해 의존성 순서를 보장합니다:

```
PostgreSQL (healthy)
    ↓
Redis Cluster Nodes (all healthy)
    ↓
Redis Cluster Init (completed successfully)
    ↓
Spring Boot App (healthy)
    ↓
Monitoring Stack (Prometheus, Grafana, Alertmanager)
```

**헬스체크 실패 시**:
- 컨테이너 재시작 (restart: always)
- 의존 서비스 대기 (depends_on conditions)

---

## 롤백 전략

### 자동 롤백 (없음)
현재 파이프라인은 자동 롤백을 지원하지 않습니다.

### 수동 롤백 방법
1. **이전 버전 태그로 이미지 변경**:
   ```bash
   docker pull ghcr.io/chehyeon-kim23/tt_backend:v1.2.2
   # docker-compose.yml 수정 또는 환경 변수 변경
   docker compose up -d --force-recreate app
   ```

2. **Git 리버트 후 재배포**:
   ```bash
   git revert <commit-hash>
   git push origin develop
   # GitHub Actions 자동 재배포
   ```

---

## 모니터링 및 알림

### Prometheus 메트릭
- **Spring Boot**: JVM, HTTP 요청, 데이터베이스 연결
- **Redis**: 메모리 사용량, 키 개수, 커넥션
- **PostgreSQL**: 쿼리 성능, 테이블 크기, 락

### Grafana 대시보드
- 프로비저닝된 대시보드: `/var/lib/grafana/dashboards`
- 데이터 소스: Prometheus (자동 구성)

### Alertmanager 알림
- **대상**: Slack Webhook
- **설정**: `monitoring/alertmanager/alertmanager.yml`
- **알림 예시**:
    - 애플리케이션 다운
    - 높은 메모리 사용량
    - 데이터베이스 연결 실패

---

## 트러블슈팅

### 1. 배포 실패
**증상**: SSM Send-Command 실패

**확인 사항**:
- EC2 인스턴스 상태 (`running`)
- SSM Agent 실행 여부
- IAM 역할 권한 (SSM, ECR)

### 2. 컨테이너 시작 실패
**증상**: `docker compose up` 오류

**확인 사항**:
```bash
docker compose logs <service-name>
docker compose ps
```

**일반적인 원인**:
- 환경 변수 누락 (Doppler 토큰 확인)
- 포트 충돌
- 디스크 공간 부족

### 3. Redis Cluster 초기화 실패
**증상**: `redis-cluster-init` 컨테이너가 재시작 반복

**확인 사항**:
```bash
docker compose logs redis-cluster-init
redis-cli -h redis-node-1 -p 7000 -a <password> cluster info
```

**해결 방법**:
```bash
# 클러스터 초기화 재시도
docker compose restart redis-cluster-init

# 또는 전체 Redis 스택 재시작
docker compose down
docker volume rm <redis-volumes>
docker compose up -d
```

### 4. Healthcheck 타임아웃
**증상**: 서비스가 `unhealthy` 상태

**확인 사항**:
```bash
# Spring Boot 헬스 엔드포인트
curl http://localhost:8080/actuator/health

# PostgreSQL
docker compose exec postgres pg_isready

# Redis
docker compose exec redis-node-1 redis-cli -a <password> ping
```

### 5. Doppler 인증 실패
**증상**: `Invalid Auth token` 또는 `Unable to fetch secrets`

**확인 사항**:
```bash
# Doppler 토큰 파일 존재 확인
ls -la /etc/tt-secrets/doppler-token

# 토큰 파일 권한 확인 (400 또는 600이어야 함)
sudo chmod 400 /etc/tt-secrets/doppler-token

# Doppler 토큰 테스트
sudo bash -c 'export DOPPLER_TOKEN="$(tr -d "\r\n" < /etc/tt-secrets/doppler-token)" && doppler secrets --project tt --config prd'
```

**해결 방법**:
- Doppler 토큰이 만료된 경우: Doppler 대시보드에서 새 토큰 생성 후 파일 업데이트
- 파일 권한 문제: `sudo chmod 400 /etc/tt-secrets/doppler-token`

### 6. GitHub Container Registry 로그인 실패
**증상**: `unauthorized: authentication required`

**확인 사항**:
```bash
# Doppler에 GITHUB_TOKEN이 있는지 확인
sudo bash -c 'export DOPPLER_TOKEN="$(tr -d "\r\n" < /etc/tt-secrets/doppler-token)" && doppler secrets get GITHUB_TOKEN --project tt --config prd --plain'
```

**해결 방법**:
1. Doppler에 GITHUB_TOKEN 추가:
   ```bash
   doppler secrets set GITHUB_TOKEN=<your-pat> --project tt --config prd
   ```
2. GitHub PAT 권한 확인 (`packages:read`, `packages:write` 필요)
3. PAT가 만료된 경우 새로 생성하여 Doppler에 업데이트

---

## 성능 최적화

### 1. Docker 빌드 캐싱
- 레지스트리 기반 캐시 사용
- 의존성 레이어 분리 (변경 적음)

### 2. JVM 튜닝
- G1GC 사용 (낮은 지연)
- 컨테이너 메모리 인식
- Heap 크기 자동 조정

### 3. Redis 데이터 영속성
- AOF + RDB 조합
- 클러스터 모드로 고가용성

### 4. PostgreSQL 연결 풀
- Spring Boot 기본 HikariCP
- 환경 변수로 풀 크기 조정 가능

---

## 체크리스트

### 배포 전
- [ ] Doppler에 모든 환경 변수 설정 완료 (GITHUB_TOKEN 포함)
- [ ] `/etc/tt-secrets/doppler-token` 파일 존재 및 권한 확인 (400)
- [ ] EC2 인스턴스 상태 확인
- [ ] 디스크 공간 확인 (최소 10GB 여유)

### 배포 중
- [ ] GitHub Actions 워크플로우 성공
- [ ] Docker 이미지 GHCR에 푸시 확인
- [ ] SSM 명령 실행 성공

### 배포 후
- [ ] Spring Boot 애플리케이션 헬스체크 통과
- [ ] Prometheus 메트릭 수집 확인
- [ ] Grafana 대시보드 접근 가능
- [ ] Alertmanager Slack 알림 테스트

---

## 관련 문서

- **Doppler 문서**: https://docs.doppler.com
- **Doppler CLI**: https://docs.doppler.com/docs/cli
- **Doppler Service Tokens**: https://docs.doppler.com/docs/service-tokens
- **Docker Compose**: https://docs.docker.com/compose
- **Redis Cluster**: https://redis.io/topics/cluster-tutorial
- **Prometheus**: https://prometheus.io/docs
- **Grafana**: https://grafana.com/docs
- **GitHub PAT**: https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry

---

## 변경 이력

### 2025-01-11: Doppler 완전 통합
- GitHub Token을 EC2 파일에서 Doppler로 마이그레이션
- `/etc/tt-secrets/github-token` 파일 제거
- 모든 시크릿을 Doppler에서 중앙 관리
- 보안 강화: EC2 파일 시스템에 평문 시크릿 저장 최소화

---

## 연락처

배포 관련 문제 발생 시:
- GitHub Issues: 프로젝트 저장소
- Slack: Alertmanager 알림 채널