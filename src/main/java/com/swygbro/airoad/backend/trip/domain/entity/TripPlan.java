package com.swygbro.airoad.backend.trip.domain.entity;

import java.time.LocalDate;

import jakarta.persistence.*;

import com.swygbro.airoad.backend.common.domain.embeddable.Location;
import com.swygbro.airoad.backend.common.domain.entity.BaseEntity;
import com.swygbro.airoad.backend.member.domain.entity.Member;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사용자의 전체 여행 계획을 나타내는 엔티티 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripPlan extends BaseEntity {

  /** 여행 테마 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn
  private TripTheme tripTheme;

  /** 여행 계획을 생성한 사용자 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private Member member;

  /** 여행 제목 */
  @Column(nullable = false)
  private String title;

  /** 여행 시작일 */
  @Column(nullable = false)
  private LocalDate startDate;

  /** 여행 종료일 */
  @Column(nullable = false)
  private LocalDate endDate;

  /** AI에 의한 여행 일정 생성 완료 여부 */
  @Column(nullable = false)
  private Boolean isCompleted;

  /** 선호 도시 (서울, 경기, 제주 등) */
  @Column(nullable = false, length = 50)
  private String region;

  /** 선호 이동 방식 */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Transportation transportation = Transportation.NONE;

  /** 예산 수준 */
  @Column(length = 50)
  private String budget;

  /** 여행 인원 수 */
  @Column(nullable = false)
  private Integer peopleCount;

  /** 출발지 정보 */
  @Embedded
  @AttributeOverride(name = "name", column = @Column(name = "start_location"))
  @AttributeOverride(name = "point", column = @Column(name = "start_point"))
  @AttributeOverride(name = "address", column = @Column(name = "start_address"))
  private Location startLocation;

  /** 도착지 정보 */
  @Embedded
  @AttributeOverride(name = "name", column = @Column(name = "end_location"))
  @AttributeOverride(name = "point", column = @Column(name = "end_point"))
  @AttributeOverride(name = "address", column = @Column(name = "end_address"))
  private Location endLocation;

  @Builder
  private TripPlan(
      TripTheme tripTheme,
      Member member,
      String title,
      LocalDate startDate,
      LocalDate endDate,
      Boolean isCompleted,
      String region,
      Transportation transportation,
      String budget,
      Integer peopleCount,
      Location startLocation,
      Location endLocation) {
    this.tripTheme = tripTheme;
    this.member = member;
    this.title = title;
    this.startDate = startDate;
    this.endDate = endDate;
    this.isCompleted = isCompleted;
    this.region = region;
    this.transportation = transportation;
    this.budget = budget;
    this.peopleCount = peopleCount;
    this.startLocation = startLocation;
    this.endLocation = endLocation;
  }
}
