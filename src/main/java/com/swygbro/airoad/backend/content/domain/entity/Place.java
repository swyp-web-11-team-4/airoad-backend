package com.swygbro.airoad.backend.content.domain.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;

import org.hibernate.annotations.BatchSize;

import com.swygbro.airoad.backend.common.domain.embeddable.Location;
import com.swygbro.airoad.backend.common.domain.entity.BaseEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;

/** 장소 정보를 나타내는 엔티티 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseEntity {

  /** 장소의 위치 정보 (이름, 주소, 좌표) */
  @Embedded private Location location;

  /** 장소 상세 설명 (RAG 임베딩용) */
  @Column(columnDefinition = "TEXT")
  private String description;

  /** 대표 이미지 URL */
  @Column(columnDefinition = "TEXT")
  private String imageUrl;

  /** 운영 시간 정보 */
  private String operatingHours;

  /** 휴무일 정보 */
  private String holidayInfo;

  /** 추천 여행지 (꼭 가봐야할 명소) 여부 */
  @Column(nullable = false)
  private Boolean isMustVisit = false;

  /** TourAPI 장소 고유 ID (외부 API 연동용) */
  @Column(unique = true, nullable = true)
  private Long apiPlaceId;

  /** TourAPI 콘텐츠 타입 ID (12=관광지, 14=문화시설, 15=축제, 28=레포츠, 38=쇼핑, 39=음식점) */
  @Column private Integer contentTypeId;

  /** 장소 테마 (한 장소는 여러 테마를 가질 수 있음) */
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "place_theme_type", joinColumns = @JoinColumn(name = "place_id"))
  @Column(name = "theme")
  @Enumerated(EnumType.STRING)
  @BatchSize(size = 100)
  private Set<PlaceThemeType> themes = new HashSet<>();

  @Builder
  private Place(
      Location location,
      String description,
      String imageUrl,
      String operatingHours,
      String holidayInfo,
      Boolean isMustVisit,
      Long apiPlaceId,
      Integer contentTypeId,
      @Singular Set<PlaceThemeType> themes) {
    this.location = location;
    this.description = description;
    this.imageUrl = imageUrl;
    this.operatingHours = operatingHours;
    this.holidayInfo = holidayInfo;
    this.isMustVisit = isMustVisit != null ? isMustVisit : Boolean.FALSE;
    this.apiPlaceId = apiPlaceId;
    this.contentTypeId = contentTypeId;
    this.themes = themes != null ? new HashSet<>(themes) : new HashSet<>();
  }

  /** description 업데이트 메서드 (Phase 2에서 overview 추가용) */
  public void updateDescription(String description) {
    this.description = description;
  }

  /** 운영 시간 및 휴무일 정보 업데이트 메서드 */
  public void updateOperatingInfo(String operatingHours, String holidayInfo) {
    this.operatingHours = operatingHours;
    this.holidayInfo = holidayInfo;
  }

  /**
   * isMustVisit 업데이트 메서드
   *
   * @param isMustVisit 추천 여행지 여부
   */
  public void updateIsMustVisit(Boolean isMustVisit) {
    this.isMustVisit = isMustVisit != null ? isMustVisit : Boolean.FALSE;
  }
}
