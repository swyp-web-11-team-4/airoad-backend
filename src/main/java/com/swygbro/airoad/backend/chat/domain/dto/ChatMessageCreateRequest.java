package com.swygbro.airoad.backend.chat.domain.dto;

import org.springframework.ai.chat.messages.MessageType;

import lombok.Builder;

@Builder
public record ChatMessageCreateRequest(String message, MessageType messageType) {}
