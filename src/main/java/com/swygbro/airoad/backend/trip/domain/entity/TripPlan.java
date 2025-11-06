package com.swygbro.airoad.backend.trip.domain.entity;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import com.swygbro.airoad.backend.common.domain.embeddable.Location;
import com.swygbro.airoad.backend.common.domain.entity.BaseEntity;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;
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

  /** 여행 테마 목록 */
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "trip_plan_theme", joinColumns = @JoinColumn(name = "trip_plan_id"))
  @Enumerated(EnumType.STRING)
  private final List<PlaceThemeType> tripThemes = new ArrayList<>();

  /** 일차별 여행 일정 목록 */
  @OneToMany(mappedBy = "tripPlan", cascade = CascadeType.ALL, orphanRemoval = true)
  private final List<DailyPlan> dailyPlans = new ArrayList<>();

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

  /** 타이틀 이미지 */
  @Column private String imageUrl;

  @Builder
  private TripPlan(
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

  /**
   * 여행 계획의 타이틀 이미지 url을 업데이트합니다.
   *
   * @param imageUrl 이미지 url
   */
  public void updateImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  /**
   * 여행 계획의 제목을 업데이트합니다.
   *
   * @param title 새로운 여행 제목
   */
  public void updateTitle(String title) {
    this.title = title;
  }

  /**
   * 일차별 여행 일정을 추가합니다.
   *
   * <p>애그리게이트 루트를 통한 일관성 보장을 위해 DailyPlan 추가 시 이 메서드를 사용해야 합니다. 모든 일차가 생성되면 자동으로 여행 계획을 완료 상태로
   * 변경합니다.
   *
   * @param dailyPlan 추가할 일차별 일정
   */
  public void addDailyPlan(DailyPlan dailyPlan) {
    this.dailyPlans.add(dailyPlan);
    dailyPlan.setTripPlan(this);

    // 모든 일차 생성 완료 시 여행 계획 완료 처리
    if (isAllDaysCompleted()) {
      this.isCompleted = true;
    }
  }

  /**
   * 모든 일차의 일정이 생성되었는지 확인합니다.
   *
   * @return 모든 일차 생성 완료 여부
   */
  private boolean isAllDaysCompleted() {
    if (this.startDate == null || this.endDate == null) {
      return false;
    }
    long expectedDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
    return dailyPlans.size() >= expectedDays;
  }

  /**
   * 여행 테마를 추가합니다.
   *
   * <p>애그리게이트 루트를 통한 일관성 보장을 위해 TripTheme 추가 시 이 메서드를 사용해야 합니다.
   *
   * @param tripTheme 추가할 여행 테마
   */
  public void addTripTheme(PlaceThemeType tripTheme) {
    this.tripThemes.add(tripTheme);
  }

  /** 모든 여행 테마를 제거합니다. */
  public void clearTripThemes() {
    this.tripThemes.clear();
  }
}
