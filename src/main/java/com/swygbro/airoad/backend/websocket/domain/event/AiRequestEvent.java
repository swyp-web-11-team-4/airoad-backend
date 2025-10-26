package com.swygbro.airoad.backend.websocket.domain.event;

/**
 * AI 서버로 메시지 전송 요청 이벤트
 *
 * <p>사용자가 채팅 메시지를 보내면 이 이벤트가 발행되어 AI 서버로 메시지를 전송합니다.
 *
 * <h3>이벤트 흐름</h3>
 *
 * <ol>
 *   <li>사용자가 WebSocket으로 메시지 전송 ({@code /pub/chat/{chatRoomId}/message})
 *   <li>{@link com.swygbro.airoad.backend.chat.application.AiMessageService}에서 이 이벤트 발행
 *   <li>{@link com.swygbro.airoad.backend.websocket.application.AiRequestEventListener}에서 이벤트 수신
 *   <li>AI 서버로 HTTP 요청 전송
 *   <li>AI 응답은 {@link AiResponseReceivedEvent}로 수신되어 WebSocket으로 전송됨
 * </ol>
 *
 * @param chatRoomId 채팅방 ID (AiConversation.id)
 * @param tripPlanId 여행 계획 ID (TripPlan.id, null 가능)
 * @param userId 사용자 ID (이메일)
 * @param userMessage 사용자가 보낸 메시지 내용
 */
public record AiRequestEvent(Long chatRoomId, Long tripPlanId, String userId, String userMessage) {}
