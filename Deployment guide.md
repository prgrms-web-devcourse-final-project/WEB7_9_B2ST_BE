# TT Backend 배포 가이드

> **최종 수정일**: 2026-01-10  
> **작성자**: Chehyeon-Kim 
> **버전**: 2.0

## 📋 목차

1. [배포 아키텍처](#배포-아키텍처)
2. [사전 요구사항](#사전-요구사항)
3. [환경 변수 관리](#환경-변수-관리)
4. [배포 프로세스](#배포-프로세스)
5. [롤백 절차](#롤백-절차)
6. [트러블슈팅](#트러블슈팅)
7. [모니터링](#모니터링)

---

## 🏗️ 배포 아키텍처

### 전체 흐름

```
개발자 Push (develop)
    ↓
GitHub Actions 트리거
    ↓
1. Tag & Release 생성
    ↓
2. Docker 이미지 빌드
    ↓
3. GHCR에 푸시
    ↓
4. AWS SSM으로 EC2 배포 명령
    ↓
5. EC2에서 Doppler로 환경변수 주입
    ↓
6. Docker Compose로 서비스 재시작
    ↓
배포 완료
```

### 인프라 구성

```
┌─────────────────────────────────────────┐
│           GitHub Actions                │
│  - 태그 생성                             │
│  - Docker 이미지 빌드                    │
│  - GHCR 푸시                            │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│         AWS EC2 (TT-ec2-1)              │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │   Docker Compose Stack          │   │
│  │                                 │   │
│  │  ├─ Spring Boot App             │   │
│  │  ├─ PostgreSQL 16               │   │
│  │  ├─ Redis Cluster (6 nodes)     │   │
│  │  ├─ Prometheus                  │   │
│  │  ├─ Grafana                     │   │
│  │  ├─ AlertManager                │   │
│  │  └─ Exporters                   │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
               ▲
               │
┌──────────────┴──────────────────────────┐
│            Doppler                      │
│  - 환경변수 중앙 관리                    │
│  - 민감정보 암호화 저장                  │
└─────────────────────────────────────────┘
```

---

## ✅ 사전 요구사항

### 1. GitHub Secrets 설정

다음 시크릿이 GitHub 레포지토리에 등록되어 있어야 합니다:

| Secret 이름 | 설명 | 예시 |
|------------|------|------|
| `PERSONAL_ACCESS_TOKEN` | GitHub PAT (repo, packages 권한) | `ghp_xxx...` |
| `DOPPLER_TOKEN` | Doppler Service Token | `dp.st.xxx...` |
| `AWS_REGION` | AWS 리전 | `ap-northeast-2` |
| `AWS_ACCESS_KEY_ID` | AWS IAM Access Key | `AKIA...` |
| `AWS_SECRET_ACCESS_KEY` | AWS IAM Secret Key | `xxx...` |

### 2. Doppler 프로젝트 설정

**프로젝트**: `tt`  
**환경**: `prd`

#### Doppler에 등록된 환경변수 목록

```bash
# Database
POSTGRES_USER=tt_user
POSTGRES_PASSWORD=secure_password_here
POSTGRES_DB=tt_database
POSTGRES_PORT=5432

# Redis
REDIS_PASSWORD=secure_redis_password

# Spring Boot
SPRING_PROFILES_ACTIVE=prod

# Monitoring
GRAFANA_PASSWORD=secure_grafana_password
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/xxx

# ... 기타 애플리케이션별 환경변수
```

### 3. EC2 인스턴스 요구사항

- **인스턴스 태그**: `Name=TT-ec2-1`
- **IAM 역할**: SSM 권한 필요
- **설치된 소프트웨어**:
    - Docker & Docker Compose
    - Doppler CLI
    - Git

---

## 🔐 환경 변수 관리

### Doppler 기반 환경변수 전략

#### 장점
✅ **중앙 집중식 관리**: 모든 환경변수를 Doppler에서 관리  
✅ **보안**: 암호화된 저장소, 접근 제어  
✅ **버전 관리**: 변경 이력 추적  
✅ **환경별 분리**: dev, staging, prod 독립 관리  
✅ **서버에 파일 미보관**: doppler.env는 배포 시에만 임시 생성 후 삭제

#### 환경변수 우선순위

```
1. Docker Compose의 environment (최우선)
   → 현재: 비어있음 (모두 Doppler로 이관)

2. env_file (doppler.env)
   → Doppler에서 런타임에 생성
   → 배포 완료 후 shred로 완전 삭제

3. 컨테이너 기본값
```

### Doppler 환경변수 추가/수정 방법

```bash
# 1. Doppler 웹 콘솔에서 수정
https://dashboard.doppler.com/

# 2. CLI로 수정 (로컬/서버)
doppler secrets set VARIABLE_NAME="value" --project tt --config prd

# 3. 대량 업로드 (.env 파일)
doppler secrets upload .env --project tt --config prd
```

---

## 🚀 배포 프로세스

### 자동 배포 (develop 브랜치 Push)

#### 1단계: 코드 Push

```bash
git add .
git commit -m "feat: 새로운 기능 추가"
git push origin develop
```

#### 2단계: GitHub Actions 자동 실행

**Job 1: makeTagAndRelease**
- Semantic versioning으로 태그 자동 생성
- GitHub Release 생성
- 출력: `tag_name` (예: v1.2.3)

**Job 2: buildImageAndPush**
- Multi-stage Dockerfile로 빌드
- 이미지 태그:
    - `ghcr.io/chehyeon-kim23/tt_backend:v1.2.3`
    - `ghcr.io/chehyeon-kim23/tt_backend:latest`
- GHCR에 푸시
- 빌드 캐시 활용

**Job 3: deploy**
- EC2 인스턴스 ID 조회
- AWS SSM Send-Command 실행
- 배포 스크립트 실행

#### 3단계: EC2에서 실행되는 배포 스크립트

```bash
#!/bin/bash
set -euo pipefail

# 1. Git 업데이트
cd /dockerProjects/tt-src/WEB7_9_B2ST_BE/
git fetch --all
git reset --hard origin/develop

# 2. Docker 디렉토리로 이동
cd docker/

# 3. Doppler에서 환경변수 다운로드
export DOPPLER_TOKEN="xxx"
export DOPPLER_PROJECT=tt
export DOPPLER_CONFIG=prd

umask 077  # 파일 권한 600으로 생성
doppler secrets download \
  --project "$DOPPLER_PROJECT" \
  --config "$DOPPLER_CONFIG" \
  --format env \
  --no-file > doppler.env

chmod 600 doppler.env

# 4. Docker 이미지 Pull & 재시작
docker compose --env-file doppler.env pull app
docker compose --env-file doppler.env up -d --force-recreate app

# 5. 민감 파일 완전 삭제 (복구 불가)
shred -vfz -n 3 doppler.env 2>/dev/null || rm -f doppler.env

# 6. 정리
docker image prune -f
docker logout ghcr.io 2>/dev/null

# 7. 배포 확인
docker compose ps app
```

### 수동 배포 (필요시)

```bash
# EC2에 SSH 접속
ssh ec2-user@your-ec2-ip

# 배포 디렉토리로 이동
cd /dockerProjects/tt-src/WEB7_9_B2ST_BE/docker/

# Doppler 환경변수 다운로드
export DOPPLER_TOKEN="your-token"
doppler secrets download \
  --project tt \
  --config prd \
  --format env \
  --no-file > doppler.env

# 배포 실행
docker compose --env-file doppler.env pull
docker compose --env-file doppler.env up -d --force-recreate

# 민감 파일 삭제
shred -vfz -n 3 doppler.env

# 상태 확인
docker compose ps
docker compose logs -f app
```

---

## 🔄 롤백 절차

### 빠른 롤백 (이전 버전으로)

```bash
# 1. EC2 접속
ssh ec2-user@your-ec2-ip

cd /dockerProjects/tt-src/WEB7_9_B2ST_BE/docker/

# 2. 이전 이미지 태그로 변경
# docker-compose.yml에서 직접 수정하거나:
export ROLLBACK_VERSION=v1.2.2

# 3. 이전 버전 Pull
docker pull ghcr.io/chehyeon-kim23/tt_backend:$ROLLBACK_VERSION

# 4. 태그 변경
docker tag ghcr.io/chehyeon-kim23/tt_backend:$ROLLBACK_VERSION \
  ghcr.io/chehyeon-kim23/tt_backend:latest

# 5. 재시작
doppler secrets download --project tt --config prd --format env --no-file > doppler.env
docker compose --env-file doppler.env up -d --force-recreate app
shred -vfz -n 3 doppler.env

# 6. 확인
docker compose logs -f app
```

### Git 커밋 롤백

```bash
# 1. 문제가 되는 커밋 되돌리기
git revert <commit-hash>
git push origin develop

# 2. GitHub Actions가 자동으로 새 버전 배포
```

---

## 🔧 트러블슈팅

### 문제 1: 배포가 실패했어요

**증상**: GitHub Actions에서 deploy job 실패

**확인 사항**:
```bash
# EC2에서 SSM 에이전트 상태 확인
sudo systemctl status amazon-ssm-agent

# Docker 서비스 상태
sudo systemctl status docker

# 디스크 공간
df -h

# 로그 확인
cd /dockerProjects/tt-src/WEB7_9_B2ST_BE/docker/
docker compose logs app
```

### 문제 2: Doppler 환경변수를 못 가져와요

**확인 사항**:
```bash
# Doppler CLI 설치 확인
doppler --version

# 토큰 테스트
export DOPPLER_TOKEN="your-token"
doppler secrets --project tt --config prd

# 네트워크 확인
curl -I https://api.doppler.com
```

### 문제 3: 컨테이너가 계속 재시작해요

**확인**:
```bash
# 컨테이너 상태
docker compose ps

# 상세 로그
docker compose logs --tail=100 app

# 헬스체크 확인
docker inspect tt_backend_app | grep -A 10 Health
```

**일반적인 원인**:
- 환경변수 누락
- DB 연결 실패
- 포트 충돌
- 메모리 부족

### 문제 4: doppler.env 파일이 남아있어요

**정상 상황**: 배포 완료 후 자동 삭제됨

**수동 삭제**:
```bash
# 파일 완전 삭제
shred -vfz -n 3 doppler.env

# 또는
rm -f doppler.env
```

### 문제 5: Redis Cluster 초기화 실패

**확인**:
```bash
# Redis 노드 상태
docker compose ps | grep redis

# Cluster 상태 확인
docker compose exec redis-node-1 redis-cli -a $REDIS_PASSWORD cluster info

# 재초기화
docker compose down
docker volume prune -f
docker compose up -d
```

---

## 📊 모니터링

### 배포 후 체크리스트

#### 1. 애플리케이션 헬스체크
```bash
# HTTP 헬스체크
curl http://localhost:8080/actuator/health

# Docker 헬스 상태
docker compose ps app
```

#### 2. 로그 확인
```bash
# 실시간 로그
docker compose logs -f app

# 최근 100줄
docker compose logs --tail=100 app

# 에러만 필터링
docker compose logs app | grep ERROR
```

#### 3. Grafana 대시보드
```
URL: http://your-ec2-ip:3001
ID: admin
PW: (Doppler의 GRAFANA_PASSWORD)
```

**확인 항목**:
- CPU/Memory 사용률
- HTTP Request Rate
- Error Rate
- Database Connection Pool
- Redis 응답 시간

#### 4. Prometheus 메트릭
```
URL: http://your-ec2-ip:9090
```

**주요 쿼리**:
```promql
# HTTP 요청 수
rate(http_server_requests_seconds_count[5m])

# JVM 메모리 사용률
jvm_memory_used_bytes / jvm_memory_max_bytes * 100

# DB 연결 풀
hikaricp_connections_active
```

### 알림 설정

**Slack 알림** (AlertManager 사용)
- 애플리케이션 다운
- CPU 사용률 80% 초과
- 메모리 사용률 85% 초과
- Error Rate 1% 초과
- DB 연결 실패

---

## 📝 배포 체크리스트

### 배포 전

- [ ] 코드 리뷰 완료
- [ ] 단위 테스트 통과
- [ ] Doppler에 필요한 환경변수 등록
- [ ] develop 브랜치에 최신 코드 머지
- [ ] 데이터베이스 마이그레이션 스크립트 확인

### 배포 중

- [ ] GitHub Actions 워크플로우 정상 실행 확인
- [ ] Docker 이미지 빌드 성공
- [ ] GHCR Push 성공
- [ ] SSM 명령 실행 성공

### 배포 후

- [ ] 애플리케이션 헬스체크 통과
- [ ] 주요 API 엔드포인트 테스트
- [ ] 에러 로그 확인
- [ ] Grafana 메트릭 정상 확인
- [ ] Redis Cluster 연결 확인
- [ ] PostgreSQL 연결 확인
- [ ] 모니터링 알림 정상 작동 확인

---

## 🎯 Best Practices

### 1. 환경변수 관리
- ✅ 모든 민감정보는 Doppler에 보관
- ✅ 서버에 .env 파일 영구 저장 금지
- ✅ 환경변수 변경 시 Doppler에서만 수정
- ✅ 로컬 개발도 Doppler 사용 권장

### 2. Docker 이미지
- ✅ Alpine 베이스 이미지 사용 (경량화)
- ✅ Multi-stage build로 빌드/실행 분리
- ✅ 비-root 유저로 실행 (보안)
- ✅ .dockerignore로 불필요한 파일 제외

### 3. 배포
- ✅ Blue-Green 배포 고려 (무중단)
- ✅ 배포 전 백업 확인
- ✅ 모니터링 알림 활성화
- ✅ 롤백 계획 수립

### 4. 보안
- ✅ 최소 권한 원칙 (IAM, Docker)
- ✅ 민감 파일 shred로 완전 삭제
- ✅ 정기적인 보안 패치
- ✅ 이미지 취약점 스캔 (Trivy)