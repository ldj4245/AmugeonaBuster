# 🍱 아뮤거나 버스터 (AmugeonaBuster)

> **직장인을 위한 실시간 메뉴 매칭 및 사내식당 식단 구독 서비스**  
> 🔗 **배포 링크:** [https://amugeona-buster-6eda848df67d.herokuapp.com](https://amugeona-buster-6eda848df67d.herokuapp.com)

---

## 📌 프로젝트 소개
매일 점심시간마다 메뉴 결정에 피로감을 느끼는 직장인들을 위한 유틸리티 웹 서비스입니다.  
사내식당 실시간 식단 조회 및 카카오톡 리스트형 예약 알림 발송 기능을 제공하며, 외부 맛집 선정을 위해 실시간 투표(스와이프 매칭) 및 복불복 내기 미니게임 기능을 지원합니다.

---

## 🛠️ 기술 스택

### Backend
*   **Java 21** / **Spring Boot 3.2**
*   **Spring Data JPA** / **PostgreSQL** (H2 for Local)
*   **Spring WebSocket** (STOMP)
*   **ArchUnit** (아키텍처 규칙 검증)

### Frontend
*   **React 18** / **TypeScript**
*   **Vite**
*   **TailwindCSS**

### DevOps / Infrastructure
*   **Heroku** (Cloud Deployment)
*   **GitHub Actions** (CI/CD)

---

## ✨ 핵심 기능

1.  **사내식당 식단 실시간 연동 & 카카오톡 예약 알림**
    *   Welplus 모바일 API 직접 연동 및 층수별 운영 여부 파싱 노출
    *   사용자가 설정한 요일/시간대에 맞춰 당일 맞춤형 식단을 카카오톡 '나에게 보내기' 리스트 템플릿 메시지로 자동 발송 (OAuth 토큰 자동 갱신 및 만료 예외 대응)
2.  **실시간 투표 기반 메뉴 매칭 (스와이프 게임)**
    *   웹소켓(STOMP) 프로토콜을 이용해 대기방을 구성하고, 구성원들이 메뉴별로 Like/Dislike 스와이프 진행
    *   전원 투표 완료 시 매칭 1위 메뉴를 선정하고, 카카오 로컬 검색 API를 이용해 주변 2km 반경 맛집 추천
3.  **식단별 실시간 평점 및 한줄평 피드백**
    *   당일 코너별 식단에 대해 동료들의 평점(별점) 및 한줄평 등록 기능
    *   낙관적 업데이트(Optimistic Update) 및 브라우저 세션 핑거프린팅을 통한 중복 도배 방지
4.  **내기 미니게임 센터**
    *   소금 아메리카노 복불복 및 반응형 SVG 애니메이션을 탑재한 사다리 타기 게임 지원

---

## 🏗️ 아키텍처 및 기술적 해결 내역 (Interview Focus)

### 1. 헥사고날 아키텍처(Hexagonal Architecture) 도입을 통한 결합도 분리
외부 기술 인프라(Welstory Plus API, Kakao REST API, JPA, WebSocket)의 변화가 도메인 비즈니스 규칙에 미치는 영향을 최소화하기 위해 **포트와 어댑터(Ports & Adapters) 패턴**을 전면 적용했습니다.
*   **핵심 도메인의 POJO화:** 도메인 엔티티(`Room`, `Member` 등)는 프레임워크나 외부 라이브러리 의존성이 없는 순수 자바 객체로 보존했습니다.
*   **기술 컴포넌트 격리:** 800줄에 달하던 Welplus API 연동/파싱 코드를 `WelplusApiAdapter`로 분리하고, 컨트롤러와 스케줄러가 오직 인바운드 포트(Query/Command 인터페이스)만을 주입받게 구현했습니다.
*   **ArchUnit 자동화 검증:** 패키지 간의 의존성 방향 규칙(Domain/Application ➡️ Adapter 방향으로의 의존성 차단)을 검증하기 위해 ArchUnit 테스트를 도입, 빌드 타임에 정합성을 강제했습니다.

### 2. CQRS(명령-조회 책임 분리) 기반 트랜잭션 최적화
단일 도메인 서비스 내에 조회와 쓰기가 섞여 데이터 일관성 제어와 부하 제어가 어렵던 구조를 쪼갰습니다.
*   `GetTodayMenuQuery`, `GetWelstoryAlertQuery`(조회 전용)와 `SaveWelstoryAlertCommand`, `SendScheduledAlertsCommand`(상태 변경 전용)로 유스케이스 포트를 나눴습니다.
*   조회용 서비스 메서드에는 `@Transactional(readOnly = true)`를 적용해 JPA 영속성 컨텍스트의 변경 감지(Dirty Checking) 스냅샷 연산 오버헤드를 차단하고 데이터 읽기 성능을 개선했습니다.

### 3. 외부 API 장애 및 간헐적 만료 예외 대응
*   Spring `RestTemplate`이 외부 API 401/403 응답 수신 시 예외를 전파해 재인증 로직을 타지 못하던 버그를 진단했습니다. 
*   `HttpClientErrorException` 예외를 세분화하여 캐치하고, 자동으로 토큰을 강제 리프레시한 뒤 재요청을 1회 수행하는 예외 안전성 파이프라인을 구축해 연동 안정성을 99% 이상으로 유지했습니다.

### 4. 모바일 웹뷰 가상 키보드 가림 대응 (UX 개선)
*   모바일 환경에서 한줄평 작성 시 가상 키보드가 화면 하단의 입력창을 가리는 이슈가 있었습니다.
*   프론트엔드 포커스 감지 라이프사이클을 도입하여, 포커스 시 브라우저 정렬 가속을 제어하고 스크롤 가능 높이를 확보하기 위해 동적 스페이서(Spacer)를 하단에 렌더링했습니다.
*   포커싱 직후 `scrollIntoView({ behavior: 'smooth', block: 'center' })`를 호출해 모바일 기기별 자판 상승 직후 입력창이 뷰포트 정중앙에 부드럽게 고정되도록 개선했습니다.

---

## 📂 프로젝트 패키지 구조 (Backend)

```text
com.amugeonabuster
├── adapter
│   ├── in
│   │   ├── web        # REST Controllers (Room, Alert, Review)
│   │   ├── websocket  # WebSocket Configuration
│   │   └── scheduler  # Scheduled Alert Batch trigger
│   └── out
│       ├── external   # Outbound REST clients (Kakao, Welplus)
│       ├── mock       # Outbound mock recommendation adapter
│       └── persistence# Spring Data JPA persistence adapter & Entity
├── application
│   ├── port
│   │   ├── in         # Inbound UseCases (Commands & Queries)
│   │   └── out        # Outbound Ports (Repository, API abstraction)
│   └── service        # Pure business logic and orchestration
└── domain
    ├── exception      # Domain-specific exceptions
    └── model          # Rich Domain Models (Aggregate roots, entities)
```
