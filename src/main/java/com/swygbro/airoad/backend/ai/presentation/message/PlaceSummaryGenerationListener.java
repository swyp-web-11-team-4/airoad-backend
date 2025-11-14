package com.swygbro.airoad.backend.ai.presentation.message;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.agent.summary.dto.request.AiPlaceSummaryRequest;
import com.swygbro.airoad.backend.ai.application.AiUseCase;
import com.swygbro.airoad.backend.ai.domain.entity.AgentType;
import com.swygbro.airoad.backend.content.domain.event.PlaceSummaryRequestedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class PlaceSummaryGenerationListener {
  private final AiUseCase aiUseCase;

  @EventListener
  @Async
  public void onPlaceSummaryRequested(PlaceSummaryRequestedEvent event) {
    log.info("PlaceSummaryRequestedEvent 수신 - placeId: {}", event.placeId());

    AiPlaceSummaryRequest request =
        AiPlaceSummaryRequest.builder()
            .placeId(event.placeId())
            .name(event.name())
            .address(event.address())
            .description(event.description())
            .operatingHours(event.operatingHours())
            .holidayInfo(event.holidayInfo())
            .themes(event.themes())
            .build();

    aiUseCase.agentCall(AgentType.PLACE_SUMMARY_AGENT, request);
  }
}
