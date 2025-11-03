package com.swygbro.airoad.backend.tourdata.domain;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** TourAPI contentTypeId 정의 */
@Getter
@RequiredArgsConstructor
public enum ContentType {
  TOURIST_SPOT(12, "관광지", "Tourist Spot"),
  CULTURAL_FACILITY(14, "문화시설", "Cultural Facility"),
  FESTIVAL_EVENT(15, "축제공연행사", "Festival/Event"),
  TRAVEL_COURSE(25, "여행코스", "Travel Course"),
  LEISURE_SPORTS(28, "레포츠", "Leisure Sports"),
  ACCOMMODATION(32, "숙박", "Accommodation"),
  SHOPPING(38, "쇼핑", "Shopping"),
  RESTAURANT(39, "음식점", "Restaurant");

  private final Integer code;
  private final String koreanName;
  private final String englishName;

  /**
   * contentTypeId로 ContentType 찾기
   *
   * @param code contentTypeId
   * @return ContentType (없으면 null)
   */
  public static ContentType fromCode(Integer code) {
    if (code == null) {
      return null;
    }
    return Arrays.stream(values()).filter(type -> type.code.equals(code)).findFirst().orElse(null);
  }

  /**
   * contentTypeId가 유효한지 확인
   *
   * @param code contentTypeId
   * @return 유효 여부
   */
  public static boolean isValid(Integer code) {
    return fromCode(code) != null;
  }

  /**
   * 한글 이름 조회
   *
   * @param code contentTypeId
   * @return 한글 이름 (없으면 "알 수 없음")
   */
  public static String getKoreanName(Integer code) {
    ContentType type = fromCode(code);
    return type != null ? type.koreanName : "알 수 없음";
  }
}
