package com.swygbro.airoad.backend.content.application;

import com.swygbro.airoad.backend.content.domain.dto.request.PlaceVectorSaveRequest;

public interface PlaceVectorCommandUseCase {

  void savePlaceVector(PlaceVectorSaveRequest request);
}
