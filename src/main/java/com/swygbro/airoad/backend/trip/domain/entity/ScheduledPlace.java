package com.swygbro.airoad.backend.trip.domain.entity;

import java.time.LocalTime;

import jakarta.persistence.*;

import com.swygbro.airoad.backend.common.domain.entity.BaseEntity;
import com.swygbro.airoad.backend.content.domain.entity.Place;
import com.swygbro.airoad.backend.trip.domain.embeddable.TravelSegment;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 일일 계획에 포함된 개별 장소 방문 일정을 나타내는 엔티티 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduledPlace extends BaseEntity {

  /** 일정이 속한 일일 계획 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private DailyPlan dailyPlan;

  /** 방문할 장소 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private Place place;

  /** 일일 계획 내 방문 순서 */
  @Column(nullable = false, unique = true)
  private Integer visitOrder;

  /** 일정 분류 (아침, 점심, 저녁 등) */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ScheduledCategory category;

  /** 계획된 시작 시간 */
  @Column private LocalTime startTime;

  /** 계획된 종료 시간 */
  @Column private LocalTime endTime;

  /** 해당 장소까지의 이동 정보 (이동 시간, 수단) */
  @Embedded private TravelSegment travelSegment;

  @Builder
  private ScheduledPlace(
      DailyPlan dailyPlan,
      Place place,
      Integer visitOrder,
      ScheduledCategory category,
      LocalTime startTime,
      LocalTime endTime,
      TravelSegment travelSegment) {
    this.dailyPlan = dailyPlan;
    this.place = place;
    this.visitOrder = visitOrder;
    this.category = category;
    this.startTime = startTime;
    this.endTime = endTime;
    this.travelSegment = travelSegment;
  }

  public void update(
      Place place, Integer visitOrder, ScheduledCategory category, TravelSegment travelSegment) {
    this.place = place;
    this.visitOrder = visitOrder;
    this.category = category;
    this.travelSegment = travelSegment;
  }

  /**
   * 방문 순서만 업데이트합니다.
   *
   * @param visitOrder 새로운 방문 순서
   */
  public void updateVisitOrder(Integer visitOrder) {
    this.visitOrder = visitOrder;
  }

  /**
   * 일정 장소를 업데이트합니다.
   *
   * @param place 업데이트할 장소
   */
  public void updatePlace(Place place) {
    this.place = place;
  }

  /**
   * DailyPlan과의 양방향 관계 설정을 위한 메서드입니다.
   *
   * <p>DailyPlan.addScheduledPlace()에서만 호출되어야 합니다.
   *
   * @param dailyPlan 소속될 일일 계획
   */
  void setDailyPlan(DailyPlan dailyPlan) {
    this.dailyPlan = dailyPlan;
  }
}
