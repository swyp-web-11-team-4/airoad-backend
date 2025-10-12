# Contributing

이 프로젝트는 [Conventional Commits](https://www.conventionalcommits.org/ko/v1.0.0/) 명세를 따릅니다.  
`semantic-release`를 통해 자동으로 버전이 관리되고 릴리스 노트가 생성되므로, 커밋 메시지 규칙을 지켜주세요.

## 커밋 메시지 형식

각 커밋 메시지는 다음 형식으로 구성됩니다.

```
<type>: <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

- **type**: 커밋의 종류를 나타냅니다. (필수)
    - `feat`: 새로운 기능 추가
    - `fix`: 버그 수정
    - `design`: CSS 등 사용자 UI 디자인 변경
    - `docs`: 문서 변경 (README, 가이드, 주석 등)
    - `chore`: 빌드 설정, 의존성, 환경 설정 변경
    - `refactor`: 코드 리팩토링 혹은 성능 개선
    - `test`: 테스트 코드 추가 또는 수정
    - `comment`: 필요한 주석 추가 및 수정
    - `style`: 코드 포맷팅, 세미콜론 등 (기능 변경 없음)
    - `remove`: 파일, 기능, 의존성 제거
    - `ci`: CI 설정 변경
    - `cd`: CD 설정 변경

- **subject**: 커밋에 대한 간결한 설명을 제공합니다. (필수)

### 주요 타입 `feat` 와 `fix`

- `feat`: 새로운 기능이 추가될 때 사용합니다. `minor` 버전이 올라갑니다. (e.g., `1.0.0` -> `1.1.0`)
- `fix`: 버그가 수정될 때 사용합니다. `patch` 버전이 올라갑니다. (e.g., `1.0.0` -> `1.0.1`)

### 브레이킹 체인지 (Breaking Change)

기존 버전과 호환되지 않는 변경사항이 있을 경우, 커밋 메시지의 `body`나 `footer`에 `BREAKING CHANGE:`를 포함해야 합니다. 이 경우 `major` 버전이 올라갑니다. (e.g., `1.0.0` -> `2.0.0`)

```
feat: 사용자인증 API 변경

BREAKING CHANGE: `/api/v1/auth` 엔드포인트가 `/api/v2/auth`로 변경되었습니다.
```

## 예시

```
feat: 소셜 로그인 기능 추가
```

```
fix: 사용자 조회 시 발생하는 NPE 버그 수정
```
