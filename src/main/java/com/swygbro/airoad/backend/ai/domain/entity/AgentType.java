package com.swygbro.airoad.backend.ai.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AgentType {
  TRIP_AGENT("tripAgent"),
  CHAT_AGENT("chatAgent"),
  PLACE_SUMMARY_AGENT("placeSummaryAgent");

  private final String name;
}
