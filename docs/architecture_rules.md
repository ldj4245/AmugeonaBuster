# 📐 헥사고날 아키텍처(Ports & Adapters) 설계 규칙 가이드라인

본 문서는 프로젝트의 핵심 아키텍처 규칙인 **헥사고날 아키텍처(Hexagonal Architecture / Ports and Adapters Pattern)**를 정의합니다.
"변경이 쉬운 소프트웨어"를 달성하기 위해, 모든 개발자는 본 문서에 명시된 레이어 별 역할과 의존성 규칙을 완벽히 준수해야 합니다.

---

## 1. 아키텍처 핵심 철학 및 의존성 규칙

```
                    ┌──────────────────────────────────────────────┐
                    │                   ADAPTERS                   │
                    │   ┌──────────────────────────────────────┐   │
                    │   │                PORTS                 │   │
                    │   │   ┌──────────────────────────────┐   │   │
                    │   │   │            DOMAIN            │   │   │
                    │   │   │                              │   │   │
                    │   │   │    - Pure Java Entities      │   │   │
                    │   │   │    - Pure Business Rules     │   │   │
                    │   │   └──────────────────────────────┘   │   │
                    │   │     - Use Cases (In Ports)           │   │
                    │   │     - SPIs (Out Ports)               │   │
                    │   │──────────────────────────────────────┘   │
                    │     - Controllers (In Adapters)              │
                    │     - JPA Repositories (Out Adapters)        │
                    │     - External Map APIs (Out Adapters)       │
                    └──────────────────────────────────────────────┘
                    ◀──────────────────────────────────────────────
                        의존성의 방향은 외곽에서 중심(Domain)으로만 향한다!
```

> [!IMPORTANT]
> **황금률 (Golden Rule):** 
> *   `domain` 패키지는 어떠한 외곽 프레임워크(Spring, JPA, Lombok, 외부 라이브러리 등) 어노테이션이나 import도 가질 수 없습니다. 오직 **순수 자바(Java SE)** 코드로만 작성합니다. (프레임워크 비종속성 확보)
> *   의존성은 항상 외부(어댑터)에서 내부(포트 -> 도메인)로만 흘러야 합니다.

---

## 2. 표준 패키지 구조 (Package Structure)

루트 패키지 (`com.amugeonabuster`) 하위의 모듈식 패키지 구조는 다음과 같습니다.

```
com.amugeonabuster
 ├── domain                     # [도메인 헥사곤] 비즈니스 엔티티 및 핵심 규칙
 │    ├── model                 # 순수 도메인 모델 (Room, Member, Swipe, Restaurant)
 │    └── exception             # 도메인 제약 조건 에러 정의
 ├── application                # [애플리케이션 헥사곤] 포트 및 유스케이스 서비스
 │    ├── port
 │    │    ├── in               # 인커밍 포트 (방 생성, 스와이프 등 유저가 호출하는 인터페이스)
 │    │    └── out              # 아웃고잉 포트 (DB 로드, 지도 검색 등 시스템이 호출하는 인터페이스)
 │    └── service               # 포트 구현체 (스프링 @Service 적용, 비즈니스 흐름 제어)
 └── adapter                    # [어댑터 헥사곤] 프레임워크/외부 인프라와의 통신 구현
      ├── in
      │    ├── web              # REST HTTP API 컨트롤러
      │    └── websocket        # 웹소켓 메시지 핸들러
      └── out
           ├── persistence      # JPA / Redis 저장소 어댑터 및 영속화 객체
           └── external         # 카카오맵 API 통신 연동 모듈
```

---

## 3. 계층별 역할 및 개발 가이드

### A. 도메인 계층 (`domain`)
*   **어노테이션 금지:** `@Entity`, `@Table` (JPA), `@RestController` 등 프레임워크 어노테이션 사용 금지.
*   **역할:** 비즈니스의 상태 변화와 규칙을 가집니다. 예: *"이미 종료된 방에는 투표할 수 없다"*는 비즈니스 예외 룰은 `Room` 도메인 내부에 구현합니다.
*   Lombok의 `@Getter`, `@Builder` 같이 단순 자바 코드 생성을 돕는 라이브러리 어노테이션은 예외적으로 허용합니다.

