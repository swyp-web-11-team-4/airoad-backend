# [0.20.0](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.19.1...v0.20.0) (2025-11-12)


### Features

* 간단한 회원 정보 조회 API 추가 ([91e1e53](https://github.com/swyp-web-11-team-4/airoad-backend/commit/91e1e533ba21cc0bbc51d84549e09ca4c02ec814))
* 간단한 회원 정보 조회 API 추가 ([4897416](https://github.com/swyp-web-11-team-4/airoad-backend/commit/4897416c04f9c6661a54ea86740d8e09b431080d))

# [0.19.0](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.18.0...v0.19.0) (2025-11-11)


### Features

* AI 프롬프트 관리 API 추가 ([#59](https://github.com/swyp-web-11-team-4/airoad-backend/issues/59)) ([0841279](https://github.com/swyp-web-11-team-4/airoad-backend/commit/0841279f8eb67f8524b0ef6538129ba929a835a0))

# [0.18.0](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.17.2...v0.18.0) (2025-11-11)


### Features

* WebSocket RECEIPT 프레임 수동 전송 및 여행 일차별 일정 조회 API 추가 ([00af57d](https://github.com/swyp-web-11-team-4/airoad-backend/commit/00af57dc5c9baf005d810e4cdb4b8c3f9c39ce26))

## [0.17.2](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.17.1...v0.17.2) (2025-11-10)


### Bug Fixes

* 여행 일정 삭제 로직 개선 및 RAG topK 값 수정 ([#57](https://github.com/swyp-web-11-team-4/airoad-backend/issues/57)) ([b1ca242](https://github.com/swyp-web-11-team-4/airoad-backend/commit/b1ca24209969fa3d5a136d5c1f3de417f94aa981))

## [0.17.1](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.17.0...v0.17.1) (2025-11-10)


### Bug Fixes

* RAG 파이프라인 개선 및 일정 생성 응답 DTO 필드 수정 ([#56](https://github.com/swyp-web-11-team-4/airoad-backend/issues/56)) ([d8f4099](https://github.com/swyp-web-11-team-4/airoad-backend/commit/d8f4099bf07b86aa5c5b9c4abfafc5bfad07eb3d))

# [0.17.0](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.16.0...v0.17.0) (2025-11-09)


### Features

* STOMP SUBSCRIBE 응답 최적화 및 여행 일정 상세 조회 API 추가 ([689665b](https://github.com/swyp-web-11-team-4/airoad-backend/commit/689665b39ca090580362b402c3eec0526653bece))

# [0.16.0](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.15.0...v0.16.0) (2025-11-08)


### Features

* OAuth2 인증 시 프론트엔드 출처 기반 동적 리다이렉트 기능 추가 ([#54](https://github.com/swyp-web-11-team-4/airoad-backend/issues/54)) ([be46ea3](https://github.com/swyp-web-11-team-4/airoad-backend/commit/be46ea37dcc7e4c0797da768dbd0844658b0b14e))

# [0.15.0](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.14.0...v0.15.0) (2025-11-08)


### Features

* 관광 데이터 동기화를 위한 인프라 개선 ([e193a27](https://github.com/swyp-web-11-team-4/airoad-backend/commit/e193a2723582460b07d419e1a8d1e9f97657cad4))

# [0.14.0](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.13.0...v0.14.0) (2025-11-07)


### Features

* OpenTelemetry 기반 옵저빌리티 환경 구축 ([#50](https://github.com/swyp-web-11-team-4/airoad-backend/issues/50)) ([81eb064](https://github.com/swyp-web-11-team-4/airoad-backend/commit/81eb064d1985bbedaad193b3ca9a45914412949f))

# [0.13.0](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.12.0...v0.13.0) (2025-11-06)


### Bug Fixes

* 여행 계획 생성 API 분리 및 권한 검증 강화 ([b11cdc2](https://github.com/swyp-web-11-team-4/airoad-backend/commit/b11cdc29b4f1ed12940e49241e04847c7d40dea7))
* 여행 계획 생성 이벤트에 요청 데이터 포함 및 엔티티 매핑 개선 ([bd40abf](https://github.com/swyp-web-11-team-4/airoad-backend/commit/bd40abfbf727ced47db08be0a19f88500ebcb92d))


### Features

* Place 엔티티에 TourAPI 연동 필드 추가 ([925d2ef](https://github.com/swyp-web-11-team-4/airoad-backend/commit/925d2ef711bbf2adf5b2fb43e7116098acd04465))
* TourAPI 데이터 동기화 기능 구현 ([d2393ff](https://github.com/swyp-web-11-team-4/airoad-backend/commit/d2393ff13f9ca602ebbf7722717a1db2f5ece9ca))

# [0.12.0](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.11.1...v0.12.0) (2025-11-06)


### Features

* 사용자 여행 일정 목록 조회 및 삭제 API 구현 ([#48](https://github.com/swyp-web-11-team-4/airoad-backend/issues/48)) ([c142e7c](https://github.com/swyp-web-11-team-4/airoad-backend/commit/c142e7c4fb1683bd5aeb4e612cf86a94d9bab884))

## [0.11.1](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.11.0...v0.11.1) (2025-11-04)


### Bug Fixes

* OAuth2 로그인 및 JWT 인증 프로세스 개선 ([#46](https://github.com/swyp-web-11-team-4/airoad-backend/issues/46)) ([0c7fba3](https://github.com/swyp-web-11-team-4/airoad-backend/commit/0c7fba31cd1651c16570b9647938955a3f9dfe95))

# [0.11.0](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.10.1...v0.11.0) (2025-11-04)


### Bug Fixes

* TripPlan 생성 시 transportation 필드 초기화 ([47490d0](https://github.com/swyp-web-11-team-4/airoad-backend/commit/47490d02592889d4e50d65da89ff1357757bb16f))


### Features

* TripPlanCreateRequest에 validation 어노테이션 추가 ([5dc330e](https://github.com/swyp-web-11-team-4/airoad-backend/commit/5dc330e705c3b0317fc1df47130a92590bd10e46))
* 여행 일정 생성 API 엔드포인트 추가 ([07c5650](https://github.com/swyp-web-11-team-4/airoad-backend/commit/07c5650c8f5cc156a894cbc82e446d17310497c6))
* 여행 일정 생성 비즈니스 로직 구현 ([e6fb5b7](https://github.com/swyp-web-11-team-4/airoad-backend/commit/e6fb5b7453dcedaceba695c1148822427156057b))

## [0.10.1](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.10.0...v0.10.1) (2025-11-04)


### Bug Fixes

* OAuth2 로그인 성공 시 토큰 쿼리 파라미터에 포함되게 변경 ([#45](https://github.com/swyp-web-11-team-4/airoad-backend/issues/45)) ([b287dac](https://github.com/swyp-web-11-team-4/airoad-backend/commit/b287dac7427f17b63bc64c082b4b8ba260a3cf47))

# [0.10.0](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.9.0...v0.10.0) (2025-11-01)


### Features

* Spring AI와 WebFlux를 이용한 AI 일정 생성 기능 구현 ([#39](https://github.com/swyp-web-11-team-4/airoad-backend/issues/39)) ([95ac4b6](https://github.com/swyp-web-11-team-4/airoad-backend/commit/95ac4b6f6eeaa2c5b90b4ce1296f2601d9e8c7ad))

# [0.9.0](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.8.0...v0.9.0) (2025-10-30)


### Features

* WebSocket 스트리밍 응답 구조화 및 더미 AI 서비스 구현 ([68dc112](https://github.com/swyp-web-11-team-4/airoad-backend/commit/68dc112c3a2273f5dab017ee76b8c680d146ccb8))

# [0.8.0](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.7.0...v0.8.0) (2025-10-29)


### Bug Fixes

* Spring Actuator 관련 리다이렉트 실패 발생 문제 개선 ([#35](https://github.com/swyp-web-11-team-4/airoad-backend/issues/35)) ([3297267](https://github.com/swyp-web-11-team-4/airoad-backend/commit/3297267d6887ef37ea821a8feb1b423767069ba2))


### Features

* 여행 일정 생성 이벤트 기반 아키텍처 구현 ([#33](https://github.com/swyp-web-11-team-4/airoad-backend/issues/33)) ([076e892](https://github.com/swyp-web-11-team-4/airoad-backend/commit/076e8921e6641ce81d4fc2d27af24d74b38b9eae))

# [0.7.0](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.6.0...v0.7.0) (2025-10-26)


### Features

* 장소 데이터 임베딩 및 벡터 스토어 적재 구현 ([#26](https://github.com/swyp-web-11-team-4/airoad-backend/issues/26)) ([c16afcd](https://github.com/swyp-web-11-team-4/airoad-backend/commit/c16afcdedeed0c96c895041286618c36c61d6640)), closes [#1](https://github.com/swyp-web-11-team-4/airoad-backend/issues/1) [#3](https://github.com/swyp-web-11-team-4/airoad-backend/issues/3) [#4](https://github.com/swyp-web-11-team-4/airoad-backend/issues/4) [#7](https://github.com/swyp-web-11-team-4/airoad-backend/issues/7)

# [0.6.0](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.5.1...v0.6.0) (2025-10-26)


### Bug Fixes

* NPE 문제 수정 및 테스트 추가 ([eda2233](https://github.com/swyp-web-11-team-4/airoad-backend/commit/eda223316c053560f4d5d38016e60d101a4b1933))


### Features

* AiMessage Repository 및 커서 페이징 쿼리 구현 ([0019f4a](https://github.com/swyp-web-11-team-4/airoad-backend/commit/0019f4a5d72fe5156d99b73bfab938347ee02933))
* AiMessageService 및 메시지 히스토리 조회 구현 ([7df9d99](https://github.com/swyp-web-11-team-4/airoad-backend/commit/7df9d9931aa0c3c9fbbc7e1df81ce6b3e3f6020e))
* 이벤트 기반 메시지 처리 아키텍처 구현 ([4776738](https://github.com/swyp-web-11-team-4/airoad-backend/commit/47767388a72dc0743ad45edba50ce0bf8f054999))
* 채팅 도메인 예외 처리 체계 구축 ([a9b430b](https://github.com/swyp-web-11-team-4/airoad-backend/commit/a9b430b635e95a452ac27927e8351dd1c058d421))
* 채팅 컨트롤러 구현 및 Swagger 문서화 ([6e04c51](https://github.com/swyp-web-11-team-4/airoad-backend/commit/6e04c5128517f40a451edce64864d8bc7c30f5dd))

## [0.5.1](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.5.0...v0.5.1) (2025-10-24)


### Bug Fixes

* Remove unnecessary example code and package structure ([12f9397](https://github.com/swyp-web-11-team-4/airoad-backend/commit/12f9397fd4624a73f46192ac700eca25ed289474)), closes [#10](https://github.com/swyp-web-11-team-4/airoad-backend/issues/10)
* Remove unnecessary example code and package structure ([ee4ab1c](https://github.com/swyp-web-11-team-4/airoad-backend/commit/ee4ab1c3d4ff33b0b392ff8f69917342fc813b22)), closes [#10](https://github.com/swyp-web-11-team-4/airoad-backend/issues/10)

# [0.5.0](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.4.0...v0.5.0) (2025-10-19)


### Features

* AI 채팅 메시지 처리 서비스 구현 ([5deb6f4](https://github.com/swyp-web-11-team-4/airoad-backend/commit/5deb6f451fa4e004afc0e351450cea8d91b7660d))
* Redis 설정 및 캐싱 구성 ([f67e8d3](https://github.com/swyp-web-11-team-4/airoad-backend/commit/f67e8d3e35573b80c66312486e24313d91fe0492))
* WebSocket STOMP 기본 설정 구현 ([df04eed](https://github.com/swyp-web-11-team-4/airoad-backend/commit/df04eedfd92757aba5326a09f6abb04736c389e2))
* WebSocket 예외 처리 추가 ([c671793](https://github.com/swyp-web-11-team-4/airoad-backend/commit/c671793425703e52bc99ad1e32f89883e572f7f4))
* 실시간 메시징 패키지 및 일정 구독 테스트 추가 ([f94f8ef](https://github.com/swyp-web-11-team-4/airoad-backend/commit/f94f8efe4c488e9babe47528b29cd15175c687fd))

# [0.4.0](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.3.0...v0.4.0) (2025-10-19)


### Features

* AI 채팅 메모리 기능 구현 ([#19](https://github.com/swyp-web-11-team-4/airoad-backend/issues/19)) ([91a326f](https://github.com/swyp-web-11-team-4/airoad-backend/commit/91a326f75b60f8035258d3a87f3076570151bca2))

# [0.3.0](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.2.0...v0.3.0) (2025-10-16)


### Features

* 채팅 API 컨트롤러 추가 (ChatRoom, ChatMessage) ([f19c9bb](https://github.com/swyp-web-11-team-4/airoad-backend/commit/f19c9bb11ffc80e26db04bc902d1830c8a36ca40))
* 채팅 API용 DTO 추가 (ChatMessage, ChatRoom, CursorPageResponse) ([4ccfb7a](https://github.com/swyp-web-11-team-4/airoad-backend/commit/4ccfb7abb32d8d7a0a725faaf1d18feb4493a629))
* 채팅 메시지 타입 및 미디어 지원 기능 추가 ([d99de5d](https://github.com/swyp-web-11-team-4/airoad-backend/commit/d99de5d37bb9d7c46433d4c7edd1ebc0ad176a75))

# [0.2.0](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.1.2...v0.2.0) (2025-10-16)


### Features

* JPA 도메인 엔티티 클래스 추가 ([#14](https://github.com/swyp-web-11-team-4/airoad-backend/issues/14)) ([f329571](https://github.com/swyp-web-11-team-4/airoad-backend/commit/f32957136c7b66978fb5428f0e34a750bfe93d00))

## [0.1.2](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.1.1...v0.1.2) (2025-10-14)


### Bug Fixes

* Remove unnecessary example code and package structure ([fbb700b](https://github.com/swyp-web-11-team-4/airoad-backend/commit/fbb700b1caa2e3dfc57f7a317cbe5ddb06ce3800)), closes [#5](https://github.com/swyp-web-11-team-4/airoad-backend/issues/5)

## [0.1.1](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.1.0...v0.1.1) (2025-10-12)


### Bug Fixes

* 불필요한 예제 코드 및 테스트 코드 제거 ([a25b9ae](https://github.com/swyp-web-11-team-4/airoad-backend/commit/a25b9ae1b7a3e17281b5cb047234978b98f69b3b))

# [0.1.0](https://github.com/swyp-web-11-team-4/airoad-backend/compare/v0.0.1...v0.1.0) (2025-10-12)


### Features

* 프로젝트 초기 설정 ([917b47f](https://github.com/swyp-web-11-team-4/airoad-backend/commit/917b47fade61a098d4b142fc7e288c5faf07617c))
