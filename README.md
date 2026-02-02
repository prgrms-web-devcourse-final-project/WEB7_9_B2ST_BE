# TT (Ticket & Trade) - Backend

> 공연 예매(대기열/좌석 선점/추첨·신청 예매)와 티켓 거래(교환/양도), 결제 흐름을 지원하는 Spring Boot 기반 백엔드 서버

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white" alt="Java 21"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.0-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot 4.0.0"/>
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white" alt="PostgreSQL"/>
  <img src="https://img.shields.io/badge/Redis%20Cluster-7-DC382D?logo=redis&logoColor=white" alt="Redis Cluster"/>
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white" alt="Docker"/>
</p>

## 📋 목차

- 🎯 [프로젝트 개요](#-프로젝트-개요)
- 👥 [팀 구성](#-팀-구성)
- 🔗 [링크](#-링크)
- ⚙️ [핵심 기능](#️-핵심-기능)
- 🛠️ [기술 스택](#️-기술-스택)
- 🏗️ [시스템 아키텍처](#️-시스템-아키텍처)
- 🗂️ [ERD](#️-erd)
- 🗂️ [프로젝트 구조](#-프로젝트-구조)
- 📊 [모니터링 구성](#-모니터링-구성)
- 🧩 [협업 규칙](#-협업-규칙)

---

## 🎯 프로젝트 개요

<img width="1408" height="633" alt="image" src="https://github.com/user-attachments/assets/23181df2-a498-4e96-9f33-7a71cb2848fc" />

**TT(Ticket & Trade)** 는 공연 티켓 예매 및 2차 거래(교환/양도) 플랫폼의 백엔드 서버입니다.


### 주요 도메인

| 도메인         | 설명                                     |
|:------------|:---------------------------------------|
| **대기열**     | Redis 기반 트래픽 분산, 순차 접근 제어              |
| **좌석 예매**   | 좌석 선점(HOLD → SOLD), 만료 자동 복구           |
| **추첨 예매**   | 등급별 응모, 공정 추첨, 당첨 알림                   |
| **사전신청 예매** | 오픈/마감 정책 기반 신청 처리                      |
| **거래**      | 티켓 소유권 검증, 교환·양도 흐름                    |
| **결제**      | 도메인별 결제 흐름 분리, 상태 관리                   |
| **인증**      | JWT + Refresh Token Rotation, 카카오 OIDC |

---

## 👥 팀 구성

|       이름       |   역할    |                                                               GitHub                                                               |
|:--------------:|:-------:|:----------------------------------------------------------------------------------------------------------------------------------:|
|     김채현     | Backend (PO) |           [![GitHub](https://img.shields.io/badge/-Chehyeon--Kim23-181717?logo=github&logoColor=white)](https://github.com/Chehyeon-Kim23)            |
| 김채현 | Backend |  [![GitHub](https://img.shields.io/badge/-whyin-181717?logo=github&logoColor=white)](https://github.com/whyin)  |
|      노미경      | Backend |          [![GitHub](https://img.shields.io/badge/-77r77r-181717?logo=github&logoColor=white)](https://github.com/77r77r)           |
| 박민형  | Backend (팀장) |          [![GitHub](https://img.shields.io/badge/-minibr-181717?logo=github&logoColor=white)](https://github.com/minibr)           |
|     이위림     | Backend | [![GitHub](https://img.shields.io/badge/-weilim0513--tech-181717?logo=github&logoColor=white)](https://github.com/weilim0513-tech) |

---

## 🔗 링크

|     구분      |                                           링크                                           |
|:-----------:|:--------------------------------------------------------------------------------------:|
|    🌐 배포    |              [https://doncrytt.vercel.app](https://doncrytt.vercel.app/)               |
| 💻 Frontend | [WEB7_9_B2ST_FE](https://github.com/prgrms-web-devcourse-final-project/WEB7_9_B2ST_FE) |
| 🔧 Backend  | [WEB7_9_B2ST_BE](https://github.com/prgrms-web-devcourse-final-project/WEB7_9_B2ST_BE) |
|  📖 API 문서  |             [Swagger UI](http://15.165.115.135:8080/swagger-ui/index.html#/)             |

---

## ⚙️ 핵심 기능

### 🔐 인증/인가 (Auth)

| 기능             | 구현 상세                                                                  |
|:---------------|:-----------------------------------------------------------------------|
| JWT 인증         | Access Token(30분) + Refresh Token(7일, Redis 저장)                        |
| Token Rotation | 재발급 시 Family/Generation 기반 탈취 감지, 이전 토큰 사용 시 전체 세션 무효화                 |
| 카카오 OIDC       | ID Token RSA 서명 검증(JWKS 24시간 캐싱), nonce 1회성 검증, 자동 계정 연동               |
| 로그인 보안         | 5회 실패 시 10분 잠금(Redis TTL), Lua Script 원자적 카운팅                          |
| 위협 탐지          | Credential Stuffing(IP당 10+ 계정), Brute Force(IP당 50+ 실패) 탐지 → Slack 알림 |

### 👤 회원 (Member)

| 기능     | 구현 상세                                             |
|:-------|:--------------------------------------------------|
| 회원가입   | BCrypt 암호화, IP별 Rate Limiting(시간당 3회, Lua Script) |
| 이메일 인증 | SecureRandom 6자리 코드, Redis TTL 5분, 시도 횟수 제한(5회)   |
| 탈퇴/복구  | Soft Delete + 30일 복구 유예, 복구 토큰(UUID, 24시간 TTL)    |
| 감사 로그  | 로그인/가입 이벤트 비동기 저장(@Async + REQUIRES_NEW)          |

### ⏳ 대기열 (Queue)

| 기능     | 구현 상세                                  |
|:-------|:---------------------------------------|
| 대기열 진입 | Redis Sorted Set 기반, 타임스탬프 스코어로 순서 보장  |
| 순번 조회  | ZRANK 명령으로 실시간 대기 순번 반환                |
| 입장 처리  | 순차적 입장 토큰 발급, 유효 시간 제한                 |
| 상태 관리  | WAITING → PROCESSING → COMPLETED 상태 전이 |
| 환경 설정  | 대기열 on/off 설정 가능, 트래픽 상황에 따라 유연하게 적용   |

### 🪑 좌석 예매 (Reservation)

| 기능    | 구현 상세                                      |
|:------|:-------------------------------------------|
| 좌석 선점 | AVAILABLE → HOLD 상태 전이, 5분 TTL 설정          |
| 중복 방지 | 동일 좌석 동시 선점 시도 시 낙관적 락으로 충돌 감지             |
| 예매 확정 | 결제 완료 시 HOLD → SOLD 상태 전이                  |
| 선점 만료 | 스케줄러 기반 TTL 만료 좌석 자동 복구 (HOLD → AVAILABLE) |
| 예매 취소 | 예매 취소 시 좌석 상태 원복, 환불 처리 연동                 |

### 🎲 추첨 예매 (Lottery)

| 기능     | 구현 상세                      |
|:-------|:---------------------------|
| 응모 등록  | 회차/등급별 응모, 중복 응모 검증        |
| 응모 제한  | 1인당 등급별 최대 응모 수량 제한        |
| 추첨 처리  | SecureRandom 기반 공정 추첨 알고리즘 |
| 당첨 처리  | 당첨자 좌석 자동 배정, 결제 기한 설정     |
| 결과 알림  | 당첨/낙첨 이메일 비동기 발송           |
| 미결제 처리 | 결제 기한 초과 시 자동 당첨 취소, 좌석 반환 |

### 📝 사전신청 예매 (Pre-Reservation)

| 기능    | 구현 상세                   |
|:------|:------------------------|
| 신청 기간 | 오픈/마감 일시 기반 신청 가능 기간 검증 |
| 신청 등록 | 회차/등급별 사전신청, 수량 지정      |
| 신청 확정 | 신청 → 결제 대기 → 결제 완료 흐름, 예외: CANCELLED, EXPIRED|
| 만료 처리 | 결제 기한 초과 시 신청 자동 만료, 결제 시도 차단     |
| 신청 취소 | 사용자 요청에 의한 신청 취소 처리     |

### 💳 결제 (Payment)

| 기능      | 구현 상세                                             |
|:--------|:--------------------------------------------------|
| 도메인별 분리 | 좌석예매/추첨/사전신청/거래별 결제 생성/검증 로직 독립                      |
| 상태 관리   | PENDING → PROCESSING → COMPLETED/FAILED/CANCELLED (전이 규칙 명시) |

### 🔁 거래 (Trade)

| 기능     | 구현 상세                  |
|:-------|:-----------------------|
| 거래 등록  | 티켓 소유권 검증, 중복 등록 방지    |
| 거래 요청  | 구매자 거래 요청, 판매자 승인 대기   |
| 임시 점유  | 승인 시 redis에 티켓 임시 점유 등록, 결제 기한까지 재거래/재예약 요청 즉시 차단(최종 확정은 DB로 검증)  |
| 거래 승인  | 판매자 승인 시 결제 프로세스 진입    |
| 소유권 이전 | 결제 완료 시 티켓 소유권 구매자로 변경 |

### 🧑‍💼 관리자 (Admin)

| 기능    | 구현 상세                                           |
|:------|:------------------------------------------------|
| 회원 관리 | 검색/필터링/페이징, 대시보드 통계                             |
| 인증 관리 | 로그인/가입 로그 조회, 계정 잠금 해제                          |
| 권한 분리 | `/api/admin/**` URL 레벨 + `@PreAuthorize` 메서드 레벨 |

---

## 🛠️ 기술 스택

### \<Backend\>
![Java](https://img.shields.io/badge/Java-ED8B00?logo=openjdk&logoColor=white&style=for-the-badge)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?logo=springboot&logoColor=white&style=for-the-badge)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?logo=springsecurity&logoColor=white&style=for-the-badge)
![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-0D6EFD?logo=spring&logoColor=white&style=for-the-badge)
![QueryDSL](https://img.shields.io/badge/QueryDSL-000000?style=for-the-badge)
![Gradle](https://img.shields.io/badge/Gradle-02303A?logo=gradle&logoColor=white&style=for-the-badge)
![Swagger](https://img.shields.io/badge/Swagger-85EA2D?logo=swagger&logoColor=black&style=for-the-badge)

### \<DataBase\>
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white&style=for-the-badge)
![H2 Database](https://img.shields.io/badge/H2%20Database-1A73E8?logo=h2database&logoColor=white&style=for-the-badge)
![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white&style=for-the-badge)

### \<Observability & Performance\>
![Grafana](https://img.shields.io/badge/Grafana-F46800?logo=grafana&logoColor=white&style=for-the-badge)
![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?logo=prometheus&logoColor=white&style=for-the-badge)
![Doppler](https://img.shields.io/badge/Doppler-000000?logo=doppler&logoColor=white&style=for-the-badge)
![Micrometer](https://img.shields.io/badge/Micrometer-000000?style=for-the-badge)

### \<Infra\>
![Terraform](https://img.shields.io/badge/Terraform-7B42BC?logo=terraform&logoColor=white&style=for-the-badge)
![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white&style=for-the-badge)
![AWS](https://img.shields.io/badge/AWS-232F3E?logo=amazonaws&logoColor=white&style=for-the-badge)
![Amazon%20EC2](https://img.shields.io/badge/Amazon%20EC2-FF9900?logo=amazonec2&logoColor=black&style=for-the-badge)
![Amazon%20S3](https://img.shields.io/badge/Amazon%20S3-569A31?logo=amazons3&logoColor=white&style=for-the-badge)

### \<External Services\>
![JWT](https://img.shields.io/badge/JWT-000000?logo=jsonwebtokens&logoColor=white&style=for-the-badge)
![KAKAO OAuth](https://img.shields.io/badge/KAKAO%20OAuth-FFCD00?style=for-the-badge&logo=kakaotalk&logoColor=000000)
![SMTP](https://img.shields.io/badge/SMTP-000000?style=for-the-badge)
![Slack Webhook](https://img.shields.io/badge/Slack%20Webhook-4A154B?logo=slack&logoColor=white&style=for-the-badge)
![Swagger](https://img.shields.io/badge/Swagger-85EA2D?logo=swagger&logoColor=black&style=for-the-badge)
![Postman](https://img.shields.io/badge/Postman-FF6C37?logo=postman&logoColor=white&style=for-the-badge)
![Testcontainers](https://img.shields.io/badge/Testcontainers-000000?logo=testcontainers&logoColor=white&style=for-the-badge)
![JUnit](https://img.shields.io/badge/JUnit-25A162?logo=junit5&logoColor=white&style=for-the-badge)

---

## 🏗️ 시스템 아키텍처

<img width="2430" height="1386" alt="Image" src="https://github.com/user-attachments/assets/9d08cc5f-a93b-45b5-9fcd-9c9b39275a24" />

---

## 🗂️ ERD

![Image](https://github.com/user-attachments/assets/3c393c6a-5186-444d-80fc-b69c17f406c1)

---

## 📁 프로젝트 구조

<details>
<summary><b>펼쳐보기</b></summary>

```
src/main/java/com/back/b2st/
├── domain/
│   ├── auth/               # JWT, OAuth, 로그인 보안, 토큰 관리
│   ├── member/             # 회원 CRUD, 탈퇴/복구, Rate Limiting
│   ├── email/              # 이메일 인증, 비동기 발송
│   ├── performance/        # 공연 관리
│   ├── performanceschedule/# 공연 회차
│   ├── seat/               # 좌석 관리
│   ├── scheduleseat/       # 회차별 좌석 상태
│   ├── queue/              # 대기열 시스템
│   ├── reservation/        # 좌석 예매
│   ├── prereservation/     # 사전 신청
│   ├── lottery/            # 추첨 예매
│   ├── payment/            # 결제
│   ├── ticket/             # 티켓 관리
│   ├── trade/              # 거래 (교환/양도)
│   ├── venue/              # 공연장
│   └── bank/               # 은행 코드 Enum
│
├── global/
│   ├── alert/              # SlackAlertService (Webhook 연동)
│   ├── config/             # Redis, S3, Redisson, Alert 설정
│   ├── error/              # GlobalExceptionHandler, ErrorCode
│   ├── jwt/                # JwtTokenProvider, JwtAuthenticationFilter
│   ├── jpa/                # BaseEntity, QueryDslConfig, AuditorAware
│   ├── s3/                 # S3Service, PresignedUrl
│   ├── util/               # MaskingUtil, CookieUtils, SecurityUtils
│   └── metrics/            # MetricsConfig
│
└── security/               # Spring Security 설정
    ├── SecurityConfig.java
    ├── CustomUserDetails.java
    ├── CustomUserDetailsService.java
    ├── JwtAuthenticationEntryPoint.java
    └── JwtAccessDeniedHandler.java

docker/
├── docker-compose.yml              # 전체 스택 (App, DB, Redis Cluster, Monitoring)
├── monitoring/
│   ├── prometheus/
│   │   ├── prometheus.yml
│   │   └── rules/auth-alerts.yml
│   ├── grafana/
│   │   ├── provisioning/
│   │   └── dashboards/
│   └── alertmanager/
│       └── alertmanager.yml
└── init-*.sh                       # Redis Cluster 초기화 스크립트
```

</details>

---

## 📊 모니터링 구성

<details>
<summary><b>Grafana 대시보드</b></summary>

| 계층          | 대시보드                     | 주요 메트릭                                                                      |
|:------------|:-------------------------|:----------------------------------------------------------------------------|
| **Service** | tt-service-overview      | 요청 수, 에러율, 응답 시간 분포                                                         |
| **Domain**  | tt-auth-dashboard        | `auth_login_total`, `auth_account_locked_total`, `auth_token_reissue_total` |
|             | tt-email-dashboard       | `email_sent_total`, `email_verification_total`                              |
|             | tt-queue-dashboard       | 대기열 상태, 처리량                                                                 |
|             | tt-reservation-dashboard | 예매 생성, 좌석 선점/취소                                                             |
|             | tt-lottery-dashboard     | 응모 수, 당첨 처리                                                                 |
|             | tt-payment-dashboard     | 결제 요청/완료/실패                                                                 |
|             | tt-trade-dashboard       | 거래 등록/완료                                                                    |
| **Infra**   | tt-jvm-dashboard         | 힙 메모리, GC, 스레드 풀                                                            |
|             | tt-database-dashboard    | 커넥션 풀, 쿼리 성능                                                                |
|             | tt-redis-dashboard       | 메모리, 커맨드/sec, 키 상태                                                          |

</details>

<details>
<summary><b>Alertmanager 규칙</b></summary>

```yaml
// 예시
groups:
  - name: auth-alerts
    rules:
      - alert: HighLoginFailureRate
        expr: rate(auth_login_total{result="failure"}[5m]) > 0.1
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "로그인 실패율 증가"

      - alert: AccountLockDetected
        expr: increase(auth_account_locked_total[5m]) > 0
        labels:
          severity: critical
        annotations:
          summary: "계정 잠금 발생"
```

</details>

---

## 🧩 협업 규칙

<details>
<summary><b>펼쳐보기</b></summary>

### 코드 컨벤션

- **Naver Java Convention** 기반
- IntelliJ IDEA 자동 서식 준수

### Git 브랜치 전략

- `main`: 프로덕션
- `develop`: 개발 통합
- `feature/*`: 기능 개발
- 머지 조건: 최소 1명 리뷰 승인

</details>

---

### 🏷️ 네이밍 & 작성 규칙

<details>
<summary><b>펼쳐보기</b></summary>

#### 이슈(Issue)
- **제목 규칙**: `[타입] 작업내용`
  - 예시: `[feat] 로그인 기능 추가`
- **본문**: 팀 템플릿에 맞춰 작성

#### PR(Pull Request)
- **제목 규칙**: `[타입] 작업내용`
  - 예시: `[feat] 로그인 기능 추가`
- **본문**: 팀 템플릿에 맞춰 작성
- **브랜치 보호 규칙**: `main`, `develop`은 보호 브랜치로 **최소 1명 리뷰 승인 후**에만 머지

#### 브랜치(Branch)
- **생성 기준**: `develop` 브랜치에서 생성
- **명명 규칙**: `타입/작업내용`
  - 예시: `feat/조회-기능-개발`

#### Commit Message
- **형식**: `타입: 작업내용`
  - 예시: `feat: 로그인 기능 추가`

| 타입 | 의미 |
|---|---|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정(README, 주석 등) |
| `refactor` | 코드 리팩토링(동작 변화 없음) |
| `test` | 테스트 코드 추가/수정 |

</details>

