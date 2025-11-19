package com.swygbro.airoad.backend.content.application;

import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.content.domain.dto.response.PlaceResponse;

public interface PlaceQueryUseCase {

  /**
   * 주어진 이름을 기반으로 장소 정보를 조회합니다.
   *
   * @param name 장소 이름 (정확히 일치)
   * @return 조회된 장소 정보를 담은 PlaceResponse 객체
   * @throws BusinessException 만약 해당 장소를 찾을 수 없다면 예외를 발생시킵니다.
   */
  PlaceResponse findPlaceByName(String name);
}
