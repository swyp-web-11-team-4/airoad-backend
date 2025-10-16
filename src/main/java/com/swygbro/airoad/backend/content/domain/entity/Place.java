package com.swygbro.airoad.backend.content.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;

import com.swygbro.airoad.backend.common.domain.embeddable.Location;
import com.swygbro.airoad.backend.common.domain.entity.BaseEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 장소 정보를 나타내는 엔티티 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseEntity {

  /** 장소의 위치 정보 (이름, 주소, 좌표) */
  @Embedded private Location location;

  /** 장소 상세 설명 (RAG 임베딩용) */
  @Lob
  @Column(columnDefinition = "TEXT")
  private String description;

  /** 대표 이미지 URL */
  @Lob
  @Column(columnDefinition = "TEXT")
  private String imageUrl;

  /** 운영 시간 정보 */
  private String operatingHours;

  /** 휴무일 정보 */
  private String holidayInfo;

  /** 추천 여행지 (꼭 가봐야할 명소) 여부 */
  @Column(nullable = false)
  private Boolean isMustVisit = false;

  /** 추천 우선순위 점수 (1~5) */
  @Column(nullable = false)
  private Integer placeScore = 1;

  @Builder
  private Place(
      Location location,
      String description,
      String imageUrl,
      String operatingHours,
      String holidayInfo,
      Boolean isMustVisit,
      Integer placeScore) {
    this.location = location;
    this.description = description;
    this.imageUrl = imageUrl;
    this.operatingHours = operatingHours;
    this.holidayInfo = holidayInfo;
    this.isMustVisit = isMustVisit;
    this.placeScore = placeScore;
  }
}
