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