### B. 애플리케이션 계층 (`application`)
*   **포트 (`port.in`, `port.out`):**
    *   **In Port (UseCase):** 프론트엔드가 요청할 수 있는 기능 명세서입니다. (예: `CreateRoomUseCase.java`)
    *   **Out Port (SPI):** 시스템이 비즈니스를 수행하기 위해 필요한 인프라 기능의 명세서입니다. (예: `SaveRoomPort.java`, `SearchRestaurantPort.java`)
*   **서비스 (`service`):**
    *   인커밍 포트를 구현(`implements`)하며, 아웃고잉 포트를 통해 데이터를 쓰고 읽는 오케스트레이터입니다.
    *   트랜잭션 어노테이션(`@Transactional`)은 이 계층의 서비스 메서드에 적용합니다.

### C. 어댑터 계층 (`adapter`)
*   **인커밍 어댑터 (`adapter.in`):**
    *   외부 요청(HTTP REST, WebSocket JSON)을 자바 객체로 변환하여 **In Port (UseCase)**를 호출합니다.
    *   컨트롤러는 절대로 아웃고잉 포트나 레포지토리를 직접 참조할 수 없습니다.
*   **아웃고잉 어댑터 (`adapter.out`):**
    *   **Out Port**를 구현(`implements`)하여 DB(JPA)나 외부 API에 직접 물리적 바인딩을 수행합니다.
    *   데이터베이스 엔티티(JPA `@Entity`)는 이 계층에만 존재하며, 도메인 모델과 데이터 모델 간 변환을 전담하는 매퍼(Mapper) 클래스를 구축합니다.

---

## 4. 데이터 전송 객체 (DTO) 격리 원칙

변경에 강력해지려면 계층 간 데이터를 넘길 때 단일 객체(예: 엔티티 하나)를 돌려막기 하지 않아야 합니다.

1.  **Web Request DTO ──> Command Object**
    *   웹 컨트롤러는 요청 DTO를 받아 검증한 뒤, 유스케이스 포트에 넘길 때는 `domain`이나 `application` 레이어에 속하는 불변 객체인 `Command` 객체로 매핑해 전달합니다.
2.  **Domain Model <──> JPA Entity mapping**
    *   `adapter.out.persistence` 계층에서는 `domain.model`을 JPA용 `@Entity` 객체로 상호 매핑하는 `RoomMapper`와 같은 클래스를 필수 작성합니다.
    *   DB 구조 변경(컬럼 추가 등)이 비즈니스 로직 도메인 객체에 영향을 미치지 않도록 보호하는 방어벽 역할을 합니다.

---

## 5. 아키텍처 규칙 자동 검증 (ArchUnit)

구두 규칙은 한계가 있으므로, 빌드 및 테스트 단계에서 아키텍처 규칙을 위반할 경우 테스트가 깨지도록 **ArchUnit** 테스트 코드를 필수로 추가합니다.

### Gradle 의존성 주입 예시
```groovy
testImplementation 'com.tngtech.archunit:archunit-junit5:1.2.0'
```

### 아키텍처 무결성 검증 테스트 예시 (`HexagonalArchitectureTest.java`)
```java
package com.amugeonabuster;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.onionArchitecture;

@AnalyzeClasses(packages = "com.amugeonabuster")
public class HexagonalArchitectureTest {

    @ArchTest
    public static final ArchRule 헥사고날_의존성_무결성_검증 = onionArchitecture()
            .domainModels("..domain..")
            .domainServices("..application.service..")
            .applicationServices("..application.service..")
            .adapter("web", "..adapter.in.web..")
            .adapter("websocket", "..adapter.in.websocket..")
            .adapter("persistence", "..adapter.out.persistence..")
            .adapter("external", "..adapter.out.external..");
}
```
*   본 테스트는 도메인이 어댑터 계층을 참조하거나, 웹 어댑터가 영속성 어댑터를 직접 참조하는 등의 아키텍처 붕괴를 **빌드 시간(Compile time / Test phase)**에 기계적으로 전면 통제합니다.
