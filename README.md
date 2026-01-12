# WEB7_9_B2ST_BE
> **TT(Ticket & Trade)** — 공연 예매(대기열/좌석 선점/추첨·신청 예매)와 교환·양도(거래), 결제 흐름을 지원하는 백엔드 서버입니다.

## 🎯 프로젝트 소개
(작성)

## 👥 팀 구성 & 링크
- 팀 구성: (작성)
- 배포 링크: https://doncrytt.vercel.app/
- Frontend Github: https://github.com/prgrms-web-devcourse-final-project/WEB7_9_B2ST_FE
- Backend Github: https://github.com/prgrms-web-devcourse-final-project/WEB7_9_B2ST_BE


## ⚙️ 핵심 기능

### 🎫 공연/회차
- 공연 생성/조회(관리자/사용자 분리)
- 회차(공연 일정) 생성 및 조회
- 좌석 등급/가격 정책 관리

### ⏳ 대기열
- 예매 트래픽 집중 상황에서 대기열 기반 접근 제어
- Redis 기반 상태 관리(환경 설정으로 on/off 가능)

### 🪑 좌석 선점/예매
- 좌석 선점(HOLD) → 예매 확정(SOLD) 흐름
- 선점 만료/실패 처리로 좌석 상태 복구

### 🎲 추첨 예매
- 추첨 응모 생성 및 내 응모 내역 조회
- 추첨 결과 처리 및 좌석 배정 흐름 지원

### 📝 신청 예매(사전신청)
- 신청 예매 오픈/마감 정책
- 신청/예약 생성 및 만료 처리

### 💳 결제
- 도메인별 결제 흐름 분리(예매/신청/추첨/거래)
- 결제 상태 관리 및 후처리 구조

### 🔁 교환·양도(거래)
- 거래 등록/조회
- 거래 요청/승인 흐름
- 티켓 상태와 소유권 검증

### 🧑‍💼 관리자
- 관리자 전용 API(`/api/admin/**`) 권한 분리
- 공연/예매/거래 운영 지원 기능


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

## 🏗️ 아키텍처
(작성)

## 🗂️ ERD
(작성)

## 🎬 시연 영상
(작성)
## 🧩 협업 규칙

### 💬 코드 컨벤션
- **Naver Java Convention** 기반
- **기본 원칙**
  - 가독성 최우선
  - 특별한 이유가 없는 경우 **IntelliJ IDEA 자동 서식** 준수

### 🏷️ 네이밍 & 작성 규칙

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




