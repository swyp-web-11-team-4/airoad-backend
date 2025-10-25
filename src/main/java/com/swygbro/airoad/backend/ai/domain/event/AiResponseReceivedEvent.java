package com.swygbro.airoad.backend.ai.domain.event;

import com.swygbro.airoad.backend.ai.domain.dto.AiResponseContentType;

/**
 * AI 응답 수신 이벤트
 *
 * <p>AI 서버로부터 응답을 받았을 때 발행되는 이벤트입니다. WebSocket을 통해 클라이언트에게 실시간으로 전달됩니다.
 *
 * @param chatRoomId 채팅방 ID (AiConversation ID) - CHAT 타입에서 사용
 * @param tripPlanId 여행 계획 ID (TripPlan ID) - SCHEDULE 타입에서 사용
 * @param userId 사용자 ID (사용자 email)
 * @param content AI 응답 내용 (스트리밍의 경우 청크 단위)
 * @param contentType AI 응답 콘텐츠 타입 (CHAT: 채팅 메시지, SCHEDULE: 여행 일정)
 * @param isComplete 응답 완료 여부 (true: 마지막 청크, false: 중간 청크)
 */
public record AiResponseReceivedEvent(
    Long chatRoomId,
    Long tripPlanId,
    String userId,
    String content,
    AiResponseContentType contentType,
    boolean isComplete) {}
