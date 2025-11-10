package com.swygbro.airoad.backend.trip.domain.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import com.swygbro.airoad.backend.common.domain.entity.BaseEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 여행의 일일 계획을 나타내는 엔티티 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyPlan extends BaseEntity {

  /** 일정이 속한 전체 여행 계획 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private TripPlan tripPlan;

  /** 예정된 방문 장소 목록 */
  @OneToMany(mappedBy = "dailyPlan", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ScheduledPlace> scheduledPlaces = new ArrayList<>();

  /** 일정 날짜 */
  @Column(nullable = false)
  private LocalDate date;

  /** 일차 번호 (1일차, 2일차, ...) */
  @Column(nullable = false)
  private Integer dayNumber;

  /** 일정 제목 */
  @Column private String title;

  /** 일정 설명 */
  @Column(columnDefinition = "TEXT")
  private String description;

  @Builder
  private DailyPlan(
      TripPlan tripPlan, LocalDate date, Integer dayNumber, String title, String description) {
    this.tripPlan = tripPlan;
    this.date = date;
    this.dayNumber = dayNumber;
    this.title = title;
    this.description = description;
  }

  /**
   * 방문 장소를 일정에 추가합니다.
   *
   * <p>양방향 관계를 유지하기 위해 ScheduledPlace의 dailyPlan도 설정합니다.
   *
   * @param scheduledPlace 추가할 방문 장소
   */
  public void addScheduledPlace(ScheduledPlace scheduledPlace) {
    this.scheduledPlaces.add(scheduledPlace);
    scheduledPlace.setDailyPlan(this);
  }

  /**
   * TripPlan과의 양방향 관계 설정을 위한 메서드입니다.
   *
   * <p>애그리게이트 루트(TripPlan)에서만 호출되어야 합니다.
   *
   * @param tripPlan 소속될 여행 계획
   */
  void setTripPlan(TripPlan tripPlan) {
    this.tripPlan = tripPlan;
  }
}
