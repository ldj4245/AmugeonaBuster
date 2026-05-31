# 📝 코드 스타일 및 컨벤션 규칙 가이드라인 (Coding Convention)

본 문서는 프로젝트의 전체 코드 가독성을 보장하고 유지보수를 용이하게 하기 위한 **백엔드(Java) 및 프론트엔드(React/TypeScript) 표준 코드 컨벤션**을 정의합니다. 

모든 개발자는 본 컨벤션을 준수하여 일관성 있는 코드를 작성해야 하며, 이 규칙은 코드 리뷰 및 머지(Merge)의 필수 심사 기준이 됩니다.

---

## 1. 백엔드 Java 코딩 컨벤션 (Naver Hackday Style 기반)

우리는 국내 최고 수준의 테크 기업 개발 표준인 **네이버 핵데이 자바 컨벤션**을 계승하여 엄격한 가독성을 유지합니다.

### A. 들여쓰기 및 빈 줄 (Indentation & Spacing)
1.  **들여쓰기(Indent):** 탭(Tab) 문자 사용을 엄격히 금지하며, **4개의 스페이스(4 Spaces)** 들여쓰기를 원칙으로 합니다.
2.  **줄 너비 제한:** 한 줄의 최대 길이는 **120자**로 제한합니다. 120자를 초과하는 코드는 적절히 줄바꿈(Line Wrap)을 수행합니다.
3.  **빈 줄 두기:**
    *   클래스 멤버(필드, 생성자, 메서드) 사이에는 항상 **한 줄의 빈 줄**을 둡니다.
    *   메서드 바디 내부에서 논리적인 단락이 바뀔 때 한 줄의 빈 줄을 사용할 수 있으나, 두 줄 이상의 연속된 빈 줄은 금지합니다.

### B. 중괄호 및 줄바꿈 (Braces & Wrapping)
1.  **중괄호 스타일:** **K&R 스타일(Kernighan & Ritchie Style)**을 적용합니다. 
    *   여는 중괄호 `{`는 구문과 같은 줄에 작성하고, 닫는 중괄호 `}`는 새로운 줄에 단독으로 배치합니다.
    *   `else`, `catch`, `finally` 키워드는 이전 닫는 중괄호 `}`와 같은 줄에 붙여 씁니다.

```java
// 올바른 예 (Good)
if (condition) {
    doSomething();
} else {
    doOtherThing();
}
```

2.  **생략 금지:** 단 한 줄의 실행문이 있더라도 `if`, `for`, `while` 등의 블록 구조에서 중괄호 `{}` 생략을 엄격히 금지합니다.

### C. 네이밍 컨벤션 (Naming Convention)
1.  **패키지(Package):** 소문자 단일 단어 작성을 원칙으로 합니다. (예: `com.amugeonabuster.domain.model`)
2.  **클래스 및 인터페이스:** 파스칼 케이스(**PascalCase**)를 사용합니다.
    *   *클래스:* `RoomService`, `SaveRoomPort`
    *   *인터페이스:* 명사나 형용사 형태 사용 (`CreateRoomUseCase`, `Identifiable`)
3.  **메서드 및 변수:** 카멜 케이스(**camelCase**)를 사용합니다. (예: `createRoom()`, `roomStatus`)
4.  **상수(Constants):** 모든 글자를 대문자로 작성하며, 단어 구분은 언더바(`_`)를 사용합니다. (예: `MAX_MEMBER_COUNT`)

### D. 임포트 규칙 (Import Rules)
1.  **와일드카드 금지:** 스타 임포트(`import java.util.*;`) 형태의 사용을 절대 금지합니다. 모든 클래스는 개별적으로 명시하여 임포트합니다.
2.  **임포트 그룹화 순서:**
    1.  `java.` 관련 패키지
    2.  `javax.` 및 프레임워크 패키지 (Spring 등)
    3.  써드파티 라이브러리 (Lombok, ArchUnit 등)
    4.  프로젝트 로컬 패키지 (`com.amugeonabuster.*`)

---

## 2. 롬복(Lombok) 사용 규칙 및 금지 사항

Lombok은 코드를 간결하게 만들어 주지만, 남용할 경우 도메인의 불변성을 저해하고 디버깅을 어렵게 만듭니다.

1.  **`@Data` 사용 엄격 금지:** Getter, Setter, RequiredArgsConstructor, EqualsAndHashCode를 무작위로 생성하는 `@Data` 어노테이션의 엔티티 사용을 금지합니다. (특히 JPA 엔티티나 도메인 모델에서는 양방향 연산 시 무한 루프 에러 유발 가능)
2.  **도메인 객체 불변성 유지:** `domain` 헥사곤 하위의 도메인 모델은 불변 객체(Immutable Object) 설계를 지향하므로, `@Setter` 생성을 절대 금지하고 **생성자** 또는 **`@Builder`**를 통해서만 상태 값을 할당합니다.
3.  **로그 선언 규칙:** 로깅 처리가 필요한 경우 수동으로 Logger 인스턴스를 선언하지 않고, Lombok의 `@Slf4j` 어노테이션을 클래스 상단에 선언하여 활용합니다.

---

