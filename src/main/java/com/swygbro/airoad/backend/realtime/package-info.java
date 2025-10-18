/**
 * 실시간 메시징 패키지
 *
 * <p>WebSocket STOMP를 통한 실시간 양방향 통신을 담당합니다.
 *
 * <h2>구독 경로 구조</h2>
 *
 * <p>모든 구독 경로는 {@code /user} prefix를 사용하여 사용자별 개인 메시지를 수신합니다.
 *
 * <ul>
 *   <li><strong>/user/sub/chat/{chatRoomId}</strong> - AI 채팅 메시지
 *       <ul>
 *         <li>페이로드: {@code ChatMessageResponse}
 *         <li>용도: AI와의 1:1 대화 메시지 수신
 *       </ul>
 *   <li><strong>/user/sub/schedule</strong> - 여행 일정 DTO (향후 구현)
 *       <ul>
 *         <li>페이로드: {@code TripPlanDto (아직 미정)}
 *         <li>용도: AI로부터 받아오는 여행 일정 전송(1일 단위)
 *       </ul>
 * </ul>
 *
 * <h2>메시지 발행 경로 구조</h2>
 *
 * <ul>
 *   <li><strong>/pub/chat/{chatRoomId}/message</strong> - 채팅 메시지 전송
 *       <ul>
 *         <li>페이로드: {@code ChatMessageRequest}
 *       </ul>
 * </ul>
 *
 * <h2>클라이언트 구독 예시</h2>
 *
 * <pre>{@code
 * stompClient.connect({}, (frame) => {
 *     // 1. 채팅방 구독 (필수)
 *     stompClient.subscribe('/user/sub/chat/1', (message) => {
 *         const chatMsg = JSON.parse(message.body);
 *         displayChatMessage(chatMsg);
 *     });
 *
 *     // 2. 여행 일정 구독 (선택)
 *     stompClient.subscribe('/user/sub/schedule', (message) => {
 *         const tripPlan = JSON.parse(message.body);
 *         displayTripPlan(tripPlan);
 *     });
 * });
 * }</pre>
 *
 * @see com.swygbro.airoad.backend.chat.presentation.ChatMessageController
 * @see com.swygbro.airoad.backend.common.config.WebSocketConfig
 */
package com.swygbro.airoad.backend.realtime;
