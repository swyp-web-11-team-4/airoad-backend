package com.swygbro.airoad.backend.content.presentation.message;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.ai.domain.event.PlaceSummaryGeneratedEvent;
import com.swygbro.airoad.backend.content.application.PlaceVectorCommandUseCase;
import com.swygbro.airoad.backend.content.domain.dto.request.PlaceVectorSaveRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceVectorStoreListener {

  private final PlaceVectorCommandUseCase placeVectorCommandUseCase;

  @Async
  @EventListener
  public void onPlaceSummaryGenerated(PlaceSummaryGeneratedEvent event) {
    log.debug("PlaceSummaryGeneratedEvent 수신 - placeId: {}", event.placeId());
    PlaceVectorSaveRequest request =
        PlaceVectorSaveRequest.builder()
            .placeId(event.placeId())
            .name(event.name())
            .address(event.address())
            .themes(event.themes())
            .content(event.content())
            .build();

    placeVectorCommandUseCase.savePlaceVector(request);
  }
}