## 3. 프론트엔드 React / TypeScript 코딩 컨벤션

프론트엔드 코드는 React의 컴포넌트 생태계와 TypeScript의 안전성을 보장할 수 있는 코딩 규칙을 따릅니다.

### A. 컴포넌트 선언 및 작성 규칙
1.  **화살표 함수 컴포넌트:** 모든 함수형 컴포넌트는 화살표 함수 형태로 선언하며, 컴포넌트 타입으로 `React.FC` 또는 일반 함수의 명시적 리턴 타입을 선언합니다.

```typescript
// 올바른 예 (Good)
interface HeaderProps {
  title: string;
}

export const Header: React.FC<HeaderProps> = ({ title }) => {
  return <header className="w-full">{title}</header>;
};
```

2.  **화면 분할 및 파일 단위:** 하나의 파일에는 **단 하나의 컴포넌트(1 File = 1 Component)**만 선언하는 것을 원칙으로 합니다. 컴포넌트 내부에서만 쓰이는 초소형 서브 뷰는 파일 하단에 선언할 수 있으나, 가독성을 저해할 시 분리합니다.

### B. 네이밍 및 파일명 규칙
1.  **컴포넌트 파일:** 컴포넌트를 가진 파일명은 파스칼 케이스(**PascalCase**) 및 `.tsx` 확장자를 사용합니다. (예: `RoomLobby.tsx`, `SwipeCard.tsx`)
2.  **일반 유틸 및 타입 파일:** 일반 로직이나 타입 선언 파일은 카멜 케이스(**camelCase**) 및 `.ts` 확장자를 사용합니다. (예: `useWebSocket.ts`, `types.ts`)
3.  **Tailwind CSS 정렬:** 컴포넌트의 마크업 내 `className` 속성은 가독성을 위해 **레이아웃(w, h, flex) -> 스페이싱(p, m) -> 디자인(bg, text, border) -> 인터랙션(hover, active) -> 애니메이션** 순으로 정렬하는 것을 지향합니다.

---

## 4. 모바일 및 인앱 브라우저 대응 컨벤션 (Mobile WebView Workarounds)

카카오톡 인앱 브라우저 및 iOS Safari 등 일부 터치 제스처 기반 모바일 웹뷰(WKWebView) 환경에서는 일반적인 브라우저와 다른 고유 렌더링 결함(예: 탄성 스크롤 바운스 누수)이 존재하므로, 이를 예방하기 위한 특수 구현 컨벤션을 따릅니다.

### A. 배경 스크롤 누수 완전 차단 규칙 (Scroll Position Memory Lock)
모바일 환경에서는 모달 창이 노출되었을 때 `body { overflow: hidden }` 스타일만으로는 뒷배경 스크롤의 탄성 드래그(Rubber-banding)를 차단할 수 없습니다. 따라서 모달 개설 시 반드시 **Scroll Position Memory Lock** 기법을 적용해야 합니다.

1.  **동작 원리:**
    *   모달이 열리는 순간 사용자의 현재 스크롤 위치(`window.scrollY`)를 백업합니다.
    *   `body`를 `position: fixed`로 강제 설정하여 뷰포트를 화면에 고정하고, `top` 좌표를 음수 단위의 스크롤 값(`-scrollY`)으로 변경해 고정 당시의 화면 위치를 보존합니다.
    *   모달이 닫히면 고정을 해제하고, 백업해 두었던 스크롤 좌표로 즉각 복원(`window.scrollTo`)시킵니다.

2.  **구현 예시 (React Hook / Effect):**
```typescript
useEffect(() => {
  const isAnyModalOpen = isWelstoryModalOpen || isMenuDetailOpen;
  
  if (isAnyModalOpen) {
    const scrollY = window.scrollY;
    document.body.style.position = 'fixed';
    document.body.style.top = `-${scrollY}px`;
    document.body.style.width = '100%';
    document.body.style.overflow = 'hidden';
    document.body.dataset.scrollY = scrollY.toString();
  } else {
    const savedScrollY = document.body.dataset.scrollY;
    document.body.style.position = '';
    document.body.style.top = '';
    document.body.style.width = '';
    document.body.style.overflow = '';
    delete document.body.dataset.scrollY;
    
    if (savedScrollY) {
      window.scrollTo(0, parseInt(savedScrollY, 10));
    }
  }
  
  return () => {
    document.body.style.position = '';
    document.body.style.top = '';
    document.body.style.width = '';
    document.body.style.overflow = '';
    delete document.body.dataset.scrollY;
  };
}, [isWelstoryModalOpen, isMenuDetailOpen]);
```

### B. 스크롤 경계 격리 (`overscroll-contain`)
모달 내부에 스크롤 뷰가 존재하는 경우(예: 식단 정보 리스트), 해당 스크롤 컨테이너의 최상단/최하단 도달 시 발생하는 스크롤 이벤트가 부모 창이나 배경으로 전이되는 것을 막기 위해, 스크롤 컨테이너에 반드시 CSS `overscroll-behavior: contain` (Tailwind CSS: **`overscroll-contain`**) 속성을 지정해야 합니다.

*   *올바른 예:* `<div className="overflow-y-auto overscroll-contain">...</div>`

