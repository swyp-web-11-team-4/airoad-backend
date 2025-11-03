package com.swygbro.airoad.backend.tourdata.domain.dto;

import lombok.Builder;
import lombok.Getter;

/** TourAPI 데이터 동기화 응답 DTO */
@Getter
@Builder
public class TourDataSyncResponse {
  /** 처리된 총 개수 */
  private Integer totalProcessed;

  /** 메시지 */
  private String message;
}
